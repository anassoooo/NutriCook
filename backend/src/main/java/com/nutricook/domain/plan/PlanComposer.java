package com.nutricook.domain.plan;

import static com.nutricook.domain.model.FoodRole.CARB;
import static com.nutricook.domain.model.FoodRole.FAT;
import static com.nutricook.domain.model.FoodRole.PRODUCE;
import static com.nutricook.domain.model.FoodRole.PROTEIN;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.PlanDraft;
import com.nutricook.domain.model.Slot;
import com.nutricook.domain.model.Targets;
import com.nutricook.domain.model.Warning;
import com.nutricook.domain.model.WarningCode;
import com.nutricook.domain.rules.NutritionRules;
import com.nutricook.domain.safety.EligibilityPolicy;
import com.nutricook.domain.safety.PlanSafetyValidator;
import com.nutricook.domain.safety.SafetyConstraints;
import com.nutricook.domain.safety.Violation;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Builds a day plan from targets, safety constraints and the catalog (BR-CMP, plan D2).
 *
 * <p>Greedy and deterministic: for each slot it draws foods by role from a seeded stream, sizes
 * them with {@link PortionSolver}, discards candidates that break a safety rule, and keeps the one
 * closest to the slot's goal. The day is then balanced, validated as a whole and classified against
 * the tolerances. Hard constraints are never relaxed: if no safe plan exists it fails with {@code
 * PLAN_INFEASIBLE} (BR-SAF-04).
 */
public final class PlanComposer {

  /** Whole-day retries with fresh streams before giving up. */
  static final int DAY_ATTEMPTS = 3;

  /** Day protein below (1 - this) of the target triggers LOW_PROTEIN (BR-CMP-05). */
  static final double MIN_PROTEIN_SHORTFALL = 0.10;

  /** A food appears at most this many times in a day (BR-CMP-06). */
  static final int MAX_USES_PER_DAY = 2;

  static final List<FoodRole> MAIN_PATTERN = List.of(PROTEIN, CARB, PRODUCE);
  static final List<List<FoodRole>> SNACK_PATTERNS =
      List.of(List.of(PROTEIN, PRODUCE), List.of(PRODUCE, FAT));

  enum DayFit {
    OK,
    WARNING,
    INFEASIBLE
  }

  private final NutritionRules rules;

  public PlanComposer(NutritionRules rules) {
    this.rules = rules;
  }

  public PlanDraft compose(
      Targets targets,
      SafetyConstraints constraints,
      Collection<FoodView> catalog,
      LocalDate date,
      long seed) {
    // Defense in depth: the composer only ever sees foods the policy allows (BR-SAF-05)
    List<FoodView> eligible =
        catalog.stream()
            .filter(f -> EligibilityPolicy.allows(f, constraints))
            .sorted(Comparator.comparingLong(FoodView::id))
            .toList();

    // Stop at the first day without warnings; otherwise keep the day with the fewest warnings,
    // then the smallest calorie deviation
    DomainException last = null;
    PlanDraft best = null;
    double bestScore = Double.MAX_VALUE;
    for (int attempt = 0; attempt < DAY_ATTEMPTS; attempt++) {
      try {
        PlanDraft day = composeDay(targets, constraints, eligible, date, seed, attempt);
        if (day.warnings().isEmpty()) return day;
        double deviation = Math.abs(day.totals().kcal() - targets.kcal()) / targets.kcal();
        double score = day.warnings().size() + deviation;
        if (score < bestScore) {
          best = day;
          bestScore = score;
        }
      } catch (DomainException e) {
        last = e;
      }
    }
    if (best != null) return best;
    throw last;
  }

