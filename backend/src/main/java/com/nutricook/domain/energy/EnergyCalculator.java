package com.nutricook.domain.energy;

import com.nutricook.domain.model.ActivityLevel;
import com.nutricook.domain.model.Goal;
import com.nutricook.domain.model.Sex;
import com.nutricook.domain.rules.NutritionRules;

/** Daily energy target: BMR, TDEE, goal adjustment, floor and rounding (BR-NRG-01..05). Pure. */
public class EnergyCalculator {

  /** Unrounded BMR and TDEE are kept so the UI can explain the target. */
  public record EnergyTarget(double bmr, double tdee, int kcal, boolean floorApplied) {}

  private final NutritionRules rules;

  public EnergyCalculator(NutritionRules rules) {
    this.rules = rules;
  }

  /** Mifflin-St Jeor (BR-NRG-01). */
  public double bmr(Sex sex, double weightKg, double heightCm, int ageYears) {
    double sexConstant =
        switch (sex) {
          case MALE -> 5;
          case FEMALE -> -161;
          case UNSPECIFIED -> -78; // midpoint of the two
        };
    return 10 * weightKg + 6.25 * heightCm - 5 * ageYears + sexConstant;
  }

  public EnergyTarget target(
      Sex sex, double weightKg, double heightCm, int ageYears, ActivityLevel activity, Goal goal) {
    double bmr = bmr(sex, weightKg, heightCm, ageYears);
    double tdee = bmr * activity.factor(); // BR-NRG-02
    double raw = tdee + adjustment(goal); // BR-NRG-03

    // BR-NRG-04: never below the sex minimum, and a deficit never goes below BMR
    int sexMinimum = sex == Sex.MALE ? rules.minKcal().male() : rules.minKcal().other();
    double floor = Math.max(sexMinimum, bmr);
    boolean floorApplied = raw < floor;

    // BR-NRG-05: nearest 10, but rounding must not undo the floor
    int kcal = roundToNearestTen(Math.max(raw, floor));
    if (kcal < floor) kcal = (int) (Math.ceil(floor / 10.0) * 10);

    return new EnergyTarget(bmr, tdee, kcal, floorApplied);
  }

  private int adjustment(Goal goal) {
    return switch (goal) {
      case LOSE_WEIGHT -> rules.goalAdjustment().lose();
      case GAIN_MUSCLE -> rules.goalAdjustment().gain();
      case MAINTAIN_WEIGHT, IMPROVE_HEALTH -> 0;
    };
  }

  private static int roundToNearestTen(double kcal) {
    return (int) (Math.floor(kcal / 10.0 + 0.5) * 10);
  }
}
