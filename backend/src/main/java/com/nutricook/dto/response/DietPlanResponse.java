package com.nutricook.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DietPlanResponse {
  private Long id;
  private String title;
  private Integer totalCalories;
  private Double totalProteinG;
  private Double totalCarbsG;
  private Double totalFatG;
  private Double totalSugarG;
  private LocalDate validForDate;
  private LocalDateTime generatedAt;
  private String status;
  private String aiExplanation;
  private List<MealResponse> meals;
}
