package com.nutricook.domain.energy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.nutricook.domain.fixtures.TestRules;
import com.nutricook.domain.model.ActivityLevel;
import com.nutricook.domain.model.Goal;
import com.nutricook.domain.model.Sex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** BR-NRG-01..05. All expected values are worked out by hand in business-rules.md §2. */
class EnergyCalculatorTest {

  private final EnergyCalculator calc = new EnergyCalculator(TestRules.nutrition());

  @Test
  void BR_NRG_01_bmrUsesMifflinStJeorForMen() {
    // 10*80 + 6.25*180 - 5*30 + 5
    assertThat(calc.bmr(Sex.MALE, 80, 180, 30)).isEqualTo(1780.0);
  }

  @Test
  void BR_NRG_01_bmrUsesMifflinStJeorForWomen() {
    // 10*60 + 6.25*165 - 5*25 - 161
    assertThat(calc.bmr(Sex.FEMALE, 60, 165, 25)).isEqualTo(1345.25);
  }

  @Test
  void BR_NRG_01_unspecifiedSexUsesTheMidpointConstant() {
    // 10*70 + 6.25*170 - 5*40 - 78: halfway between the male (+5) and female (-161) results
    double unspecified = calc.bmr(Sex.UNSPECIFIED, 70, 170, 40);
    assertThat(unspecified).isEqualTo(1484.5);
    assertThat(unspecified)
        .isEqualTo((calc.bmr(Sex.MALE, 70, 170, 40) + calc.bmr(Sex.FEMALE, 70, 170, 40)) / 2);
  }

  @ParameterizedTest(name = "{0} -> x{1}")
  @CsvSource({
    "SEDENTARY,         1.2",
    "LIGHTLY_ACTIVE,    1.375",
    "MODERATELY_ACTIVE, 1.55",
    "VERY_ACTIVE,       1.725",
    "EXTRA_ACTIVE,      1.9"
  })
  void BR_NRG_02_tdeeMultipliesBmrByActivityFactor(ActivityLevel level, double factor) {
    EnergyCalculator.EnergyTarget t =
        calc.target(Sex.MALE, 80, 180, 30, level, Goal.MAINTAIN_WEIGHT);
    assertThat(t.tdee()).isCloseTo(1780.0 * factor, within(1e-9));
  }

  @ParameterizedTest(name = "{0}: {1} kcal")
  @CsvSource({
    "MAINTAIN_WEIGHT, 2760", // TDEE 2759 -> 2760
    "IMPROVE_HEALTH,  2760",
    "LOSE_WEIGHT,     2260", // 2759 - 500 = 2259 -> 2260
    "GAIN_MUSCLE,     3060" // 2759 + 300 = 3059 -> 3060
  })
  void BR_NRG_03_goalAdjustsTdee(Goal goal, int expectedKcal) {
    assertThat(calc.target(Sex.MALE, 80, 180, 30, ActivityLevel.MODERATELY_ACTIVE, goal).kcal())
        .isEqualTo(expectedKcal);
  }

  @Test
  void BR_NRG_04_maleMinimumIs1500() {
    // BMR 1255, TDEE 1506, -500 = 1006 -> floor max(1500, 1255) = 1500
    EnergyCalculator.EnergyTarget t =
        calc.target(Sex.MALE, 55, 160, 60, ActivityLevel.SEDENTARY, Goal.LOSE_WEIGHT);
    assertThat(t.kcal()).isEqualTo(1500);
    assertThat(t.floorApplied()).isTrue();
  }

  @Test
  void BR_NRG_04_floorIsBmrWhenBmrExceedsTheSexMinimum() {
    // BMR 1682.75 > 1200, TDEE 2019.3, -500 = 1519.3 -> floored to BMR
    EnergyCalculator.EnergyTarget t =
        calc.target(Sex.FEMALE, 90, 175, 30, ActivityLevel.SEDENTARY, Goal.LOSE_WEIGHT);
    assertThat(t.floorApplied()).isTrue();
    assertThat(t.kcal()).isGreaterThanOrEqualTo((int) Math.ceil(t.bmr()));
  }

  @Test
  void BR_NRG_04_noFloorWhenTargetIsAboveIt() {
    assertThat(
            calc.target(Sex.MALE, 80, 180, 30, ActivityLevel.MODERATELY_ACTIVE, Goal.LOSE_WEIGHT)
                .floorApplied())
        .isFalse();
  }

  @Test
  void BR_NRG_05_roundsToNearestTen() {
    // Reference case B: 1849.72 - 500 = 1349.72 -> 1350
    assertThat(
            calc.target(Sex.FEMALE, 60, 165, 25, ActivityLevel.LIGHTLY_ACTIVE, Goal.LOSE_WEIGHT)
                .kcal())
        .isEqualTo(1350);
  }

  @Test
  void BR_NRG_05_roundingNeverDropsBelowTheFloor() {
    // Floor = BMR 1682.75; nearest ten would be 1680 (< BMR), so round up to 1690
    EnergyCalculator.EnergyTarget t =
        calc.target(Sex.FEMALE, 90, 175, 30, ActivityLevel.SEDENTARY, Goal.LOSE_WEIGHT);
    assertThat(t.bmr()).isEqualTo(1682.75);
    assertThat(t.kcal()).isEqualTo(1690);
  }
}
