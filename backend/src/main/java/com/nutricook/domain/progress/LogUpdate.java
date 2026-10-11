package com.nutricook.domain.progress;

/**
 * A request to change a day's log. {@code null} fields are left as they are (BR-PRG-01).
 *
 * @param caloriesConsumed a number typed by the user: it overrides the derived value (BR-PRG-05)
 * @param resetCalories go back to calories derived from checked meals
 * @param notes an empty string clears the notes
 */
public record LogUpdate(
    Integer caloriesConsumed,
    boolean resetCalories,
    Integer waterMl,
    Integer exerciseMinutes,
    Double weightKg,
    String notes) {}
