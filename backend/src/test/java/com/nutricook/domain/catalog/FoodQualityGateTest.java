package com.nutricook.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.fixtures.TestFoods;
import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.Nutrients;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FoodQualityGateTest {

  @Test
  void BR_CAT_02_fiberCountsTwoKcalPerGram() {
    // Spinach (USDA): plain 4/4/9 gives 29.6 kcal (29 % off); fiber-aware gives 25.2 (10 % off)
    Nutrients spinach = new Nutrients(23, 2.9, 3.6, 0.4, 0.4, 2.2, 79);
    assertThat(FoodQualityGate.expectedKcal(spinach))
        .isCloseTo(25.2, org.assertj.core.data.Offset.offset(1e-9));
    assertThat(FoodQualityGate.problems(spinach)).isEmpty();
  }

  @Test
  void BR_CAT_02_rejectsEnergyThatDoesNotMatchTheMacros() {
    Nutrients wrong = new Nutrients(500, 10, 20, 5, 1, 1, 10); // macros give ~163 kcal
    assertThat(FoodQualityGate.problems(wrong)).singleElement().asString().contains("energy 500");
  }

  @Test
  void BR_CAT_02_lowEnergyFoodsHaveATenKcalFloor() {
    // Mushrooms: 22 kcal vs 26.3 expected is 19.5 % but only 4.3 kcal off
    Nutrients mushrooms = new Nutrients(22, 3.1, 3.3, 0.3, 2.0, 1.0, 5);
    assertThat(FoodQualityGate.problems(mushrooms)).isEmpty();
  }

  @Test
  void BR_CAT_02_rejectsImpossibleComposition() {
    assertThat(FoodQualityGate.problems(new Nutrients(400, 50, 40, 20, 50, 10, 0)))
        .contains(
            "sugar exceeds carbohydrates", "protein + carbohydrates + fat exceed 100 g per 100 g");
    assertThat(FoodQualityGate.problems(new Nutrients(20, 1, 2, 0, 0, 5, 0)))
        .contains("fiber exceeds carbohydrates");
  }

  @Test
  void BR_CAT_02_dietLabelsMustNotContradictTheFood() {
    assertThat(FoodQualityGate.labelProblems(Set.of(), Set.of(DietFlag.VEGAN), false))
        .containsExactly("VEGAN foods must also be VEGETARIAN");
    assertThat(FoodQualityGate.labelProblems(Set.of(), Set.of(DietFlag.VEGETARIAN), true))
        .containsExactly("meat cannot be VEGETARIAN or VEGAN");
    assertThat(
            FoodQualityGate.labelProblems(
                Set.of(Allergen.FISH), Set.of(DietFlag.VEGETARIAN), false))
        .containsExactly("a food with fish or shellfish cannot be VEGETARIAN");
    assertThat(
            FoodQualityGate.labelProblems(
                Set.of(Allergen.MILK), Set.of(DietFlag.VEGAN, DietFlag.VEGETARIAN), false))
        .containsExactly("a food with an animal-derived allergen cannot be VEGAN");
    // dairy is vegetarian; halal meat is fine
    assertThat(
            FoodQualityGate.labelProblems(
                Set.of(Allergen.MILK), Set.of(DietFlag.VEGETARIAN), false))
        .isEmpty();
    assertThat(FoodQualityGate.labelProblems(Set.of(), Set.of(DietFlag.HALAL), true)).isEmpty();
  }

  @Test
  void BR_CAT_02_wholeFoodGatePassesCuratedFixtures() {
    for (var food :
        java.util.List.of(
            TestFoods.chickenBreast(), TestFoods.salmon(), TestFoods.tofu(), TestFoods.lentils())) {
      assertThat(FoodQualityGate.problems(food)).as(food.name()).isEmpty();
    }
  }
}
