package com.nutricook.domain.plan;

import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.PlanDraft;
import com.nutricook.domain.model.Slot;
import com.nutricook.domain.safety.EligibilityPolicy;
import com.nutricook.domain.safety.PlanSafetyValidator;
import com.nutricook.domain.safety.SafetyConstraints;
import com.nutricook.domain.safety.Violation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Replaces one food in a meal (BR-AI-03). Only the component with the same role as the new food
 * changes; its portion is re-sized to keep the meal's energy, within the food's bounds (BR-CMP-03).
 * The whole day is re-validated before the swap is accepted (BR-SAF-05). Used by manual swaps and
 * AI swaps alike.
 */
public final class SwapPlanner {

  public sealed interface Result permits Swapped, Rejected {}

  /** The day after the swap, and the changed meal. */
  public record Swapped(PlanDraft plan, MealDraft meal) implements Result {}

  public record Rejected(Reason reason, String message) implements Result {}

  public enum Reason {
    /** The food breaks a restriction or allergy (BR-SAF-02). */
    NOT_ELIGIBLE,
    /** The food is not offered for this meal (BR-CMP-04). */
    NOT_SUITABLE_FOR_MEAL,
    /** The meal has no food of the same role to replace (BR-AI-03). */
    NO_SAME_ROLE,
    SAME_FOOD,
    /** The swapped day would exceed a health limit (BR-SAF-06). */
    VIOLATES_LIMIT
  }

  private SwapPlanner() {}

  public static Result swap(PlanDraft plan, Slot slot, FoodView food, SafetyConstraints c) {
    List<Violation> ineligible = EligibilityPolicy.check(food, c);
    if (!ineligible.isEmpty())
      return new Rejected(Reason.NOT_ELIGIBLE, ineligible.get(0).message());
    if (!food.suitableSlots().contains(slot)) {
      return new Rejected(
          Reason.NOT_SUITABLE_FOR_MEAL, food.name() + " isn't offered for " + label(slot) + ".");
    }
    MealDraft meal = meal(plan, slot);
    Component old =
        meal.components().stream().filter(x -> x.role() == food.role()).findFirst().orElse(null);
    if (old == null) {
      return new Rejected(
          Reason.NO_SAME_ROLE,
          "The "
              + label(slot)
              + " has no "
              + label(food.role())
              + " to replace with "
              + food.name()
              + ".");
    }
    if (old.food().id() == food.id()) {
      return new Rejected(
          Reason.SAME_FOOD, food.name() + " is already in the " + label(slot) + ".");
    }

    Component replacement = new Component(food, portionFor(food, old.nutrients().kcal()));
    List<Component> components = new ArrayList<>(meal.components());
    components.set(components.indexOf(old), replacement);
    MealDraft newMeal = new MealDraft(slot, components);

    List<MealDraft> meals = new ArrayList<>(plan.meals());
    meals.set(meals.indexOf(meal), newMeal);
    PlanDraft newPlan = new PlanDraft(plan.date(), meals, plan.warnings());

    List<Violation> violations = PlanSafetyValidator.validate(newPlan, c);
    if (!violations.isEmpty()) {
      return new Rejected(Reason.VIOLATES_LIMIT, violations.get(0).message());
    }
    return new Swapped(newPlan, newMeal);
  }

  /**
   * Foods that could replace the {@code role} component of the {@code slot} meal right now, closest
   * in energy first. Every alternative is a swap that would be accepted.
   */
  public static List<Component> alternatives(
      PlanDraft plan,
      Slot slot,
      FoodRole role,
      Collection<FoodView> catalog,
      SafetyConstraints c,
      int limit) {
    Component current =
        meal(plan, slot).components().stream()
            .filter(x -> x.role() == role)
            .findFirst()
            .orElse(null);
    if (current == null) return List.of();
    double kcal = current.nutrients().kcal();
    return catalog.stream()
        .filter(f -> f.role() == role && f.id() != current.food().id())
        .filter(f -> swap(plan, slot, f, c) instanceof Swapped)
        .map(f -> new Component(f, portionFor(f, kcal)))
        .sorted(
            Comparator.comparingDouble((Component x) -> Math.abs(x.nutrients().kcal() - kcal))
                .thenComparing(x -> x.food().name()))
        .limit(limit)
        .toList();
  }

  /** The legal portion of {@code food} closest to {@code kcal} of energy. */
  static double portionFor(FoodView food, double kcal) {
    double kcalPerGram = food.per100g().kcal() / 100;
    return food.portion()
        .normalize(kcalPerGram > 0 ? kcal / kcalPerGram : food.portion().defaultG());
  }

  private static MealDraft meal(PlanDraft plan, Slot slot) {
    return plan.meals().stream()
        .filter(m -> m.slot() == slot)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("plan has no " + slot));
  }

  private static String label(Enum<?> e) {
    return e.name().toLowerCase(Locale.ROOT).replace('_', ' ');
  }
}
