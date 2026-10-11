package com.nutricook.domain.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutricook.domain.model.AchievementType;
import com.nutricook.domain.model.Goal;
import com.nutricook.domain.rules.TrackingRules;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** BR-ACH-01..05 on synthetic histories. */
class EvaluatorsTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);
  private static final EvaluatorRegistry REGISTRY =
      EvaluatorRegistry.standard(new TrackingRules(30, 2000));

  private static DayRecord logged(int daysAgo) {
    return new DayRecord(TODAY.minusDays(daysAgo), null, null, null, null);
  }

  private static DayRecord ate(int daysAgo, int eaten, Integer target) {
    return new DayRecord(TODAY.minusDays(daysAgo), eaten, target, null, null);
  }

  private static DayRecord drank(int daysAgo, int ml) {
    return new DayRecord(TODAY.minusDays(daysAgo), null, null, ml, null);
  }

  private static DayRecord weighed(int daysAgo, double kg) {
    return new DayRecord(TODAY.minusDays(daysAgo), null, null, null, kg);
  }

  private static EvaluationContext ctx(List<DayRecord> days) {
    return new EvaluationContext(TODAY, days, 0, null, null);
  }

  private static double value(AchievementType type, EvaluationContext ctx) {
    return REGISTRY.currentValue(type, ctx);
  }

  /** Days ago that have an entry, e.g. "0 1 2". */
  private static List<DayRecord> history(String daysAgo) {
    List<DayRecord> days = new ArrayList<>();
    if (!daysAgo.isBlank()) {
      for (String d : daysAgo.trim().split(" ")) days.add(logged(Integer.parseInt(d)));
    }
    return days;
  }

  // ── BR-ACH-01 ──────────────────────────────────────────────────────────────

  @ParameterizedTest(name = "logged [{0}] -> streak {1}")
  @CsvSource({
    "'0 1 2 3 4 5 6', 7",
    "'1 2 3 4 5 6 7', 7", // today not logged yet: the streak runs through yesterday
    "'0',             1",
    "'1',             1",
    "'',              0"
  })
  void BR_ACH_01_streakGraceUntilEndOfDay(String daysAgo, int streak) {
    assertThat(value(AchievementType.LOGGING_STREAK, ctx(history(daysAgo)))).isEqualTo(streak);
  }

  @ParameterizedTest(name = "logged [{0}] -> streak {1}")
  @CsvSource({
    "'0 1 3 4 5', 2", // a missed day ends the streak
    "'2 3 4',     0", // neither today nor yesterday: no current streak
    "'0 2',       1"
  })
  void BR_ACH_01_gapBreaksStreak(String daysAgo, int streak) {
    assertThat(value(AchievementType.LOGGING_STREAK, ctx(history(daysAgo)))).isEqualTo(streak);
  }

  @Test
  void BR_ACH_01_futureEntriesDoNotExtendTheStreak() {
    List<DayRecord> days = new ArrayList<>(history("0 1"));
    days.add(new DayRecord(TODAY.plusDays(1), null, null, null, null));
    assertThat(value(AchievementType.LOGGING_STREAK, ctx(days))).isEqualTo(2);
  }

  // ── BR-ACH-02 ──────────────────────────────────────────────────────────────

  @Test
  void BR_ACH_02_supersededNotCounted() {
    // the service counts distinct plan days; regenerations of a day are one day
    EvaluationContext ctx = new EvaluationContext(TODAY, List.of(), 3, null, null);
    assertThat(value(AchievementType.PLANS_GENERATED, ctx)).isEqualTo(3);
    assertThat(REGISTRY.earned(AchievementType.PLANS_GENERATED, 5, ctx)).isFalse();
    assertThat(REGISTRY.earned(AchievementType.PLANS_GENERATED, 1, ctx)).isTrue();
  }

  // ── BR-ACH-03 ──────────────────────────────────────────────────────────────

  @ParameterizedTest(name = "ate {0} of {1} -> counts={2}")
  @CsvSource({
    "2000, 2000, true",
    "2200, 2000, true", // +10% edge
    "1800, 2000, true", // −10% edge
    "2201, 2000, false",
    "1799, 2000, false",
    "0,    2000, false", // nothing eaten is not "on target"
    "2000,     , false" // no target snapshot: cannot be judged
  })
  void BR_ACH_03_onlyWithin10PercentCounts(int eaten, Integer target, boolean counts) {
    double v = value(AchievementType.CALORIE_GOAL, ctx(List.of(ate(0, eaten, target))));
    assertThat(v).isEqualTo(counts ? 1 : 0);
  }

  @Test
  void BR_ACH_03_rolling30Days() {
    List<DayRecord> days = List.of(ate(0, 2000, 2000), ate(29, 2000, 2000), ate(30, 2000, 2000));
    assertThat(value(AchievementType.CALORIE_GOAL, ctx(days))).isEqualTo(2);
  }

  // ── BR-ACH-04 ──────────────────────────────────────────────────────────────

  @Test
  void BR_ACH_04_hydrationRolling30() {
    List<DayRecord> days =
        List.of(
            drank(0, 2000), // threshold counts
            drank(1, 1999), // just below
            drank(10, 3500),
            drank(29, 2000), // last day of the window
            drank(30, 4000)); // outside the window
    assertThat(value(AchievementType.HYDRATION, ctx(days))).isEqualTo(3);
  }

  // ── BR-ACH-05 ──────────────────────────────────────────────────────────────

  @ParameterizedTest(name = "{0}: {1} -> {2} kg gives {3}")
  @CsvSource({
    "LOSE_WEIGHT,     80.0, 77.9, 2.1",
    "LOSE_WEIGHT,     80.0, 82.0, 0.0", // wrong direction never counts
    "GAIN_MUSCLE,     60.0, 65.0, 5.0",
    "GAIN_MUSCLE,     60.0, 58.0, 0.0",
    "MAINTAIN_WEIGHT, 80.0, 75.0, 0.0", // no direction to reward
    "IMPROVE_HEALTH,  80.0, 75.0, 0.0",
    "LOSE_WEIGHT,     80.0, 78.05, 1.9" // rounded down: 1.95 is not 2
  })
  void BR_ACH_05_weightMilestoneDirectionAware(
      Goal goal, double first, double latest, double expected) {
    EvaluationContext ctx =
        new EvaluationContext(
            TODAY, List.of(weighed(40, first), weighed(3, latest)), 0, first, goal);
    assertThat(value(AchievementType.WEIGHT_MILESTONE, ctx)).isEqualTo(expected);
  }

  @Test
  void BR_ACH_05_latestWeightIsTheMostRecentWeighIn() {
    List<DayRecord> days = List.of(weighed(1, 76), weighed(5, 79), logged(0));
    EvaluationContext ctx = new EvaluationContext(TODAY, days, 0, 80.0, Goal.LOSE_WEIGHT);
    assertThat(value(AchievementType.WEIGHT_MILESTONE, ctx)).isEqualTo(4.0);
  }

  @Test
  void BR_ACH_05_noWeightMeansNoProgress() {
    EvaluationContext ctx = new EvaluationContext(TODAY, List.of(), 0, null, Goal.LOSE_WEIGHT);
    assertThat(value(AchievementType.WEIGHT_MILESTONE, ctx)).isZero();
  }

  @ParameterizedTest
  @EnumSource(AchievementType.class)
  void BR_ACH_05_everyTypeHasEvaluator(AchievementType type) {
    assertThat(value(type, ctx(List.of()))).isGreaterThanOrEqualTo(0);
  }

  @Test
  void BR_ACH_05_registryRefusesAMissingOrDuplicateEvaluator() {
    assertThatThrownBy(() -> new EvaluatorRegistry(List.of(new LoggingStreakEvaluator())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No evaluator for");
    assertThatThrownBy(
            () ->
                new EvaluatorRegistry(
                    List.of(new LoggingStreakEvaluator(), new LoggingStreakEvaluator())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Two evaluators");
  }
}
