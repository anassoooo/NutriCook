package com.nutricook.domain.catalog;

import com.nutricook.domain.model.Nutrients;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-quality gate for catalog foods (BR-CAT-02). Used by the catalog test today and by the admin
 * API (US7) to reject bad entries.
 */
public final class FoodQualityGate {

  /** EU Regulation 1169/2011 energy factor for dietary fiber, kcal per gram. */
  static final double KCAL_PER_G_FIBER = 2;

  private FoodQualityGate() {}

  /** Energy implied by the macros: 4 P + 4 (C − fiber) + 2 fiber + 9 F. */
  public static double expectedKcal(Nutrients per100g) {
    return 4 * per100g.proteinG()
        + 4 * (per100g.carbsG() - per100g.fiberG())
        + KCAL_PER_G_FIBER * per100g.fiberG()
        + 9 * per100g.fatG();
  }

  /** Every problem with the per-100 g values; empty when the food passes. */
  public static List<String> problems(Nutrients per100g) {
    List<String> problems = new ArrayList<>();
    if (per100g.fiberG() > per100g.carbsG()) problems.add("fiber exceeds carbohydrates");
    if (per100g.sugarG() > per100g.carbsG()) problems.add("sugar exceeds carbohydrates");
    if (per100g.proteinG() + per100g.carbsG() + per100g.fatG() > 100) {
      problems.add("protein + carbohydrates + fat exceed 100 g per 100 g");
    }
    double expected = expectedKcal(per100g);
    double tolerance = Math.max(0.15 * per100g.kcal(), 10);
    if (Math.abs(per100g.kcal() - expected) > tolerance) {
      problems.add(
          "energy "
              + per100g.kcal()
              + " kcal does not match the macros ("
              + Math.round(expected)
              + " kcal)");
    }
    return problems;
  }
}
