package com.nutricook.domain.fixtures;

import static com.nutricook.domain.model.Allergen.*;
import static com.nutricook.domain.model.DietFlag.*;

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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hand-built foods for domain tests. Values per 100 g and flags mirror db/catalog/foods.csv, so a
 * test about "Peanut Butter" is about the real catalog entry.
 */
public final class TestFoods {

  private static final AtomicLong IDS = new AtomicLong(1000);
  private static final Set<DietFlag> PLANT = EnumSet.of(VEGAN, VEGETARIAN, HALAL, KOSHER);
  private static final Set<DietFlag> PLANT_PROCESSED = EnumSet.of(VEGAN, VEGETARIAN, HALAL);

  private TestFoods() {}

  /** General factory: verified allergens, all slots, 5 g steps from 5 to 500 g. */
  public static FoodView food(
      String name,
      String category,
      FoodRole role,
      Nutrients per100g,
      Set<Allergen> allergens,
      Set<DietFlag> flags,
      boolean meat) {
    return new FoodView(
        IDS.incrementAndGet(),
        name,
        category,
        role,
        FoodState.COOKED,
        per100g,
        allergens,
        true,
        flags,
        meat,
        new PortionRule(5, 500, 5, 100),
        EnumSet.allOf(Slot.class));
  }

  private static Nutrients n(
      double kcal, double p, double c, double f, double sugar, double fiber, double sodium) {
    return new Nutrients(kcal, p, c, f, sugar, fiber, sodium);
  }

  public static FoodView chickenBreast() {
    return new FoodView(
        1,
        "Chicken Breast",
        "meat",
        FoodRole.PROTEIN,
        FoodState.COOKED,
        n(165, 31, 0, 3.6, 0, 0, 74),
        Set.of(),
        true,
        Set.of(),
        true,
        new PortionRule(50, 250, 5, 150),
        EnumSet.of(Slot.LUNCH, Slot.DINNER));
  }

  public static FoodView chickenBreastHalal() {
    return food(
        "Chicken Breast (halal)",
        "meat",
        FoodRole.PROTEIN,
        n(165, 31, 0, 3.6, 0, 0, 74),
        Set.of(),
        EnumSet.of(HALAL),
        true);
  }

  public static FoodView salmon() {
    return food(
        "Salmon",
        "seafood",
        FoodRole.PROTEIN,
        n(206, 22.1, 0, 12.4, 0, 0, 61),
        EnumSet.of(FISH),
        EnumSet.of(HALAL, KOSHER),
        false);
  }

  public static FoodView shrimp() {
    return food(
        "Shrimp",
        "seafood",
        FoodRole.PROTEIN,
        n(99, 24, 0.2, 0.3, 0, 0, 111),
        EnumSet.of(CRUSTACEANS),
        Set.of(),
        false);
  }

  public static FoodView octopus() {
    return food(
        "Octopus",
        "seafood",
        FoodRole.PROTEIN,
        n(164, 29.8, 4.4, 2.1, 0, 0, 460),
        EnumSet.of(MOLLUSCS),
        Set.of(),
        false);
  }

  public static FoodView greekYogurt() {
    return food(
        "Greek Yogurt (plain; nonfat)",
        "dairy",
        FoodRole.PROTEIN,
        n(59, 10.2, 3.6, 0.4, 3.2, 0, 36),
        EnumSet.of(MILK),
        EnumSet.of(VEGETARIAN, HALAL),
        false);
  }

  public static FoodView cheddar() {
    return food(
        "Cheddar Cheese",
        "dairy",
        FoodRole.FAT,
        n(404, 22.9, 3.1, 33.3, 0.3, 0, 653),
        EnumSet.of(MILK),
        Set.of(),
        false);
  }

  public static FoodView tofu() {
    return food(
        "Tofu (firm)",
        "legume",
        FoodRole.PROTEIN,
        n(144, 17.3, 2.8, 8.7, 0.6, 2.3, 14),
        EnumSet.of(SOY),
        PLANT_PROCESSED,
        false);
  }

