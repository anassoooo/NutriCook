package com.nutricook.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.entity.FoodItem;
import com.nutricook.entity.Meal;
import com.nutricook.entity.MealFoodItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NutritionServiceTest {

  private NutritionService service;

  @BeforeEach
  void setUp() {
    service = new NutritionService();
  }

  // ── computeMealNutrition ─────────────────────────────────────────────

  @Test
  void computeMealNutrition_exactlyFor100g() {
    FoodItem food =
        FoodItem.builder()
            .caloriesPer100g(200.0)
            .proteinPer100g(20.0)
            .carbsPer100g(30.0)
            .fatPer100g(10.0)
            .sugarPer100g(5.0)
            .fiberPer100g(3.0)
            .build();
    Meal meal = mealWith(food, 100.0);

    service.computeMealNutrition(meal);

    assertThat(meal.getCalories()).isEqualTo(200);
    assertThat(meal.getProteinG()).isEqualTo(20.0);
    assertThat(meal.getCarbsG()).isEqualTo(30.0);
    assertThat(meal.getFatG()).isEqualTo(10.0);
    assertThat(meal.getSugarG()).isEqualTo(5.0);
    assertThat(meal.getFiberG()).isEqualTo(3.0);
  }

  @Test
  void computeMealNutrition_doublesFor200g() {
    FoodItem food =
        FoodItem.builder()
            .caloriesPer100g(150.0)
            .proteinPer100g(10.0)
            .carbsPer100g(20.0)
            .fatPer100g(5.0)
            .sugarPer100g(2.0)
            .fiberPer100g(1.0)
            .build();
    Meal meal = mealWith(food, 200.0);

    service.computeMealNutrition(meal);

    assertThat(meal.getCalories()).isEqualTo(300);
    assertThat(meal.getProteinG()).isEqualTo(20.0);
    assertThat(meal.getCarbsG()).isEqualTo(40.0);
    assertThat(meal.getFatG()).isEqualTo(10.0);
  }

  @Test
  void computeMealNutrition_noopForEmptyItems() {
    Meal meal = Meal.builder().name("Empty").mealFoodItems(new ArrayList<>()).build();
    service.computeMealNutrition(meal);
    assertThat(meal.getCalories()).isNull();
  }

  // ── computeTotals ────────────────────────────────────────────────────

  @Test
  void computeTotals_sumsAllMeals() {
    Meal m1 =
        Meal.builder().calories(500).proteinG(30.0).carbsG(60.0).fatG(15.0).sugarG(8.0).build();
    Meal m2 =
        Meal.builder().calories(300).proteinG(20.0).carbsG(40.0).fatG(10.0).sugarG(5.0).build();

    NutritionService.NutritionTotals totals = service.computeTotals(List.of(m1, m2));

    assertThat(totals.calories()).isEqualTo(800);
    assertThat(totals.proteinG()).isEqualTo(50.0);
    assertThat(totals.carbsG()).isEqualTo(100.0);
    assertThat(totals.fatG()).isEqualTo(25.0);
    assertThat(totals.sugarG()).isEqualTo(13.0);
  }

  @Test
  void computeTotals_handlesNullFieldsGracefully() {
    Meal m =
        Meal.builder().calories(null).proteinG(null).carbsG(null).fatG(null).sugarG(null).build();
    NutritionService.NutritionTotals totals = service.computeTotals(List.of(m));
    assertThat(totals.calories()).isEqualTo(0);
    assertThat(totals.proteinG()).isEqualTo(0.0);
    assertThat(totals.carbsG()).isEqualTo(0.0);
  }

  @Test
  void computeTotals_emptyListGivesZeros() {
    NutritionService.NutritionTotals totals = service.computeTotals(List.of());
    assertThat(totals.calories()).isEqualTo(0);
    assertThat(totals.proteinG()).isEqualTo(0.0);
  }

  // ── helpers ──────────────────────────────────────────────────────────

  private Meal mealWith(FoodItem food, double grams) {
    MealFoodItem mfi = MealFoodItem.builder().foodItem(food).quantityG(grams).build();
    return Meal.builder().name("Test").mealFoodItems(new ArrayList<>(List.of(mfi))).build();
  }
}