  private PlanDraft composeDay(
      Targets t,
      SafetyConstraints c,
      List<FoodView> eligible,
      LocalDate date,
      long seed,
      int attempt) {
    Map<Long, Integer> uses = new HashMap<>();
    Set<Long> proteinsUsed = new HashSet<>();
    List<MealDraft> meals = new ArrayList<>();

    for (Slot slot : Slot.values()) {
      SplittableRandom rng = PlanSeed.streamFor(seed, attempt, slot);
      MealDraft meal = composeSlot(goal(t, slot), eligible, uses, proteinsUsed, c, rng);
      for (Component comp : meal.components()) {
        uses.merge(comp.food().id(), 1, Integer::sum);
        if (comp.role() == PROTEIN) proteinsUsed.add(comp.food().id());
      }
      meals.add(meal);
    }

    meals = balance(meals, t, c);
    PlanDraft day = new PlanDraft(date, meals, List.of());

    List<Violation> violations = PlanSafetyValidator.validate(day, c);
    if (!violations.isEmpty()) throw infeasible(violations.get(0));

    double deviation = (day.totals().kcal() - t.kcal()) / t.kcal();
    if (classify(deviation) == DayFit.INFEASIBLE) {
      throw new DomainException(
          ErrorCode.PLAN_INFEASIBLE,
          "We could not get within 10% of your calorie target with your restrictions.",
          Map.of("constraint", "calorie target", "deviation", round1(deviation * 100)));
    }
    return new PlanDraft(date, meals, warnings(day, t, deviation));
  }

  /** Each slot aims for its share of the day's energy and protein (BR-CMP-01/02). */
  SlotGoal goal(Targets t, Slot slot) {
    return new SlotGoal(slot, t.kcal() * rules.share(slot), t.proteinG() * rules.share(slot));
  }

  private MealDraft composeSlot(
      SlotGoal goal,
      List<FoodView> eligible,
      Map<Long, Integer> uses,
      Set<Long> proteinsUsed,
      SafetyConstraints c,
      SplittableRandom rng) {
    Slot slot = goal.slot();
    List<List<FoodRole>> patterns =
        new ArrayList<>(slot.isMain() ? List.of(MAIN_PATTERN) : SNACK_PATTERNS);
    if (!slot.isMain() && rng.nextBoolean()) patterns = List.of(patterns.get(1), patterns.get(0));

    FoodRole missingRole = null;
    Violation lastViolation = null;
    for (List<FoodRole> pattern : patterns) {
      Map<FoodRole, List<FoodView>> pools = new EnumMap<>(FoodRole.class);
      FoodRole empty = null;
      for (FoodRole role : pattern) {
        List<FoodView> pool = pool(eligible, role, slot, uses, proteinsUsed);
        if (pool.isEmpty()) {
          empty = role;
          break;
        }
        pools.put(role, pool);
      }
      if (empty != null) {
        missingRole = empty;
        continue;
      }
      List<FoodView> fats =
          slot.isMain() ? pool(eligible, FAT, slot, uses, proteinsUsed) : List.of();

      MealDraft best = null;
      double bestScore = Double.MAX_VALUE;
      for (int i = 0; i < rules.composerMaxAttempts(); i++) {
        Map<FoodRole, FoodView> picks = new EnumMap<>(FoodRole.class);
        for (FoodRole role : pattern) {
          List<FoodView> pool = pools.get(role);
          picks.put(role, pool.get(rng.nextInt(pool.size())));
        }
        FoodView fat = fats.isEmpty() ? null : fats.get(rng.nextInt(fats.size()));
        MealDraft candidate = PortionSolver.solve(goal, picks, fat);

        List<Violation> violations = PlanSafetyValidator.validateMeal(candidate, c);
        if (!violations.isEmpty()) {
          lastViolation = violations.get(0);
          continue;
        }
        double score = score(candidate, goal);
        if (score < bestScore) {
          best = candidate;
          bestScore = score;
        }
        if (withinTolerance(candidate, goal)) break;
      }
      if (best != null) return best;
    }

    if (lastViolation != null) throw infeasible(lastViolation);
    String role = missingRole == null ? "food" : missingRole.name().toLowerCase(Locale.ROOT);
    throw new DomainException(
        ErrorCode.PLAN_INFEASIBLE,
        "No " + role + " food in the catalog fits your restrictions for " + label(slot) + ".",
        Map.of("constraint", "no eligible " + role, "slot", slot.name()));
  }

  /** Foods of one role allowed in this slot that have not been used up today (BR-CMP-04/06). */
  private static List<FoodView> pool(
      List<FoodView> eligible,
      FoodRole role,
      Slot slot,
      Map<Long, Integer> uses,
      Set<Long> proteinsUsed) {
    return eligible.stream()
        .filter(f -> f.role() == role)
        .filter(f -> f.suitableSlots().contains(slot))
        .filter(f -> uses.getOrDefault(f.id(), 0) < MAX_USES_PER_DAY)
        .filter(f -> role != PROTEIN || !proteinsUsed.contains(f.id()))
        .toList();
  }