  public static FoodView lentils() {
    return food(
        "Lentils",
        "legume",
        FoodRole.PROTEIN,
        n(116, 9, 20.1, 0.4, 1.8, 7.9, 2),
        Set.of(),
        PLANT,
        false);
  }

  public static FoodView seitan() {
    return food(
        "Seitan",
        "grain",
        FoodRole.PROTEIN,
        n(120, 21, 4, 1.5, 0, 1, 350),
        EnumSet.of(GLUTEN, SOY),
        PLANT_PROCESSED,
        false);
  }

  public static FoodView peanutButter() {
    // Category "legume": the old category-based filter let this through a nut allergy
    return food(
        "Peanut Butter",
        "legume",
        FoodRole.FAT,
        n(588, 25.1, 20, 50, 9.2, 6, 459),
        EnumSet.of(PEANUTS),
        PLANT_PROCESSED,
        false);
  }

  public static FoodView almonds() {
    return food(
        "Almonds",
        "nut",
        FoodRole.FAT,
        n(579, 21.2, 21.6, 49.9, 4.4, 12.5, 1),
        EnumSet.of(TREE_NUTS),
        PLANT,
        false);
  }

  public static FoodView tahini() {
    return food(
        "Tahini",
        "seed",
        FoodRole.FAT,
        n(595, 17, 21.2, 53.8, 0.5, 9.3, 115),
        EnumSet.of(SESAME),
        PLANT_PROCESSED,
        false);
  }

  public static FoodView whiteRice() {
    // Category "grain": the old filter wrongly excluded rice for Gluten-Free
    return food(
        "White Rice",
        "grain",
        FoodRole.CARB,
        n(130, 2.7, 28.2, 0.3, 0.1, 0.4, 1),
        Set.of(),
        PLANT,
        false);
  }

  public static FoodView quinoa() {
    return food(
        "Quinoa",
        "grain",
        FoodRole.CARB,
        n(120, 4.4, 21.3, 1.9, 0.9, 2.8, 7),
        Set.of(),
        PLANT,
        false);
  }

  public static FoodView cornTortilla() {
    return food(
        "Corn Tortilla",
        "grain",
        FoodRole.CARB,
        n(218, 5.7, 44.6, 2.9, 0.9, 6.3, 45),
        Set.of(),
        PLANT_PROCESSED,
        false);
  }

  public static FoodView couscous() {
    return food(
        "Couscous",
        "grain",
        FoodRole.CARB,
        n(112, 3.8, 23.2, 0.2, 0.1, 1.4, 5),
        EnumSet.of(GLUTEN),
        PLANT,
        false);
  }

  public static FoodView wholeWheatBread() {
    return food(
        "Whole Wheat Bread",
        "grain",
        FoodRole.CARB,
        n(252, 12.4, 42.7, 3.5, 4.4, 6, 455),
        EnumSet.of(GLUTEN),
        EnumSet.of(VEGETARIAN, HALAL),
        false);
  }

  public static FoodView mango() {
    return food(
        "Mango",
        "fruit",
        FoodRole.PRODUCE,
        n(60, 0.8, 15, 0.4, 13.7, 1.6, 1),
        Set.of(),
        PLANT,
        false);
  }

  public static FoodView broccoli() {
    return food(
        "Broccoli",
        "vegetable",
        FoodRole.PRODUCE,
        n(34, 2.8, 6.6, 0.4, 1.7, 2.6, 33),
        Set.of(),
        PLANT,
        false);
  }

  public static FoodView oliveOil() {
    return food(
        "Olive Oil", "fat", FoodRole.FAT, n(884, 0, 0, 100, 0, 0, 2), Set.of(), PLANT, false);
  }

  /** A legacy row whose allergens were never verified (BR-SAF-03). */
  public static FoodView unverified(String name) {
    return new FoodView(
        IDS.incrementAndGet(),
        name,
        "vegetable",
        FoodRole.PRODUCE,
        FoodState.RAW,
        n(30, 1, 6, 0.2, 2, 2, 10),
        Set.of(),
        false,
        PLANT,
        false,
        new PortionRule(50, 250, 5, 100),
        EnumSet.allOf(Slot.class));
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
