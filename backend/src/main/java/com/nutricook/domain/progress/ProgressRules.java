package com.nutricook.domain.progress;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Progress tracking rules (business-rules.md §8): which days can be logged, which values are
 * plausible, how a new log merges into the day, and which numbers are derived from the plan.
 */
public final class ProgressRules {

  public static final int MAX_CALORIES = 10_000;
  public static final int MAX_WATER_ML = 10_000;
  public static final int MAX_EXERCISE_MINUTES = 1_440;
  public static final double MIN_WEIGHT_KG = 30;
  public static final double MAX_WEIGHT_KG = 300;
  public static final int MAX_NOTES_LENGTH = 1_000;

  private ProgressRules() {}

  /**
   * BR-PRG-02: a day can be logged from {@code today − backfillDays} to today, in the user's day.
   *
   * @throws DomainException VALIDATION_FAILED on a future or too old date
   */
  public static void assertLoggable(LocalDate date, LocalDate today, int backfillDays) {
    LocalDate oldest = today.minusDays(backfillDays);
    if (date.isAfter(today) || date.isBefore(oldest)) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          "Progress can be logged for today and the past " + backfillDays + " days.",
          Map.of("field", "date", "minDate", oldest.toString(), "maxDate", today.toString()));
    }
  }

  /**
   * BR-PRG-03: plausible values only. Every invalid field is reported, not just the first.
   *
   * @throws DomainException VALIDATION_FAILED listing the fields in {@code errors}
   */
  public static void assertWithinBounds(LogUpdate u) {
    List<Map<String, String>> errors = new ArrayList<>();
    if (u.caloriesConsumed() != null && !between(u.caloriesConsumed(), 0, MAX_CALORIES)) {
      errors.add(error("caloriesConsumed", "must be between 0 and " + MAX_CALORIES));
    }
    if (u.waterMl() != null && !between(u.waterMl(), 0, MAX_WATER_ML)) {
      errors.add(error("waterMl", "must be between 0 and " + MAX_WATER_ML));
    }
    if (u.exerciseMinutes() != null && !between(u.exerciseMinutes(), 0, MAX_EXERCISE_MINUTES)) {
      errors.add(error("exerciseMinutes", "must be between 0 and " + MAX_EXERCISE_MINUTES));
    }
    // written so that NaN fails too
    if (u.weightKg() != null && !(u.weightKg() >= MIN_WEIGHT_KG && u.weightKg() <= MAX_WEIGHT_KG)) {
      errors.add(error("weightKg", "must be between 30 and 300"));
    }
    if (u.notes() != null && u.notes().length() > MAX_NOTES_LENGTH) {
      errors.add(error("notes", "must be at most " + MAX_NOTES_LENGTH + " characters"));
    }
    if (u.caloriesConsumed() != null && u.resetCalories()) {
      errors.add(error("resetCalories", "cannot be combined with caloriesConsumed"));
    }
    if (!errors.isEmpty()) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Some fields are invalid.", Map.of("errors", errors));
    }
  }

  /**
   * BR-PRG-01: only the fields present in the update change. Typing calories makes them a manual
   * override; {@code resetCalories} hands them back to the checked meals (BR-PRG-05). Derived
   * fields are refreshed separately, by {@link #derive}.
   */
  public static DayLog merge(DayLog current, LogUpdate u) {
    boolean overridden =
        u.caloriesConsumed() != null || (!u.resetCalories() && current.caloriesOverridden());
    return new DayLog(
        u.caloriesConsumed() != null ? u.caloriesConsumed() : current.caloriesConsumed(),
        overridden,
        u.waterMl() != null ? u.waterMl() : current.waterMl(),
        u.exerciseMinutes() != null ? u.exerciseMinutes() : current.exerciseMinutes(),
        u.weightKg() != null ? u.weightKg() : current.weightKg(),
        u.notes() == null ? current.notes() : (u.notes().isBlank() ? null : u.notes()),
        current.mealsCompleted(),
        current.targetCalories());
  }

  /**
   * Recomputes what the plan decides.
   *
   * <ul>
   *   <li>BR-PRG-05: {@code mealsCompleted} is the number of checked meals. Unless overridden,
   *       {@code caloriesConsumed} is their sum; with no plan for the day it is unknown.
   *   <li>BR-PRG-06: the target is the plan's when there is one. Otherwise an existing snapshot is
   *       kept, so a later profile change does not move past days; a new day takes the profile's
   *       target, which may be unknown ({@code null}) for an incomplete profile.
   * </ul>
   *
   * @param plan the day's ACTIVE plan, or {@code null}
   * @param profileTarget the profile's current calorie target, or {@code null}
   */
  public static DayLog derive(DayLog log, PlanDay plan, Integer profileTarget) {
    int meals = plan == null ? 0 : plan.checkedMealKcal().size();
    Integer calories = log.caloriesConsumed();
    if (!log.caloriesOverridden()) {
      calories =
          plan == null ? null : plan.checkedMealKcal().stream().mapToInt(Integer::intValue).sum();
    }
    Integer target;
    if (plan != null) {
      target = plan.targetCalories();
    } else if (log.targetCalories() != null) {
      target = log.targetCalories();
    } else {
      target = profileTarget;
    }
    return new DayLog(
        calories,
        log.caloriesOverridden(),
        log.waterMl(),
        log.exerciseMinutes(),
        log.weightKg(),
        log.notes(),
        meals,
        target);
  }

  /**
   * BR-PRG-04: a weight logged for a day updates the profile when no later day has a weight. A
   * backfilled weight is history; it must not replace a more recent one.
   *
   * @param latestOtherWeighIn the latest date of another entry that has a weight, or {@code null}
   */
  public static boolean isLatestWeighIn(LocalDate date, LocalDate latestOtherWeighIn) {
    return latestOtherWeighIn == null || !date.isBefore(latestOtherWeighIn);
  }

  private static boolean between(int v, int min, int max) {
    return v >= min && v <= max;
  }

  private static Map<String, String> error(String field, String message) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("field", field);
    m.put("message", message);
    return m;
  }
}