  private static double proteinDeviation(MealDraft meal, SlotGoal goal) {
    if (goal.proteinG() <= 0) return 0;
    return Math.abs(goal.proteinG() - meal.nutrients().proteinG()) / goal.proteinG();
  }

  private static double kcalDeviation(MealDraft meal, SlotGoal goal) {
    return Math.abs(meal.nutrients().kcal() - goal.kcal()) / goal.kcal();
  }

  private static double score(MealDraft meal, SlotGoal goal) {
    return kcalDeviation(meal, goal) + 0.5 * proteinDeviation(meal, goal);
  }

  /** Good enough to stop searching: energy within the meal band, protein within the day's. */
  private boolean withinTolerance(MealDraft meal, SlotGoal goal) {
    return kcalDeviation(meal, goal) <= rules.tolerance().meal()
        && proteinDeviation(meal, goal) <= MIN_PROTEIN_SHORTFALL;
  }

  /**
   * BR-CMP-05: when the day is outside the OK band, scale CARB portions (then FAT) once, within
   * their bounds. A meal keeps its previous portions if the scaled version would break a limit.
   */
  private List<MealDraft> balance(List<MealDraft> meals, Targets t, SafetyConstraints c) {
    for (FoodRole role : List.of(CARB, FAT)) {
      double total = Nutrients.sum(meals.stream().map(MealDraft::nutrients).toList()).kcal();
      double delta = t.kcal() - total;
      if (Math.abs(delta) / t.kcal() <= rules.tolerance().dayOk()) return meals;

      double roleKcal = 0;
      for (MealDraft m : meals)
        for (Component comp : m.components())
          if (comp.role() == role) roleKcal += comp.nutrients().kcal();
      if (roleKcal <= 0) continue;
      double factor = Math.max(0, (roleKcal + delta) / roleKcal);

      List<MealDraft> scaled = new ArrayList<>();
      for (MealDraft m : meals) {
        List<Component> comps = new ArrayList<>();
        for (Component comp : m.components()) {
          comps.add(
              comp.role() == role
                  ? new Component(
                      comp.food(), comp.food().portion().normalize(comp.grams() * factor))
                  : comp);
        }
        MealDraft candidate = new MealDraft(m.slot(), comps);
        scaled.add(PlanSafetyValidator.validateMeal(candidate, c).isEmpty() ? candidate : m);
      }
      meals = scaled;
    }
    return meals;
  }

  /** BR-CMP-05 day classification. */
  DayFit classify(double deviation) {
    double d = Math.abs(deviation);
    if (d <= rules.tolerance().dayOk()) return DayFit.OK;
    if (d <= rules.tolerance().dayMax()) return DayFit.WARNING;
    return DayFit.INFEASIBLE;
  }

  private List<Warning> warnings(PlanDraft day, Targets t, double deviation) {
    List<Warning> warnings = new ArrayList<>();
    if (classify(deviation) == DayFit.WARNING) {
      warnings.add(
          new Warning(
              WarningCode.TARGET_DEVIATION,
              "This plan is "
                  + round1(Math.abs(deviation) * 100)
                  + "% "
                  + (deviation < 0 ? "below" : "above")
                  + " your calorie target."));
    }
    if (day.totals().proteinG() < (1 - MIN_PROTEIN_SHORTFALL) * t.proteinG()) {
      warnings.add(
          new Warning(
              WarningCode.LOW_PROTEIN,
              "This plan has "
                  + Math.round(day.totals().proteinG())
                  + " g of protein; your target is "
                  + t.proteinG()
                  + " g."));
    }
    return warnings;
  }

  private static DomainException infeasible(Violation v) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("constraint", v.constraint());
    details.put("reason", v.message());
    return new DomainException(
        ErrorCode.PLAN_INFEASIBLE,
        "We could not build a plan that respects " + v.constraint() + ".",
        details);
  }

  private static String label(Slot slot) {
    return slot.name().toLowerCase(Locale.ROOT).replace('_', ' ');
  }

  private static double round1(double v) {
    return Math.round(v * 10) / 10.0;
  }
}
