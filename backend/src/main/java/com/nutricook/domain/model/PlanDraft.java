package com.nutricook.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A day plan produced or changed by the domain. Totals are derived, never stored independently, so
 * they always equal the sum of the meals (BR-CMP-08).
 */
public record PlanDraft(LocalDate date, List<MealDraft> meals, List<Warning> warnings) {

  public PlanDraft {
    Objects.requireNonNull(date, "date");
    meals = List.copyOf(meals);
    warnings = List.copyOf(warnings);
  }

  /** Sum of the (already rounded) meal values, so the day total matches what the user sees. */
  public Nutrients totals() {
    return Nutrients.sum(meals.stream().map(MealDraft::nutrients).toList()).rounded();
  }
}
