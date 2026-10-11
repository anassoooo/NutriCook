package com.nutricook.dto.response;

import com.nutricook.entity.ProgressEntry;
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

  /** {@code true} when the user typed the calories; {@code false} when they come from meals. */
  private boolean caloriesOverridden;

  private Integer waterMl;
  private Integer mealsCompleted;
  private Integer exerciseMinutes;
  private Integer targetCalories;
  private String notes;

  public static ProgressResponse of(ProgressEntry e) {
    return ProgressResponse.builder()
        .id(e.getId())
        .date(e.getDate())
        .weightKg(e.getWeightKg())
        .caloriesConsumed(e.getCaloriesConsumed())
        .caloriesOverridden(e.isCaloriesOverridden())
        .waterMl(e.getWaterMl())
        .mealsCompleted(e.getMealsCompleted())
        .exerciseMinutes(e.getExerciseMinutes())
        .targetCalories(e.getTargetCalories())
        .notes(e.getNotes())
        .build();
  }
}
