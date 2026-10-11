package com.nutricook.domain.catalog;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.Nutrients;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Data-quality gate for catalog foods (BR-CAT-02). The admin API rejects a food that fails it, the
 * application refuses to start with an active food that fails it, and the catalog test runs it on
 * the seeded data.
 */
public final class FoodQualityGate {

  /** EU Regulation 1169/2011 energy factor for dietary fiber, kcal per gram. */
  static final double KCAL_PER_G_FIBER = 2;

  /** Allergens that come from animals a vegetarian does not eat. */
  private static final Set<Allergen> NOT_VEGETARIAN =
      EnumSet.of(Allergen.FISH, Allergen.CRUSTACEANS, Allergen.MOLLUSCS);

  /** Allergens that come from animals or animal products: none is vegan. */
  private static final Set<Allergen> NOT_VEGAN =
      EnumSet.of(
          Allergen.FISH, Allergen.CRUSTACEANS, Allergen.MOLLUSCS, Allergen.MILK, Allergen.EGGS);

  private FoodQualityGate() {}

  /** Energy implied by the macros: 4 P + 4 (C − fiber) + 2 fiber + 9 F. */
  public static double expectedKcal(Nutrients per100g) {
    return 4 * per100g.proteinG()
        + 4 * (per100g.carbsG() - per100g.fiberG())
        + KCAL_PER_G_FIBER * per100g.fiberG()
        + 9 * per100g.fatG();
  }

  /** Every problem with the per-100 g values; empty when they pass. */
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

  /**
   * Diet labels must not contradict what the food is (BR-SAF-10 relies on them): VEGAN implies
   * VEGETARIAN, neither may be meat, and neither may declare an allergen from an animal they
   * exclude.
   */
  public static List<String> labelProblems(
      Set<Allergen> allergens, Set<DietFlag> flags, boolean meat) {
    List<String> problems = new ArrayList<>();
    boolean vegan = flags.contains(DietFlag.VEGAN);
    boolean vegetarian = flags.contains(DietFlag.VEGETARIAN);
    if (vegan && !vegetarian) problems.add("VEGAN foods must also be VEGETARIAN");
    if ((vegan || vegetarian) && meat) problems.add("meat cannot be VEGETARIAN or VEGAN");
    if (vegetarian && allergens.stream().anyMatch(NOT_VEGETARIAN::contains)) {
      problems.add("a food with fish or shellfish cannot be VEGETARIAN");
    }
    if (vegan && allergens.stream().anyMatch(NOT_VEGAN::contains)) {
      problems.add("a food with an animal-derived allergen cannot be VEGAN");
    }
    return problems;
  }

  /** The whole gate for one food: values, labels, verified allergens and at least one slot. */
  public static List<String> problems(FoodView food) {
    List<String> problems = new ArrayList<>(problems(food.per100g()));
    problems.addAll(labelProblems(food.allergens(), food.dietFlags(), food.meat()));
    if (!food.allergensKnown()) problems.add("allergens are not verified");
    if (food.suitableSlots().isEmpty()) problems.add("no meal slot");
    return problems;
  }
}
