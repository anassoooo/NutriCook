package com.nutricook.domain.plan;

import static com.nutricook.domain.model.FoodRole.CARB;
import static com.nutricook.domain.model.FoodRole.FAT;
import static com.nutricook.domain.model.FoodRole.PRODUCE;
import static com.nutricook.domain.model.FoodRole.PROTEIN;
import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.fixtures.TestCatalog;
import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.Slot;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** BR-CMP-02/03: how one meal is sized. Worked examples use the real catalog values. */
class PortionSolverTest {

  private static Map<FoodRole, FoodView> picks(Object... roleAndName) {
    Map<FoodRole, FoodView> m = new EnumMap<>(FoodRole.class);
    for (int i = 0; i < roleAndName.length; i += 2) {
      m.put((FoodRole) roleAndName[i], TestCatalog.named((String) roleAndName[i + 1]));
    }
    return m;
  }

  private static double grams(MealDraft meal, String food) {
    return meal.components().stream()
        .filter(c -> c.food().name().equals(food))
        .mapToDouble(Component::grams)
        .findFirst()
        .orElseThrow();
  }

  /**
   * Reference profile A breakfast: 690 kcal, 24 g protein.
   *
   * <ol>
   *   <li>Banana at its default 125 g (111.25 kcal, 1.4 g protein).
   *   <li>Yogurt for (24 - 1.4) / 0.102 = 222 g -> 225 g. Oats fill (690 - 132.75 - 111.25) / 3.79
   *       = 118 g -> capped at 100 g, which adds 13.2 g protein.
   *   <li>Protein now 37.5 g > 24 x 1.15, so the yogurt is re-sized for (24 - 1.4 - 13.2) / 0.102 =
   *       92 g -> its 100 g minimum. Oats stay at 100 g.
   *   <li>Gap 140.75 kcal -> almonds 24.3 g -> 25 g. Total 694 kcal (+0.6 %).
   * </ol>
   */
  @Test
  void BR_CMP_02_mainMealWorkedExample() {
    SlotGoal goal = new SlotGoal(Slot.BREAKFAST, 690, 24);
    MealDraft meal =
        PortionSolver.solve(
            goal,
            picks(PROTEIN, "Greek Yogurt (plain; nonfat)", CARB, "Rolled Oats", PRODUCE, "Banana"),
            TestCatalog.named("Almonds"));

    assertThat(grams(meal, "Greek Yogurt (plain; nonfat)")).isEqualTo(100);
    assertThat(grams(meal, "Rolled Oats")).isEqualTo(100);
    assertThat(grams(meal, "Banana")).isEqualTo(125);
    assertThat(grams(meal, "Almonds")).isEqualTo(25);
    assertThat(meal.nutrients().kcal()).isEqualTo(694);
    assertThat(meal.nutrients().proteinG()).isEqualTo(30.1);
  }

  @Test
  void BR_CMP_02_snackFillsEnergyWithTheNonProduceFood() {
    // Apple at its default 150 g = 78 kcal; almonds fill (276 - 78) / 5.79 = 34.2 g -> 35 g
    MealDraft snack =
        PortionSolver.solve(
            new SlotGoal(Slot.MORNING_SNACK, 276, 9.6),
            picks(PRODUCE, "Apple", FAT, "Almonds"),
            null);

    assertThat(grams(snack, "Apple")).isEqualTo(150);
    assertThat(grams(snack, "Almonds")).isEqualTo(35);
    assertThat(snack.nutrients().kcal()).isEqualTo(281);
  }

  @Test
  void BR_CMP_02_fatIsOnlyAddedToCloseAGap() {
    // A small lunch is already met by protein + carb + produce: no oil is added
    MealDraft lunch =
        PortionSolver.solve(
            new SlotGoal(Slot.LUNCH, 360, 27),
            picks(PROTEIN, "Chicken Breast", CARB, "White Rice", PRODUCE, "Broccoli"),
            TestCatalog.named("Olive Oil"));

    assertThat(lunch.components()).extracting(Component::role).doesNotContain(FAT);
  }

  @Test
  void BR_CMP_03_portionsStayWithinBoundsAndSteps() {
    // An unreachable goal pushes everything to its maximum, never beyond
    MealDraft huge =
        PortionSolver.solve(
            new SlotGoal(Slot.DINNER, 5000, 300),
            picks(PROTEIN, "Salmon", CARB, "Brown Rice", PRODUCE, "Zucchini"),
            TestCatalog.named("Olive Oil"));

    for (Component c : huge.components()) {
      assertThat(c.food().portion().allows(c.grams())).as(c.food().name()).isTrue();
    }
    assertThat(grams(huge, "Brown Rice")).isEqualTo(300);
    assertThat(grams(huge, "Olive Oil")).isEqualTo(20);
  }

  @Test
  void BR_CMP_02_proteinNeverTakesMoreThanHalfTheMeal() {
    // Lentils for 40 g protein would be 444 g (515 kcal) in a 500 kcal lunch
    MealDraft lunch =
        PortionSolver.solve(
            new SlotGoal(Slot.LUNCH, 500, 40),
            picks(PROTEIN, "Lentils", CARB, "White Rice", PRODUCE, "Spinach"),
            null);

    double lentilKcal =
        lunch.components().stream()
            .filter(c -> c.role() == PROTEIN)
            .findFirst()
            .orElseThrow()
            .nutrients()
            .kcal();
    assertThat(lentilKcal).isLessThanOrEqualTo(0.5 * 500 + 15); // + one 25 g step of rounding
  }
}
