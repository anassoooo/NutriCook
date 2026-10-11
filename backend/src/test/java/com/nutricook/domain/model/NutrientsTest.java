package com.nutricook.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutricook.domain.fixtures.TestFoods;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NutrientsTest {

  @Test
  void BR_CMP_09_componentNutritionScalesPer100g() {
    // Chicken breast per 100 g: 165 kcal, 31 g protein, 3.6 g fat, 74 mg sodium. 150 g portion.
    Component c = new Component(TestFoods.chickenBreast(), 150);

    Nutrients n = c.nutrients();

    assertThat(n.kcal()).isEqualTo(247.5);
    assertThat(n.proteinG()).isEqualTo(46.5);
    assertThat(n.fatG()).isCloseTo(5.4, org.assertj.core.data.Offset.offset(1e-9));
    assertThat(n.sodiumMg()).isEqualTo(111.0);
  }

  @Test
  void BR_CMP_09_mealRoundsKcalToIntegerAndGramsToOneDecimalHalfUp() {
    MealDraft meal =
        new MealDraft(Slot.LUNCH, List.of(new Component(TestFoods.chickenBreast(), 150)));

    Nutrients n = meal.nutrients();

    assertThat(n.kcal()).isEqualTo(248.0); // 247.5 rounds half-up
    assertThat(n.proteinG()).isEqualTo(46.5);
    assertThat(n.fatG()).isEqualTo(5.4);
    assertThat(n.sodiumMg()).isEqualTo(111.0);
  }

  @Test
  void BR_CMP_09_roundingHappensOnceAtMealLevelNotPerComponent() {
    // Two 5 g portions of a 5 kcal/100 g food are 0.25 kcal each and would round to 0 per
    // component,
    // but the meal (0.5 kcal) must round to 1.
    FoodView tiny = TestFoods.withKcalPer100g("Tiny", 5);
    MealDraft meal =
        new MealDraft(Slot.MORNING_SNACK, List.of(new Component(tiny, 5), new Component(tiny, 5)));

    assertThat(meal.nutrients().kcal()).isEqualTo(1.0);
  }

  @Test
  void BR_CMP_08_planTotalsEqualSumOfMealTotals() {
    MealDraft lunch =
        new MealDraft(Slot.LUNCH, List.of(new Component(TestFoods.chickenBreast(), 150)));
    MealDraft dinner =
        new MealDraft(Slot.DINNER, List.of(new Component(TestFoods.chickenBreast(), 100)));
    PlanDraft plan = new PlanDraft(LocalDate.of(2026, 10, 10), List.of(lunch, dinner), List.of());

    Nutrients totals = plan.totals();

    assertThat(totals.kcal()).isEqualTo(lunch.nutrients().kcal() + dinner.nutrients().kcal());
    assertThat(totals.proteinG()).isEqualTo(77.5);
  }

  @Test
  void sumOfNothingIsZero() {
    assertThat(Nutrients.sum(List.of())).isEqualTo(Nutrients.ZERO);
  }

  @Test
  void negativeOrNonFiniteValuesAreRejected() {
    assertThatThrownBy(() -> new Nutrients(-1, 0, 0, 0, 0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Nutrients(Double.NaN, 0, 0, 0, 0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Nutrients.ZERO.scale(-0.5))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
