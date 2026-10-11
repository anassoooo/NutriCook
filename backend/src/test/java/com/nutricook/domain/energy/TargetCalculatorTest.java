package com.nutricook.domain.energy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.fixtures.TestRules;
import com.nutricook.domain.model.ActivityLevel;
import com.nutricook.domain.model.Goal;
import com.nutricook.domain.model.NutritionProfile;
import com.nutricook.domain.model.Sex;
import com.nutricook.domain.model.Targets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Reference cases A-D, age (BR-TIM-02), completeness and eligibility (BR-PRF-01/02). */
class TargetCalculatorTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

  private final TargetCalculator calc = new TargetCalculator(TestRules.nutrition());

  /** business-rules.md §2 reference table. Ages are exact on TODAY. */
  @ParameterizedTest(name = "case {0}")
  @CsvSource({
    "A, MALE,   30, 80, 180, MODERATELY_ACTIVE, MAINTAIN_WEIGHT, 1780.00, 2759.00, 2760,  96,  92, 387, 39, false",
    "B, FEMALE, 25, 60, 165, LIGHTLY_ACTIVE,    LOSE_WEIGHT,     1345.25, 1849.72, 1350, 108,  45, 128, 19, false",
    "C, FEMALE, 60, 50, 155, SEDENTARY,         LOSE_WEIGHT,     1007.75, 1209.30, 1200,  90,  40, 120, 17, true",
    "D, MALE,   20, 70, 175, VERY_ACTIVE,       GAIN_MUSCLE,     1698.75, 2930.34, 3230, 140, 108, 425, 45, false"
  })
  void BR_NRG_ReferenceCases(
      String name,
      Sex sex,
      int age,
      double kg,
      double cm,
      ActivityLevel activity,
      Goal goal,
      double bmr,
      double tdee,
      int kcal,
      int protein,
      int fat,
      int carbs,
      int fiber,
      boolean floorApplied) {
    NutritionProfile p = new NutritionProfile(TODAY.minusYears(age), sex, kg, cm, activity, goal);

    Targets t = calc.compute(p, TODAY);

    assertThat(t.bmr()).isCloseTo(bmr, within(0.005));
    assertThat(t.tdee()).isCloseTo(tdee, within(0.005));
    assertThat(t.kcal()).isEqualTo(kcal);
    assertThat(t.proteinG()).isEqualTo(protein);
    assertThat(t.fatG()).isEqualTo(fat);
    assertThat(t.carbsG()).isEqualTo(carbs);
    assertThat(t.fiberG()).isEqualTo(fiber);
    assertThat(t.floorApplied()).isEqualTo(floorApplied);
  }

  @Test
  void BR_TIM_02_ageDoesNotIncrementBeforeTheBirthday() {
    assertThat(TargetCalculator.ageOn(LocalDate.of(1996, 10, 11), TODAY)).isEqualTo(29);
    assertThat(TargetCalculator.ageOn(LocalDate.of(1996, 10, 10), TODAY)).isEqualTo(30);
  }

  @Test
  void BR_TIM_02_leapDayBirthdayCountsFromMarchFirstInCommonYears() {
    LocalDate born = LocalDate.of(2004, 2, 29);
    assertThat(TargetCalculator.ageOn(born, LocalDate.of(2025, 2, 28))).isEqualTo(20);
    assertThat(TargetCalculator.ageOn(born, LocalDate.of(2025, 3, 1))).isEqualTo(21);
  }

  @Test
  void BR_TIM_02_birthdayTomorrowChangesTheTarget() {
    // One day before the 31st birthday, age is still 30, so BMR is 5 kcal higher than at 31
    NutritionProfile p =
        new NutritionProfile(
            TODAY.plusDays(1).minusYears(31),
            Sex.MALE,
            80.0,
            180.0,
            ActivityLevel.MODERATELY_ACTIVE,
            Goal.MAINTAIN_WEIGHT);
    assertThat(calc.compute(p, TODAY).bmr()).isEqualTo(1780.0);
    assertThat(calc.compute(p, TODAY.plusDays(1)).bmr()).isEqualTo(1775.0);
  }

  @Test
  void BR_PRF_01_eachMissingFieldIsReportedByItsApiName() {
    NutritionProfile empty = new NutritionProfile(null, null, null, null, null, null);

    assertThatThrownBy(() -> calc.compute(empty, TODAY))
        .isInstanceOfSatisfying(
            DomainException.class,
            e -> {
              assertThat(e.code()).isEqualTo(ErrorCode.PROFILE_INCOMPLETE);
              assertThat(e.details().get("missingFields"))
                  .isEqualTo(
                      List.of(
                          "dateOfBirth",
                          "gender",
                          "weightKg",
                          "heightCm",
                          "activityLevel",
                          "healthGoal"));
            });
  }

  @Test
  void BR_PRF_01_onlyTheMissingFieldsAreListed() {
    NutritionProfile p =
        new NutritionProfile(
            TODAY.minusYears(30), Sex.MALE, 80.0, null, ActivityLevel.SEDENTARY, null);

    assertThatThrownBy(() -> calc.compute(p, TODAY))
        .isInstanceOfSatisfying(
            DomainException.class,
            e ->
                assertThat(e.details().get("missingFields"))
                    .isEqualTo(List.of("heightCm", "healthGoal")));
  }

  @Test
  void BR_PRF_01_storedValuesOutsideTheAllowedRangesCountAsMissing() {
    // 25 kg was accepted by the old 20-500 kg bounds but is outside BR-PRF-03 (30-300 kg)
    NutritionProfile p =
        new NutritionProfile(
            TODAY.minusYears(30), Sex.MALE, 25.0, 180.0, ActivityLevel.SEDENTARY, Goal.LOSE_WEIGHT);

    assertThatThrownBy(() -> calc.compute(p, TODAY))
        .isInstanceOfSatisfying(
            DomainException.class,
            e -> assertThat(e.details().get("missingFields")).isEqualTo(List.of("weightKg")));
  }

  @ParameterizedTest(name = "born {0} -> eligible={1}")
  @CsvSource({
    "2008-10-11, false", // 17 years 364 days
    "2008-10-10, true", // 18 today
    "1926-10-10, true", // 100
    "1925-10-10, false", // 101
    "2030-01-01, false" // future date of birth
  })
  void BR_PRF_02_onlyAdultsAged18To100(LocalDate dateOfBirth, boolean eligible) {
    NutritionProfile p =
        new NutritionProfile(
            dateOfBirth, Sex.FEMALE, 60.0, 165.0, ActivityLevel.SEDENTARY, Goal.MAINTAIN_WEIGHT);

    if (eligible) {
      assertThat(calc.compute(p, TODAY).kcal()).isPositive();
    } else {
      assertThatThrownBy(() -> calc.compute(p, TODAY))
          .isInstanceOfSatisfying(
              DomainException.class,
              e -> assertThat(e.code()).isEqualTo(ErrorCode.PROFILE_INELIGIBLE));
    }
  }

  @Test
  void BR_NRG_06_sameInputsAlwaysGiveTheSameTargets() {
    NutritionProfile p =
        new NutritionProfile(
            TODAY.minusYears(30),
            Sex.MALE,
            80.0,
            180.0,
            ActivityLevel.MODERATELY_ACTIVE,
            Goal.MAINTAIN_WEIGHT);
    assertThat(calc.compute(p, TODAY)).isEqualTo(calc.compute(p, TODAY));
  }
}
