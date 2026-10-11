package com.nutricook.domain.progress;

/**
 * What a user logged for one day (BR-PRG-01). {@code null} means "not logged".
 *
 * @param caloriesConsumed derived from checked meals unless {@code caloriesOverridden} (BR-PRG-05)
 * @param mealsCompleted always derived: the number of checked meals (BR-PRG-05)
 * @param targetCalories the target the day is judged against (BR-PRG-06)
 */
public record DayLog(
    Integer caloriesConsumed,
    boolean caloriesOverridden,
    Integer waterMl,
    Integer exerciseMinutes,
    Double weightKg,
    String notes,
    int mealsCompleted,
    Integer targetCalories) {

  /** A day nothing was logged for yet. */
  public static final DayLog EMPTY = new DayLog(null, false, null, null, null, null, 0, null);
}
