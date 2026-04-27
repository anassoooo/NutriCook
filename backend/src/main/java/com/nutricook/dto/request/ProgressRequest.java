package com.nutricook.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ProgressRequest {
  @NotNull private LocalDate date;

  @Min(0)
  private Integer caloriesConsumed;

  @Min(0)
  private Integer waterMl;

  @Min(0)
  private Integer mealsCompleted;

  @Min(0)
  private Integer exerciseMinutes;

  private Double weightKg;
  private String notes;
}
