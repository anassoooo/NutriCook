package com.nutricook.domain.plan;

import static com.nutricook.domain.fixtures.TestConstraints.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.energy.TargetCalculator;
import com.nutricook.domain.fixtures.TestCatalog;
import com.nutricook.domain.fixtures.TestRules;
import com.nutricook.domain.model.*;
import com.nutricook.domain.safety.SafetyConstraints;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** BR-CMP-01..07 and BR-SAF-04 on the real 84-food catalog. */
class PlanComposerTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 11);
  private final PlanComposer composer = new PlanComposer(TestRules.nutrition());
  private final TargetCalculator targets = new TargetCalculator(TestRules.nutrition());

  /** Reference profiles from business-rules.md §2. */
  private Targets reference(char name) {
    return switch (name) {
      case 'A' -> targets(
          Sex.MALE, 30, 80, 180, ActivityLevel.MODERATELY_ACTIVE, Goal.MAINTAIN_WEIGHT);
      case 'B' -> targets(Sex.FEMALE, 25, 60, 165, ActivityLevel.LIGHTLY_ACTIVE, Goal.LOSE_WEIGHT);
      case 'C' -> targets(Sex.FEMALE, 60, 50, 155, ActivityLevel.SEDENTARY, Goal.LOSE_WEIGHT);
      case 'D' -> targets(Sex.MALE, 20, 70, 175, ActivityLevel.VERY_ACTIVE, Goal.GAIN_MUSCLE);
      default -> throw new IllegalArgumentException();
    };
  }

  private Targets targets(Sex sex, int age, double kg, double cm, ActivityLevel a, Goal g) {
    return targets.compute(new NutritionProfile(DAY.minusYears(age), sex, kg, cm, a, g), DAY);
  }

  private PlanDraft compose(Targets t, SafetyConstraints c, long seed) {
    return composer.compose(t, c, TestCatalog.all(), DAY, seed);
  }

  private static List<FoodRole> roles(MealDraft m) {
    return m.components().stream().map(Component::role).sorted().toList();
  }

  @Test
  void BR_CMP_02_mainMealsHaveOneProteinCarbAndProduce() {
    for (long seed = 0; seed < 20; seed++) {
      for (MealDraft m : compose(reference('A'), SafetyConstraints.NONE, seed).meals()) {
        if (!m.slot().isMain()) continue;
        List<FoodRole> roles = roles(m);
        assertThat(roles)
            .as(m.slot() + " seed " + seed)
            .containsOnlyOnce(FoodRole.PROTEIN, FoodRole.CARB, FoodRole.PRODUCE);
        assertThat(roles.stream().filter(r -> r == FoodRole.FAT).count()).isLessThanOrEqualTo(1);
      }
    }
  }

  @Test
  void BR_CMP_02_snacksAreProteinWithProduceOrProduceWithFat() {
    Set<List<FoodRole>> allowed =
        Set.of(
            List.of(FoodRole.PROTEIN, FoodRole.PRODUCE), List.of(FoodRole.PRODUCE, FoodRole.FAT));
    for (long seed = 0; seed < 20; seed++) {
      for (MealDraft m : compose(reference('B'), SafetyConstraints.NONE, seed).meals()) {
        if (m.slot().isMain()) continue;
        assertThat(allowed).as(m.slot() + " seed " + seed).contains(roles(m));
      }
    }
  }

  @Test
  void BR_CMP_03_everyPortionIsLegal() {
    for (long seed = 0; seed < 20; seed++) {
      for (MealDraft m : compose(reference('D'), SafetyConstraints.NONE, seed).meals()) {
        for (Component c : m.components()) {
          assertThat(c.food().portion().allows(c.grams())).as(c.food().name()).isTrue();
        }
      }
    }
  }

  @Test
  void BR_CMP_04_foodsOnlyAppearInTheirSuitableSlots() {
    for (long seed = 0; seed < 20; seed++) {
      for (MealDraft m : compose(reference('A'), SafetyConstraints.NONE, seed).meals()) {
        for (Component c : m.components()) {
          assertThat(c.food().suitableSlots()).as(c.food().name()).contains(m.slot());
        }
      }
    }
  }

  @ParameterizedTest(name = "profile {0}")
  @ValueSource(chars = {'A', 'B', 'C', 'D'})
  void BR_CMP_05_referenceProfilesLandWithin5Percent(char profile) {
    Targets t = reference(profile);
    for (long seed = 0; seed < 30; seed++) {
      PlanDraft day = compose(t, SafetyConstraints.NONE, seed);
      double deviation = Math.abs(day.totals().kcal() - t.kcal()) / t.kcal();
      assertThat(deviation).as("seed " + seed).isLessThanOrEqualTo(0.05);
      assertThat(day.totals().proteinG())
          .as("protein, seed " + seed)
          .isGreaterThanOrEqualTo(0.9 * t.proteinG());
      assertThat(day.warnings()).isEmpty();
    }
  }

  @Test
  void BR_CMP_05_dayClassificationBoundaries() {
    assertThat(composer.classify(0.05)).isEqualTo(PlanComposer.DayFit.OK);
    assertThat(composer.classify(-0.05)).isEqualTo(PlanComposer.DayFit.OK);
    assertThat(composer.classify(0.0501)).isEqualTo(PlanComposer.DayFit.WARNING);
    assertThat(composer.classify(-0.10)).isEqualTo(PlanComposer.DayFit.WARNING);
    assertThat(composer.classify(0.1001)).isEqualTo(PlanComposer.DayFit.INFEASIBLE);
  }

  @Test
  void BR_CMP_05_infeasibleBeyond10Percent() {
    // A catalog whose largest portions cannot reach a 3000 kcal day
    List<FoodView> tiny = new ArrayList<>();
    long id = 1;
    for (FoodRole role : FoodRole.values())
      for (int copy = 0; copy < 4; copy++) {
        tiny.add(
            new FoodView(
                id,
                "Tiny " + role + " " + id++,
                "test",
                role,
                FoodState.RAW,
                new Nutrients(50, 5, 5, 1, 0, 0, 0),
                Set.of(),
                true,
                Set.of(),
                false,
                new PortionRule(10, 50, 5, 20),
                EnumSet.allOf(Slot.class)));
      }
    Targets big = new Targets(3000, 120, 400, 100, 42, 2000, 3000, false);

    assertThatThrownBy(() -> composer.compose(big, SafetyConstraints.NONE, tiny, DAY, 1))
        .isInstanceOfSatisfying(
            DomainException.class,
            e -> {
              assertThat(e.code()).isEqualTo(ErrorCode.PLAN_INFEASIBLE);
              assertThat(e.details()).containsEntry("constraint", "calorie target");
            });
  }

  @Test
  void BR_CMP_05_lowProteinIsReportedHonestly() {
    // Strict plant-based, soy-free, gluten-free, nut-free at 1200 kcal cannot reach 90 g protein
    SafetyConstraints strict =
        SafetyConstraints.from(List.of(VEGAN, GLUTEN_FREE, SOY_ALLERGY, NUT_ALLERGY), List.of());
    PlanDraft day = compose(reference('C'), strict, 1);
    assertThat(day.warnings()).extracting(Warning::code).contains(WarningCode.LOW_PROTEIN);
  }

  @Test
  void BR_CMP_06_proteinFoodOncePerDayAndAnyFoodAtMostTwice() {
    for (long seed = 0; seed < 30; seed++) {
      Map<String, Integer> uses = new HashMap<>();
      Set<String> proteins = new HashSet<>();
      for (MealDraft m : compose(reference('A'), SafetyConstraints.NONE, seed).meals()) {
        for (Component c : m.components()) {
          uses.merge(c.food().name(), 1, Integer::sum);
          if (c.role() == FoodRole.PROTEIN) {
            assertThat(proteins.add(c.food().name())).as(c.food().name()).isTrue();
          }
        }
      }
      assertThat(uses.values()).allSatisfy(n -> assertThat(n).isLessThanOrEqualTo(2));
    }
  }

  @Test
  void BR_CMP_07_sameSeedGivesTheSamePlan() {
    long seed = PlanSeed.of(42, DAY, 1);
    assertThat(compose(reference('A'), SafetyConstraints.NONE, seed))
        .isEqualTo(compose(reference('A'), SafetyConstraints.NONE, seed));
  }

  @Test
  void BR_CMP_07_nextGenerationGivesADifferentPlan() {
    assertThat(compose(reference('A'), SafetyConstraints.NONE, PlanSeed.of(42, DAY, 1)))
        .isNotEqualTo(compose(reference('A'), SafetyConstraints.NONE, PlanSeed.of(42, DAY, 2)));
  }

  @Test
  void BR_CMP_07_seedIsStableAcrossRuns() {
    // A fixed mix, not Object.hashCode: this value must never change between JVMs or releases
    assertThat(PlanSeed.of(42, LocalDate.of(2026, 10, 11), 1))
        .isEqualTo(PlanSeed.of(42, LocalDate.of(2026, 10, 11), 1));
    assertThat(PlanSeed.of(42, DAY, 1)).isNotEqualTo(PlanSeed.of(43, DAY, 1));
    assertThat(PlanSeed.of(42, DAY, 1)).isNotEqualTo(PlanSeed.of(42, DAY.plusDays(1), 1));
  }

  @Test
  void BR_SAF_04_aMissingRoleIsInfeasibleAndNamesTheRole() {
    List<FoodView> noProtein =
        TestCatalog.all().stream().filter(f -> f.role() != FoodRole.PROTEIN).toList();

    assertThatThrownBy(
            () -> composer.compose(reference('A'), SafetyConstraints.NONE, noProtein, DAY, 1))
        .isInstanceOfSatisfying(
            DomainException.class,
            e -> {
              assertThat(e.code()).isEqualTo(ErrorCode.PLAN_INFEASIBLE);
              assertThat(e.details()).containsEntry("constraint", "no eligible protein");
            });
  }

  @Test
  void BR_SAF_04_composerFiltersIneligibleFoodsItself() {
    // Even when handed the full catalog, a nut-allergic user's plan has no nuts or peanuts
    SafetyConstraints nuts = SafetyConstraints.from(List.of(NUT_ALLERGY), List.of());
    for (long seed = 0; seed < 30; seed++) {
      for (MealDraft m : compose(reference('A'), nuts, seed).meals()) {
        for (Component c : m.components()) {
          assertThat(c.food().allergens())
              .as(c.food().name())
              .doesNotContain(Allergen.PEANUTS, Allergen.TREE_NUTS);
        }
      }
    }
  }
}
