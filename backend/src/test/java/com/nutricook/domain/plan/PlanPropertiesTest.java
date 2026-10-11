package com.nutricook.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.energy.TargetCalculator;
import com.nutricook.domain.fixtures.TestCatalog;
import com.nutricook.domain.fixtures.TestConstraints;
import com.nutricook.domain.fixtures.TestRules;
import com.nutricook.domain.model.*;
import com.nutricook.domain.safety.ConditionRule;
import com.nutricook.domain.safety.PlanSafetyValidator;
import com.nutricook.domain.safety.RestrictionRule;
import com.nutricook.domain.safety.SafetyConstraints;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;

/**
 * For any adult, any combination of restrictions and conditions, and any seed, the composer either
 * returns a safe, well-formed, reproducible plan or refuses with PLAN_INFEASIBLE (Constitution V).
 */
class PlanPropertiesTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 11);
  private final PlanComposer composer = new PlanComposer(TestRules.nutrition());
  private final TargetCalculator targets = new TargetCalculator(TestRules.nutrition());

  @Provide
  Arbitrary<Set<RestrictionRule>> restrictions() {
    return Arbitraries.subsetOf(
        TestConstraints.VEGAN,
        TestConstraints.VEGETARIAN,
        TestConstraints.GLUTEN_FREE,
        TestConstraints.LACTOSE_FREE,
        TestConstraints.NUT_ALLERGY,
        TestConstraints.SHELLFISH_ALLERGY,
        TestConstraints.EGG_ALLERGY,
        TestConstraints.SOY_ALLERGY,
        TestConstraints.FISH_ALLERGY,
        TestConstraints.SESAME_ALLERGY,
        TestConstraints.HALAL,
        TestConstraints.KOSHER);
  }

  @Provide
  Arbitrary<Set<ConditionRule>> conditions() {
    return Arbitraries.subsetOf(
        TestConstraints.DIABETES,
        TestConstraints.HIGH_CHOLESTEROL,
        TestConstraints.HYPERTENSION,
        TestConstraints.CELIAC,
        TestConstraints.LACTOSE_INTOLERANCE);
  }

  private PlanDraft composeOrNull(Targets t, SafetyConstraints c, long seed) {
    try {
      return composer.compose(t, c, TestCatalog.all(), DAY, seed);
    } catch (DomainException e) {
      assertThat(e.code()).isEqualTo(ErrorCode.PLAN_INFEASIBLE);
      return null;
    }
  }

  @Property(tries = 500)
  void BR_SAF_04_aPlanIsEitherSafeOrRefused(
      @ForAll Sex sex,
      @ForAll @IntRange(min = 18, max = 100) int age,
      @ForAll @DoubleRange(min = 40, max = 160) double kg,
      @ForAll @DoubleRange(min = 145, max = 210) double cm,
      @ForAll ActivityLevel activity,
      @ForAll Goal goal,
      @ForAll("restrictions") Set<RestrictionRule> restrictions,
      @ForAll("conditions") Set<ConditionRule> conditions,
      @ForAll long seed) {
    Targets t =
        targets.compute(
            new NutritionProfile(DAY.minusYears(age), sex, kg, cm, activity, goal), DAY);
    SafetyConstraints c = SafetyConstraints.from(restrictions, conditions);

    PlanDraft day = composeOrNull(t, c, seed);
    if (day == null) return;

    // BR-SAF-04/05: never a single violation
    assertThat(PlanSafetyValidator.validate(day, c)).isEmpty();
    // BR-CMP-03: every portion legal
    for (MealDraft m : day.meals()) {
      for (Component comp : m.components()) {
        assertThat(comp.food().portion().allows(comp.grams())).as(comp.food().name()).isTrue();
      }
    }
    // BR-CMP-05: never beyond ±10 %
    assertThat(Math.abs(day.totals().kcal() - t.kcal()) / t.kcal()).isLessThanOrEqualTo(0.10);
  }

  @Property(tries = 300)
  void BR_CMP_08_totalsAlwaysEqualTheSumOfTheMeals(
      @ForAll("restrictions") Set<RestrictionRule> restrictions, @ForAll long seed) {
    Targets t =
        targets.compute(
            new NutritionProfile(
                DAY.minusYears(35),
                Sex.FEMALE,
                70.0,
                170.0,
                ActivityLevel.MODERATELY_ACTIVE,
                Goal.MAINTAIN_WEIGHT),
            DAY);
    PlanDraft day = composeOrNull(t, SafetyConstraints.from(restrictions, List.of()), seed);
    if (day == null) return;

    Nutrients sum =
        Nutrients.sum(day.meals().stream().map(MealDraft::nutrients).toList()).rounded();
    assertThat(day.totals()).isEqualTo(sum);
  }

  @Property(tries = 200)
  void BR_CMP_07_compositionIsDeterministic(
      @ForAll("restrictions") Set<RestrictionRule> restrictions, @ForAll long seed) {
    Targets t =
        targets.compute(
            new NutritionProfile(
                DAY.minusYears(40),
                Sex.MALE,
                85.0,
                182.0,
                ActivityLevel.LIGHTLY_ACTIVE,
                Goal.LOSE_WEIGHT),
            DAY);
    SafetyConstraints c = SafetyConstraints.from(restrictions, List.of());

    assertThat(composeOrNull(t, c, seed)).isEqualTo(composeOrNull(t, c, seed));
  }
}
