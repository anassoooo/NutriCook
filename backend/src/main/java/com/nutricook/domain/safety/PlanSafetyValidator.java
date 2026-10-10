package com.nutricook.domain.safety;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.PlanDraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The single safety gate for plans (BR-SAF-05). Every path that creates or changes a plan calls it
 * immediately before saving; a non-empty result means nothing may be persisted.
 *
 * <p>Returns every violation, not just the first, so callers can explain all problems at once.
 */
public final class PlanSafetyValidator {

  private PlanSafetyValidator() {}

  /** Component eligibility plus per-meal limits (BR-SAF-02/06/09). */
  public static List<Violation> validateMeal(MealDraft meal, SafetyConstraints c) {
    List<Violation> violations = new ArrayList<>();
    for (Component component : meal.components()) {
      violations.addAll(EligibilityPolicy.check(component.food(), c));
    }

    Nutrients n = meal.nutrients(); // rounded at meal level, as the user sees it (BR-CMP-09)
    String slot = meal.slot().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    if (c.sugarPerMeal() != null && n.sugarG() > c.sugarPerMeal().value()) {
      violations.add(
          new Violation(
              Violation.Type.SUGAR_PER_MEAL,
              c.sugarPerMeal().source(),
              "The "
                  + slot
                  + " has "
                  + n.sugarG()
                  + " g of sugar; the limit for "
                  + c.sugarPerMeal().source()
                  + " is "
                  + c.sugarPerMeal().value()
                  + " g per meal."));
    }
    if (c.fatPerMeal() != null && n.fatG() > c.fatPerMeal().value()) {
      violations.add(
          new Violation(
              Violation.Type.FAT_PER_MEAL,
              c.fatPerMeal().source(),
              "The "
                  + slot
                  + " has "
                  + n.fatG()
                  + " g of fat; the limit for "
                  + c.fatPerMeal().source()
                  + " is "
                  + c.fatPerMeal().value()
                  + " g per meal."));
    }
    if (c.kosher()) {
      boolean meat = meal.components().stream().anyMatch(x -> x.food().meat());
      boolean milk = meal.components().stream().anyMatch(x -> x.food().contains(Allergen.MILK));
      if (meat && milk) {
        violations.add(
            new Violation(
                Violation.Type.KOSHER_MEAT_WITH_MILK,
                c.requiredFlags().get(DietFlag.KOSHER),
                "The " + slot + " combines meat and dairy, which is not kosher."));
      }
    }
    return violations;
  }

  /** Every meal, plus per-day limits (BR-SAF-06). */
  public static List<Violation> validate(PlanDraft plan, SafetyConstraints c) {
    List<Violation> violations = new ArrayList<>();
    for (MealDraft meal : plan.meals()) {
      violations.addAll(validateMeal(meal, c));
    }
    double sodium = plan.totals().sodiumMg();
    if (c.sodiumPerDay() != null && sodium > c.sodiumPerDay().value()) {
      violations.add(
          new Violation(
              Violation.Type.SODIUM_PER_DAY,
              c.sodiumPerDay().source(),
              "The day's plan has "
                  + Math.round(sodium)
                  + " mg of sodium; the limit for "
                  + c.sodiumPerDay().source()
                  + " is "
                  + Math.round(c.sodiumPerDay().value())
                  + " mg per day."));
    }
    return violations;
  }
}
