package com.nutricook.domain.energy;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.model.NutritionProfile;
import com.nutricook.domain.model.Slot;
import com.nutricook.domain.model.SlotTarget;
import com.nutricook.domain.model.Targets;
import com.nutricook.domain.rules.NutritionRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A user's daily targets: completeness and eligibility checks, then energy and macros
 * (BR-PRF-01/02, BR-NRG, BR-MAC). Pure: "today" is passed in (BR-TIM-03), so the same inputs always
 * give the same targets (BR-NRG-06).
 */
public class TargetCalculator {

  public static final int MIN_AGE = 18;
  public static final int MAX_AGE = 100;

  // BR-PRF-03 ranges; stored values outside them are treated as missing (BR-PRF-01)
  static final double MIN_WEIGHT_KG = 30;
  static final double MAX_WEIGHT_KG = 300;
  static final double MIN_HEIGHT_CM = 120;
  static final double MAX_HEIGHT_CM = 230;

  private final NutritionRules rules;
  private final EnergyCalculator energy;
  private final MacroCalculator macros;

  public TargetCalculator(NutritionRules rules) {
    this.rules = rules;
    this.energy = new EnergyCalculator(rules);
    this.macros = new MacroCalculator(rules);
  }

  public Targets compute(NutritionProfile p, LocalDate today) {
    requireComplete(p);

    int age = ageOn(p.dateOfBirth(), today);
    if (age < MIN_AGE || age > MAX_AGE) {
      throw new DomainException(
          ErrorCode.PROFILE_INELIGIBLE,
          "Personalized targets are available for adults aged " + MIN_AGE + " to " + MAX_AGE + ".",
          Map.of("minAge", MIN_AGE, "maxAge", MAX_AGE));
    }

    EnergyCalculator.EnergyTarget e =
        energy.target(p.sex(), p.weightKg(), p.heightCm(), age, p.activityLevel(), p.goal());
    MacroCalculator.Macros m = macros.compute(e.kcal(), p.weightKg(), p.goal());

    return new Targets(
        e.kcal(),
        m.proteinG(),
        m.carbsG(),
        m.fatG(),
        m.fiberG(),
        e.bmr(),
        e.tdee(),
        e.floorApplied());
  }

  /** Each slot's share of the daily targets, in eating order (BR-CMP-01). Rounded half-up. */
  public Map<Slot, SlotTarget> perSlot(Targets t) {
    Map<Slot, SlotTarget> result = new EnumMap<>(Slot.class);
    for (Slot slot : Slot.values()) {
      double share = rules.share(slot);
      result.put(
          slot,
          new SlotTarget(
              slot,
              share,
              round(t.kcal() * share),
              round(t.proteinG() * share),
              round(t.carbsG() * share),
              round(t.fatG() * share),
              round(t.fiberG() * share)));
    }
    return Collections.unmodifiableMap(result);
  }

  private static int round(double value) {
    return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).intValueExact();
  }

  /** Exact age in whole years (BR-TIM-02). */
  public static int ageOn(LocalDate dateOfBirth, LocalDate today) {
    return Period.between(dateOfBirth, today).getYears();
  }

  /** BR-PRF-01: field names are the API names, so the client can point at the right input. */
  private static void requireComplete(NutritionProfile p) {
    List<String> missing = new ArrayList<>();
    if (p.dateOfBirth() == null) missing.add("dateOfBirth");
    if (p.sex() == null) missing.add("gender");
    if (!inRange(p.weightKg(), MIN_WEIGHT_KG, MAX_WEIGHT_KG)) missing.add("weightKg");
    if (!inRange(p.heightCm(), MIN_HEIGHT_CM, MAX_HEIGHT_CM)) missing.add("heightCm");
    if (p.activityLevel() == null) missing.add("activityLevel");
    if (p.goal() == null) missing.add("healthGoal");
    if (!missing.isEmpty()) {
      throw new DomainException(
          ErrorCode.PROFILE_INCOMPLETE,
          "Please complete your profile: " + String.join(", ", missing) + ".",
          Map.of("missingFields", List.copyOf(missing)));
    }
  }

  private static boolean inRange(Double value, double min, double max) {
    return value != null && value >= min && value <= max;
  }
}
