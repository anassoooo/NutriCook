package com.nutricook.domain.energy;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.fixtures.TestRules;
import com.nutricook.domain.model.ActivityLevel;
import com.nutricook.domain.model.Goal;
import com.nutricook.domain.model.NutritionProfile;
import com.nutricook.domain.model.Sex;
import com.nutricook.domain.model.Targets;
import java.time.LocalDate;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;

/** Invariants that must hold for every valid adult profile, not just the reference cases. */
class TargetPropertiesTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);
  private final TargetCalculator calc = new TargetCalculator(TestRules.nutrition());

  private Targets targets(
      Sex sex, int age, double kg, double cm, ActivityLevel activity, Goal goal) {
    return calc.compute(
        new NutritionProfile(TODAY.minusYears(age), sex, kg, cm, activity, goal), TODAY);
  }

  @Property(tries = 1000)
  void BR_MAC_04_carbsNeverNegativeAndMacrosAddUpToTheTarget(
      @ForAll Sex sex,
      @ForAll @IntRange(min = 18, max = 100) int age,
      @ForAll @DoubleRange(min = 30, max = 300) double kg,
      @ForAll @DoubleRange(min = 120, max = 230) double cm,
      @ForAll ActivityLevel activity,
      @ForAll Goal goal) {
    Targets t = targets(sex, age, kg, cm, activity, goal);

    assertThat(t.carbsG()).isGreaterThanOrEqualTo(0);
    int macroKcal = 4 * t.proteinG() + 4 * t.carbsG() + 9 * t.fatG();
    // each macro is rounded once by at most 0.5 g: 4*0.5 + 4*0.5 + 9*0.5 = 8.5 kcal
    assertThat(Math.abs(macroKcal - t.kcal())).isLessThanOrEqualTo(9);
  }

  @Property(tries = 1000)
  void BR_NRG_04_targetNeverBelowBmrOrSexMinimum(
      @ForAll Sex sex,
      @ForAll @IntRange(min = 18, max = 100) int age,
      @ForAll @DoubleRange(min = 30, max = 300) double kg,
      @ForAll @DoubleRange(min = 120, max = 230) double cm,
      @ForAll ActivityLevel activity,
      @ForAll Goal goal) {
    Targets t = targets(sex, age, kg, cm, activity, goal);

    int sexMinimum = sex == Sex.MALE ? 1500 : 1200;
    assertThat(t.kcal()).isGreaterThanOrEqualTo(sexMinimum);
    assertThat((double) t.kcal()).isGreaterThanOrEqualTo(t.bmr());
    assertThat(t.kcal() % 10).isZero();
  }

  @Property(tries = 1000)
  void BR_MAC_02_proteinWithinCaps(
      @ForAll Sex sex,
      @ForAll @IntRange(min = 18, max = 100) int age,
      @ForAll @DoubleRange(min = 30, max = 300) double kg,
      @ForAll @DoubleRange(min = 120, max = 230) double cm,
      @ForAll ActivityLevel activity,
      @ForAll Goal goal) {
    Targets t = targets(sex, age, kg, cm, activity, goal);

    // +0.5 g allowance for the final rounding
    assertThat((double) t.proteinG()).isLessThanOrEqualTo(2.2 * kg + 0.5);
    assertThat((double) t.proteinG()).isLessThanOrEqualTo(0.35 * t.kcal() / 4 + 0.5);
  }
}
