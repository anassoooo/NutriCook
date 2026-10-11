package com.nutricook.domain.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** BR-PRG-01..06. */
class ProgressRulesTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

  private static LogUpdate calories(Integer kcal) {
    return new LogUpdate(kcal, false, null, null, null, null);
  }

  private static final LogUpdate RESET_CALORIES = new LogUpdate(null, true, null, null, null, null);

  @Test
  void BR_PRG_01_mergeKeepsOmittedFields() {
    DayLog current = new DayLog(1800, true, 1500, 30, 72.5, "felt good", 3, 2100);

    DayLog merged =
        ProgressRules.merge(current, new LogUpdate(null, false, 2000, null, null, null));

    assertThat(merged).isEqualTo(new DayLog(1800, true, 2000, 30, 72.5, "felt good", 3, 2100));
  }

  @Test
  void BR_PRG_01_emptyNotesClearTheNotes() {
    DayLog current = DayLog.EMPTY;
    current = ProgressRules.merge(current, new LogUpdate(null, false, null, null, null, "tired"));
    assertThat(current.notes()).isEqualTo("tired");

    current = ProgressRules.merge(current, new LogUpdate(null, false, null, null, null, " "));
    assertThat(current.notes()).isNull();
  }

  @ParameterizedTest(name = "today{0}: allowed={1}")
  @CsvSource({"0, true", "1, false", "-1, true", "-30, true", "-31, false", "-365, false"})
  void BR_PRG_02_dateWindow(int offset, boolean allowed) {
    LocalDate date = TODAY.plusDays(offset);
    if (allowed) {
      assertThatCode(() -> ProgressRules.assertLoggable(date, TODAY, 30))
          .doesNotThrowAnyException();
    } else {
      assertThatThrownBy(() -> ProgressRules.assertLoggable(date, TODAY, 30))
          .isInstanceOfSatisfying(
              DomainException.class,
              e -> {
                assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                assertThat(e.details())
                    .containsEntry("field", "date")
                    .containsEntry("minDate", "2026-09-11")
                    .containsEntry("maxDate", "2026-10-11");
              });
    }
  }

  @ParameterizedTest(name = "{0}={1}: valid={2}")
  @CsvSource({
    "caloriesConsumed, 0,      true",
    "caloriesConsumed, 10000,  true",
    "caloriesConsumed, -1,     false",
    "caloriesConsumed, 10001,  false",
    "waterMl,          0,      true",
    "waterMl,          10000,  true",
    "waterMl,          -1,     false",
    "waterMl,          10001,  false",
    "exerciseMinutes,  0,      true",
    "exerciseMinutes,  1440,   true",
    "exerciseMinutes,  -1,     false",
    "exerciseMinutes,  1441,   false",
    "weightKg,         30,     true",
    "weightKg,         300,    true",
    "weightKg,         29.9,   false",
    "weightKg,         300.1,  false",
    "weightKg,         NaN,    false",
    "notes,            1000,   true",
    "notes,            1001,   false"
  })
  void BR_PRG_03_bounds(String field, String value, boolean valid) {
    LogUpdate u =
        switch (field) {
          case "caloriesConsumed" -> calories(Integer.parseInt(value));
          case "waterMl" -> new LogUpdate(null, false, Integer.parseInt(value), null, null, null);
          case "exerciseMinutes" -> new LogUpdate(
              null, false, null, Integer.parseInt(value), null, null);
          case "weightKg" -> new LogUpdate(
              null, false, null, null, Double.parseDouble(value), null);
          case "notes" -> new LogUpdate(
              null, false, null, null, null, "x".repeat(Integer.parseInt(value)));
          default -> throw new IllegalArgumentException(field);
        };
    if (valid) {
      assertThatCode(() -> ProgressRules.assertWithinBounds(u)).doesNotThrowAnyException();
    } else {
      assertThatThrownBy(() -> ProgressRules.assertWithinBounds(u))
          .isInstanceOfSatisfying(
              DomainException.class,
              e -> {
                assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                assertThat(errorFields(e)).containsExactly(field);
              });
    }
  }

  @Test
  void BR_PRG_03_reportsEveryInvalidField() {
    LogUpdate u = new LogUpdate(-5, true, 20_000, 2_000, 10.0, null);
    assertThatThrownBy(() -> ProgressRules.assertWithinBounds(u))
        .isInstanceOfSatisfying(
            DomainException.class,
            e ->
                assertThat(errorFields(e))
                    .containsExactly(
                        "caloriesConsumed",
                        "waterMl",
                        "exerciseMinutes",
                        "weightKg",
                        "resetCalories"));
  }

  @Test
  void BR_PRG_04_onlyTheLatestWeighInUpdatesTheProfile() {
    assertThat(ProgressRules.isLatestWeighIn(TODAY, null)).isTrue();
    assertThat(ProgressRules.isLatestWeighIn(TODAY, TODAY.minusDays(3))).isTrue();
    assertThat(ProgressRules.isLatestWeighIn(TODAY.minusDays(5), TODAY.minusDays(3))).isFalse();
  }

  @Test
  void BR_PRG_05_derivedCalories() {
    PlanDay plan = new PlanDay(2200, List.of(520, 180));

    DayLog day = ProgressRules.derive(DayLog.EMPTY, plan, 2000);

    assertThat(day.mealsCompleted()).isEqualTo(2);
    assertThat(day.caloriesConsumed()).isEqualTo(700);
    assertThat(day.caloriesOverridden()).isFalse();
  }

  @Test
  void BR_PRG_05_nothingCheckedIsZeroButNoPlanIsUnknown() {
    assertThat(ProgressRules.derive(DayLog.EMPTY, new PlanDay(2200, List.of()), null))
        .extracting(DayLog::caloriesConsumed, DayLog::mealsCompleted)
        .containsExactly(0, 0);
    assertThat(ProgressRules.derive(DayLog.EMPTY, null, null))
        .extracting(DayLog::caloriesConsumed, DayLog::mealsCompleted)
        .containsExactly(null, 0);
  }

  @Test
  void BR_PRG_05_overrideWins() {
    PlanDay plan = new PlanDay(2200, List.of(520, 180));

    DayLog typed = ProgressRules.merge(DayLog.EMPTY, calories(2500));
    DayLog day = ProgressRules.derive(typed, plan, null);

    assertThat(day.caloriesOverridden()).isTrue();
    assertThat(day.caloriesConsumed()).isEqualTo(2500);
    assertThat(day.mealsCompleted()).isEqualTo(2); // still derived

    // checking another meal does not move a manual number
    day = ProgressRules.derive(day, new PlanDay(2200, List.of(520, 180, 640)), null);
    assertThat(day.caloriesConsumed()).isEqualTo(2500);
    assertThat(day.mealsCompleted()).isEqualTo(3);
  }

  @Test
  void BR_PRG_05_resetReturnsToDerivedCalories() {
    PlanDay plan = new PlanDay(2200, List.of(520, 180));
    DayLog day =
        ProgressRules.derive(ProgressRules.merge(DayLog.EMPTY, calories(2500)), plan, null);

    day = ProgressRules.derive(ProgressRules.merge(day, RESET_CALORIES), plan, null);

    assertThat(day.caloriesOverridden()).isFalse();
    assertThat(day.caloriesConsumed()).isEqualTo(700);
  }

  @Test
  void BR_PRG_05_otherFieldsKeepAnOverride() {
    DayLog day = ProgressRules.merge(DayLog.EMPTY, calories(2500));
    day = ProgressRules.merge(day, new LogUpdate(null, false, 500, null, null, null));
    assertThat(day.caloriesOverridden()).isTrue();
    assertThat(day.caloriesConsumed()).isEqualTo(2500);
  }

  @Test
  void BR_PRG_06_targetSnapshotFromPlanElseProfile() {
    // the plan decides when there is one
    assertThat(
            ProgressRules.derive(DayLog.EMPTY, new PlanDay(2200, List.of()), 1900).targetCalories())
        .isEqualTo(2200);
    // otherwise a new day takes the profile's target
    DayLog noPlan = ProgressRules.derive(DayLog.EMPTY, null, 1900);
    assertThat(noPlan.targetCalories()).isEqualTo(1900);
    // and keeps it when the profile changes later
    assertThat(ProgressRules.derive(noPlan, null, 1700).targetCalories()).isEqualTo(1900);
    // a plan generated later for that day replaces it
    assertThat(ProgressRules.derive(noPlan, new PlanDay(2050, List.of()), 1700).targetCalories())
        .isEqualTo(2050);
    // an incomplete profile gives no target
    assertThat(ProgressRules.derive(DayLog.EMPTY, null, null).targetCalories()).isNull();
  }

  @SuppressWarnings("unchecked")
  private static List<String> errorFields(DomainException e) {
    return ((List<Map<String, String>>) e.details().get("errors"))
        .stream().map(m -> m.get("field")).toList();
  }
}
