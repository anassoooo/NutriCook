package com.nutricook.domain.safety;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodState;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.PortionRule;
import com.nutricook.domain.model.Slot;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Safety invariants over random foods and random restriction sets, not just hand-picked cases
 * (Constitution V).
 */
class SafetyPropertiesTest {

  @Provide
  Arbitrary<FoodView> foods() {
    Arbitrary<Set<Allergen>> allergens = Arbitraries.of(Allergen.class).set().ofMaxSize(4);
    Arbitrary<Set<DietFlag>> flags = Arbitraries.of(DietFlag.class).set();
    Arbitrary<Boolean> known = Arbitraries.of(true, true, true, false);
    Arbitrary<Boolean> meat = Arbitraries.of(true, false);
    return Combinators.combine(allergens, flags, known, meat)
        .as(
            (a, f, k, m) ->
                new FoodView(
                    1,
                    "Random food",
                    "random",
                    FoodRole.PROTEIN,
                    FoodState.COOKED,
                    new Nutrients(100, 10, 5, 4, 1, 1, 50),
                    a,
                    k,
                    f,
                    m,
                    new PortionRule(5, 500, 5, 100),
                    EnumSet.allOf(Slot.class)));
  }

  @Provide
  Arbitrary<SafetyConstraints> constraints() {
    Arbitrary<List<RestrictionRule>> restrictions =
        Combinators.combine(
                Arbitraries.of(Allergen.class).set().ofMaxSize(3),
                Arbitraries.of(DietFlag.class).injectNull(0.5))
            .as((a, f) -> new RestrictionRule("Random restriction", a, f))
            .list()
            .ofMaxSize(3);
    Arbitrary<List<ConditionRule>> conditions =
        Arbitraries.of(Allergen.class)
            .set()
            .ofMaxSize(2)
            .map(a -> new ConditionRule("Random condition", null, null, null, a))
            .list()
            .ofMaxSize(2);
    return Combinators.combine(restrictions, conditions).as(SafetyConstraints::from);
  }

  @Property(tries = 1000)
  void BR_SAF_05_policyAndValidatorAgree(
      @ForAll("foods") FoodView food, @ForAll("constraints") SafetyConstraints c) {
    MealDraft meal = new MealDraft(Slot.LUNCH, List.of(new Component(food, 100)));
    List<Violation> mealViolations = new ArrayList<>(PlanSafetyValidator.validateMeal(meal, c));
    mealViolations.removeIf(v -> v.type() == Violation.Type.KOSHER_MEAT_WITH_MILK);

    assertThat(mealViolations.isEmpty()).isEqualTo(EligibilityPolicy.allows(food, c));
  }

  @Property(tries = 1000)
  void BR_SAF_02_anAllowedFoodNeverContainsAnExcludedAllergen(
      @ForAll("foods") FoodView food, @ForAll("constraints") SafetyConstraints c) {
    if (EligibilityPolicy.allows(food, c)) {
      for (Allergen excluded : c.excludedAllergens().keySet()) {
        assertThat(food.allergens()).doesNotContain(excluded);
      }
      assertThat(food.dietFlags()).containsAll(c.requiredFlags().keySet());
      if (c.failClosedOnUnknownAllergens()) assertThat(food.allergensKnown()).isTrue();
    }
  }
}
