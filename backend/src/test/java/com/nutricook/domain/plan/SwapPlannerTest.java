package com.nutricook.domain.plan;

import static com.nutricook.domain.fixtures.TestConstraints.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.fixtures.TestCatalog;
import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.PlanDraft;
import com.nutricook.domain.model.Slot;
import com.nutricook.domain.safety.SafetyConstraints;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** BR-AI-03 swap semantics, shared by manual and AI swaps. */
class SwapPlannerTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 11);

  /**
   * Breakfast: yogurt 200 g + oats 60 g + strawberries 150 g (14.4 g sugar, under the Diabetes
   * limit). Lunch: chicken 115 g + rice 300 g + broccoli 150 g.
   */
  private static PlanDraft day() {
    MealDraft breakfast =
        new MealDraft(
            Slot.BREAKFAST,
            List.of(
                new Component(TestCatalog.named("Greek Yogurt (plain; nonfat)"), 200),
                new Component(TestCatalog.named("Rolled Oats"), 60),
                new Component(TestCatalog.named("Strawberries"), 150)));
    MealDraft lunch =
        new MealDraft(
            Slot.LUNCH,
            List.of(
                new Component(TestCatalog.named("Chicken Breast"), 115),
                new Component(TestCatalog.named("White Rice"), 300),
                new Component(TestCatalog.named("Broccoli"), 150)));
    return new PlanDraft(DAY, List.of(breakfast, lunch), List.of());
  }

  private static SwapPlanner.Result swap(Slot slot, String food, SafetyConstraints c) {
    return SwapPlanner.swap(day(), slot, TestCatalog.named(food), c);
  }

  @Test
  void BR_AI_03_onlyTheComponentWithTheSameRoleChanges() {
    var result = (SwapPlanner.Swapped) swap(Slot.LUNCH, "Tofu (firm)", SafetyConstraints.NONE);

    List<String> foods = result.meal().components().stream().map(c -> c.food().name()).toList();
    assertThat(foods).containsExactly("Tofu (firm)", "White Rice", "Broccoli");
    // Breakfast is untouched
    assertThat(result.plan().meals().get(0)).isEqualTo(day().meals().get(0));
  }

  @Test
  void BR_AI_03_theNewPortionKeepsTheComponentsEnergyWithinBounds() {
    // Chicken 115 g = 189.75 kcal -> tofu 189.75 / 1.44 = 131.8 g -> step 25 -> 125 g
    var result = (SwapPlanner.Swapped) swap(Slot.LUNCH, "Tofu (firm)", SafetyConstraints.NONE);
    Component tofu = result.meal().components().get(0);

    assertThat(tofu.grams()).isEqualTo(125);
    assertThat(tofu.food().portion().allows(tofu.grams())).isTrue();
  }

  @Test
  void BR_CMP_08_totalsFollowTheSwap() {
    var result = (SwapPlanner.Swapped) swap(Slot.LUNCH, "Tofu (firm)", SafetyConstraints.NONE);
    Nutrients sum =
        Nutrients.sum(result.plan().meals().stream().map(MealDraft::nutrients).toList()).rounded();
    assertThat(result.plan().totals()).isEqualTo(sum);
  }

  @Test
  void BR_AI_03_aFoodWithNoMatchingRoleIsRejected() {
    // Lunch has no FAT component to replace with olive oil
    var result = (SwapPlanner.Rejected) swap(Slot.LUNCH, "Olive Oil", SafetyConstraints.NONE);
    assertThat(result.reason()).isEqualTo(SwapPlanner.Reason.NO_SAME_ROLE);
    assertThat(result.message()).isEqualTo("The lunch has no fat to replace with Olive Oil.");
  }

  @Test
  void BR_SAF_05_anIneligibleFoodIsRejected() {
    var result =
        (SwapPlanner.Rejected)
            swap(
                Slot.BREAKFAST,
                "Peanut Butter",
                SafetyConstraints.from(List.of(NUT_ALLERGY), List.of()));
    assertThat(result.reason()).isEqualTo(SwapPlanner.Reason.NOT_ELIGIBLE);
    assertThat(result.message()).contains("excluded by Nut Allergy");
  }

  @Test
  void BR_SAF_06_aSwapThatBreaksAHealthLimitIsRejected() {
    SafetyConstraints diabetes = SafetyConstraints.from(List.of(), List.of(DIABETES));
    // The day starts within the limit, so only the swap can break it
    assertThat(com.nutricook.domain.safety.PlanSafetyValidator.validate(day(), diabetes)).isEmpty();

    // Strawberries 150 g = 48 kcal -> mango 80 g -> 75 g (its minimum) = 10.3 g sugar,
    // + yogurt 6.4 g + oats 0.6 g = 17.3 g > 15 g
    var result = (SwapPlanner.Rejected) swap(Slot.BREAKFAST, "Mango", diabetes);
    assertThat(result.reason()).isEqualTo(SwapPlanner.Reason.VIOLATES_LIMIT);
    assertThat(result.message()).contains("17.3 g of sugar").contains("Diabetes");
  }

  @Test
  void BR_CMP_04_aFoodNotOfferedForTheMealIsRejected() {
    // Rolled oats are a breakfast food only
    var result = (SwapPlanner.Rejected) swap(Slot.LUNCH, "Rolled Oats", SafetyConstraints.NONE);
    assertThat(result.reason()).isEqualTo(SwapPlanner.Reason.NOT_SUITABLE_FOR_MEAL);
  }

  @Test
  void swappingInTheSameFoodIsRejected() {
    var result = (SwapPlanner.Rejected) swap(Slot.LUNCH, "Chicken Breast", SafetyConstraints.NONE);
    assertThat(result.reason()).isEqualTo(SwapPlanner.Reason.SAME_FOOD);
  }

  @Test
  void BR_AI_03_alternativesAreAcceptedSwapsClosestInEnergyFirst() {
    SafetyConstraints nuts = SafetyConstraints.from(List.of(NUT_ALLERGY, HALAL), List.of());
    List<Component> alts =
        SwapPlanner.alternatives(day(), Slot.LUNCH, FoodRole.PROTEIN, TestCatalog.all(), nuts, 8);

    assertThat(alts).isNotEmpty().hasSizeLessThanOrEqualTo(8);
    for (Component alt : alts) {
      assertThat(SwapPlanner.swap(day(), Slot.LUNCH, alt.food(), nuts))
          .isInstanceOf(SwapPlanner.Swapped.class);
      assertThat(alt.food().role()).isEqualTo(FoodRole.PROTEIN);
    }
    double target = day().meals().get(1).components().get(0).nutrients().kcal();
    List<Double> distances =
        alts.stream().map(a -> Math.abs(a.nutrients().kcal() - target)).toList();
    assertThat(distances).isSorted();
  }
}
