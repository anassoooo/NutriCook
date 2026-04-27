package com.nutricook.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProgressResponse {
  private Long id;
  private LocalDate date;
  private Double weightKg;
  private Integer caloriesConsumed;
  private Integer waterMl;
  private Integer mealsCompleted;
  private Integer exerciseMinutes;
  private String notes;
}
