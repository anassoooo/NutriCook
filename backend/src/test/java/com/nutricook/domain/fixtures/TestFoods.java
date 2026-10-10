package com.nutricook.domain.fixtures;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodState;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.PortionRule;
import com.nutricook.domain.model.Slot;
import java.util.EnumSet;
import java.util.Set;

/** Hand-built foods for domain tests. Values per 100 g (USDA FoodData Central, cooked). */
public final class TestFoods {

  private TestFoods() {}

  public static FoodView chickenBreast() {
    return new FoodView(
        1,
        "Chicken Breast",
        "meat",
        FoodRole.PROTEIN,
        FoodState.COOKED,
        new Nutrients(165, 31, 0, 3.6, 0, 0, 74),
        Set.of(),
        true,
        Set.of(),
        true,
        new PortionRule(50, 250, 5, 150),
        EnumSet.of(Slot.LUNCH, Slot.DINNER));
  }

  public static FoodView withKcalPer100g(String name, double kcal) {
    return new FoodView(
        99,
        name,
        "test",
        FoodRole.PRODUCE,
        FoodState.RAW,
        new Nutrients(kcal, 0, 0, 0, 0, 0, 0),
        Set.<Allergen>of(),
        true,
        EnumSet.of(DietFlag.VEGAN, DietFlag.VEGETARIAN),
        false,
        new PortionRule(5, 500, 5, 100),
        EnumSet.allOf(Slot.class));
  }
}
