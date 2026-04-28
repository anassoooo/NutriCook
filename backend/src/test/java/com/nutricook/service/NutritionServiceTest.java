package com.nutricook.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.entity.FoodItem;
import com.nutricook.entity.HealthCondition;
import com.nutricook.entity.Meal;
import com.nutricook.entity.MealFoodItem;
import com.nutricook.entity.UserProfile;
import com.nutricook.entity.enums.ActivityLevel;
import com.nutricook.entity.enums.Gender;
import com.nutricook.entity.enums.HealthGoal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NutritionServiceTest {

  private NutritionService service;

  @BeforeEach
  void setUp() {
    service = new NutritionService();
  }

  // ── computeTDEE ──────────────────────────────────────────────────────

  @Test
  void computeTDEE_returnsDefaultWhenProfileIncomplete() {
    assertThat(service.computeTDEE(new UserProfile())).isEqualTo(2000);
  }

  @Test
  void computeTDEE_maleSedentaryLoseWeight_isInReasonableRange() {
    UserProfile p =
        UserProfile.builder()
            .weightKg(80.0)
            .heightCm(180.0)
            .dateOfBirth(LocalDate.now().minusYears(30))
            .gender(Gender.MALE)
            .activityLevel(ActivityLevel.SEDENTARY)
            .healthGoal(HealthGoal.LOSE_WEIGHT)
            .build();
    // BMR ≈ 1858, TDEE = 1858*1.2 - 500 ≈ 1730
    assertThat(service.computeTDEE(p)).isBetween(1500, 2000);
  }

  @Test
  void computeTDEE_femaleExtraActiveGainMuscle_isHigher() {
    UserProfile p =
        UserProfile.builder()
            .weightKg(65.0)
            .heightCm(170.0)
            .dateOfBirth(LocalDate.now().minusYears(25))
            .gender(Gender.FEMALE)
            .activityLevel(ActivityLevel.EXTRA_ACTIVE)
            .healthGoal(HealthGoal.GAIN_MUSCLE)
            .build();
    assertThat(service.computeTDEE(p)).isGreaterThan(2500);
  }

  @Test
  void computeTDEE_neverDropsBelowMinimum() {
    // Extreme profile that would normally give a very low value
    UserProfile p =
        UserProfile.builder()
            .weightKg(20.0)
            .heightCm(90.0)
            .dateOfBirth(LocalDate.now().minusYears(100))
            .gender(Gender.FEMALE)
            .activityLevel(ActivityLevel.SEDENTARY)
            .healthGoal(HealthGoal.LOSE_WEIGHT)
            .build();
    assertThat(service.computeTDEE(p)).isGreaterThanOrEqualTo(1200);
  }

  @Test
  void computeTDEE_gainMuscleAdds300ToBase() {
    UserProfile base =
        UserProfile.builder()
            .weightKg(70.0)
            .heightCm(175.0)
            .dateOfBirth(LocalDate.now().minusYears(28))
            .gender(Gender.MALE)
            .activityLevel(ActivityLevel.MODERATELY_ACTIVE)
            .healthGoal(HealthGoal.MAINTAIN_WEIGHT)
            .build();
    UserProfile gain =
        UserProfile.builder()
            .weightKg(70.0)
            .heightCm(175.0)
            .dateOfBirth(LocalDate.now().minusYears(28))
            .gender(Gender.MALE)
            .activityLevel(ActivityLevel.MODERATELY_ACTIVE)
            .healthGoal(HealthGoal.GAIN_MUSCLE)
            .build();
    assertThat(service.computeTDEE(gain)).isEqualTo(service.computeTDEE(base) + 300);
  }

  @Test
  void computeTDEE_loseWeightSubtracts500FromBase() {
    UserProfile base =
        UserProfile.builder()
            .weightKg(70.0)
            .heightCm(175.0)
            .dateOfBirth(LocalDate.now().minusYears(28))
            .gender(Gender.MALE)
            .activityLevel(ActivityLevel.LIGHTLY_ACTIVE)
            .healthGoal(HealthGoal.MAINTAIN_WEIGHT)
            .build();
    UserProfile lose =
        UserProfile.builder()
            .weightKg(70.0)
            .heightCm(175.0)
            .dateOfBirth(LocalDate.now().minusYears(28))
            .gender(Gender.MALE)
            .activityLevel(ActivityLevel.LIGHTLY_ACTIVE)
            .healthGoal(HealthGoal.LOSE_WEIGHT)
            .build();
    int diff = service.computeTDEE(base) - service.computeTDEE(lose);
    assertThat(diff).isEqualTo(500);
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

  // ── isMealCompatible ─────────────────────────────────────────────────

  @Test
  void isMealCompatible_trueWhenNoConditions() {
    Meal meal = Meal.builder().sugarG(50.0).fatG(30.0).build();
    assertThat(service.isMealCompatible(meal, Set.of())).isTrue();
  }

  @Test
  void isMealCompatible_falseWhenSugarExceedsLimit() {
    HealthCondition c = HealthCondition.builder().sugarLimitG(10.0).build();
    Meal meal = Meal.builder().sugarG(15.0).fatG(5.0).build();
    assertThat(service.isMealCompatible(meal, Set.of(c))).isFalse();
  }

  @Test
  void isMealCompatible_falseWhenFatExceedsLimit() {
    HealthCondition c = HealthCondition.builder().fatLimitG(20.0).build();
    Meal meal = Meal.builder().sugarG(3.0).fatG(25.0).build();
    assertThat(service.isMealCompatible(meal, Set.of(c))).isFalse();
  }

  @Test
  void isMealCompatible_trueWhenBothUnderLimits() {
    HealthCondition c = HealthCondition.builder().sugarLimitG(10.0).fatLimitG(20.0).build();
    Meal meal = Meal.builder().sugarG(5.0).fatG(15.0).build();
    assertThat(service.isMealCompatible(meal, Set.of(c))).isTrue();
  }

  @Test
  void isMealCompatible_trueWhenConditionHasNoLimits() {
    HealthCondition c = HealthCondition.builder().sugarLimitG(null).fatLimitG(null).build();
    Meal meal = Meal.builder().sugarG(100.0).fatG(100.0).build();
    assertThat(service.isMealCompatible(meal, Set.of(c))).isTrue();
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
