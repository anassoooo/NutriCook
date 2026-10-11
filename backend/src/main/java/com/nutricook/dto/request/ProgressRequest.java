package com.nutricook.dto.request;

import com.nutricook.domain.progress.LogUpdate;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/**
 * Logs a day. Omitted fields keep their value (BR-PRG-01); ranges are checked by {@code
 * ProgressRules} so every invalid field is reported at once (BR-PRG-03). {@code mealsCompleted} is
 * not accepted: it is derived from checked meals (BR-PRG-05).
 */
@Data
public class ProgressRequest {
  @NotNull private LocalDate date;

  /** Typing calories overrides the value derived from checked meals. */
  private Integer caloriesConsumed;

  /** {@code true} returns calories to the value derived from checked meals. */
  private boolean resetCalories;

  private Integer waterMl;
  private Integer exerciseMinutes;
  private Double weightKg;

  /** An empty string clears the notes. */
  private String notes;

  public LogUpdate toUpdate() {
    return new LogUpdate(
        caloriesConsumed, resetCalories, waterMl, exerciseMinutes, weightKg, notes);
  }
}
