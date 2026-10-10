package com.nutricook.domain.energy;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.fixtures.TestRules;
import com.nutricook.domain.model.Goal;
import org.junit.jupiter.api.Test;

/** BR-MAC-01..06. */
class MacroCalculatorTest {

  private final MacroCalculator calc = new MacroCalculator(TestRules.nutrition());

  @Test
  void BR_MAC_01_atwaterFactors() {
    assertThat(MacroCalculator.KCAL_PER_G_PROTEIN).isEqualTo(4);
    assertThat(MacroCalculator.KCAL_PER_G_CARB).isEqualTo(4);
    assertThat(MacroCalculator.KCAL_PER_G_FAT).isEqualTo(9);
  }

  @Test
  void BR_MAC_02_proteinPerKgByGoal() {
    assertThat(calc.compute(2760, 80, Goal.MAINTAIN_WEIGHT).proteinG()).isEqualTo(96); // 1.2
    assertThat(calc.compute(2760, 80, Goal.IMPROVE_HEALTH).proteinG()).isEqualTo(80); // 1.0
    assertThat(calc.compute(2760, 80, Goal.LOSE_WEIGHT).proteinG()).isEqualTo(144); // 1.8
    assertThat(calc.compute(2760, 80, Goal.GAIN_MUSCLE).proteinG()).isEqualTo(160); // 2.0
  }

  @Test
  void BR_MAC_02_proteinCappedAt35PercentOfEnergy() {
    // 2.0 g/kg * 100 kg = 200 g, but 35 % of 1800 kcal / 4 = 157.5 g
    assertThat(calc.compute(1800, 100, Goal.GAIN_MUSCLE).proteinG()).isEqualTo(158);
  }

  @Test
  void BR_MAC_03_fatIs30PercentOfEnergy() {
    assertThat(calc.compute(2760, 80, Goal.MAINTAIN_WEIGHT).fatG()).isEqualTo(92); // 828 / 9
  }

  @Test
  void BR_MAC_03_fatNeverBelowPoint6GramsPerKg() {
    // 30 % of 1500 / 9 = 50 g, but 0.6 * 120 kg = 72 g
    assertThat(calc.compute(1500, 120, Goal.LOSE_WEIGHT).fatG()).isEqualTo(72);
  }

  @Test
  void BR_MAC_04_carbsAreTheRemainderFromUnroundedValues() {
    // Case D: (3230 - 4*140 - 9*107.666...) / 4 = 425.25 -> 425
    // Case B: (1350 - 4*108 - 9*45) / 4 = 128.25 -> 128
    assertThat(calc.compute(3230, 70, Goal.GAIN_MUSCLE).carbsG()).isEqualTo(425);
    assertThat(calc.compute(1350, 60, Goal.LOSE_WEIGHT).carbsG()).isEqualTo(128);
  }

  @Test
  void BR_MAC_05_fiberIs14GramsPer1000Kcal() {
    assertThat(calc.compute(2760, 80, Goal.MAINTAIN_WEIGHT).fiberG()).isEqualTo(39); // 38.64
    assertThat(calc.compute(1200, 50, Goal.LOSE_WEIGHT).fiberG()).isEqualTo(17); // 16.8
  }

  @Test
  void BR_MAC_06_roundOnceAtTheEndHalfUp() {
    // Protein 1.8 * 62.5 = 112.5 -> 113 (half-up), not banker's rounding to 112
    assertThat(calc.compute(2000, 62.5, Goal.LOSE_WEIGHT).proteinG()).isEqualTo(113);
  }
}
