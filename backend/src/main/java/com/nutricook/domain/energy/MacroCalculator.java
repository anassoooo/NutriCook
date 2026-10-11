package com.nutricook.domain.energy;

import com.nutricook.domain.model.Goal;
import com.nutricook.domain.rules.NutritionRules;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Daily macronutrient targets from the energy target (BR-MAC-01..06). Pure. */
public class MacroCalculator {

  /** Atwater general factors, kcal per gram (BR-MAC-01). */
  public static final int KCAL_PER_G_PROTEIN = 4;

  public static final int KCAL_PER_G_CARB = 4;
  public static final int KCAL_PER_G_FAT = 9;

  public record Macros(int proteinG, int carbsG, int fatG, int fiberG) {}

  private final NutritionRules rules;

  public MacroCalculator(NutritionRules rules) {
    this.rules = rules;
  }

  public Macros compute(int targetKcal, double weightKg, Goal goal) {
    // BR-MAC-02: per-kg by goal, capped per kg and by share of energy
    double protein =
        Math.min(
            rules.proteinPerKg(goal) * weightKg,
            Math.min(
                rules.proteinMaxPerKg() * weightKg,
                rules.proteinMaxEnergyShare() * targetKcal / KCAL_PER_G_PROTEIN));

    // BR-MAC-03: share of energy, with a per-kg floor
    double fat =
        Math.max(
            rules.fatEnergyShare() * targetKcal / KCAL_PER_G_FAT, rules.fatMinPerKg() * weightKg);

    // BR-MAC-04: carbs take the remaining energy, computed from unrounded values
    double carbs =
        (targetKcal - KCAL_PER_G_PROTEIN * protein - KCAL_PER_G_FAT * fat) / KCAL_PER_G_CARB;
    if (carbs < 0) {
      // Unreachable for valid adult profiles (the BMR floor keeps energy high enough);
      // a negative remainder is a defect in the rules, never a result to return.
      throw new IllegalStateException(
          "Negative carbohydrate remainder for " + targetKcal + " kcal, " + weightKg + " kg");
    }

    double fiber = rules.fiberPer1000Kcal() * targetKcal / 1000.0; // BR-MAC-05

    // BR-MAC-06: round once, at the end, half-up
    return new Macros(round(protein), round(carbs), round(fat), round(fiber));
  }

  private static int round(double grams) {
    return BigDecimal.valueOf(grams).setScale(0, RoundingMode.HALF_UP).intValueExact();
  }
}
