package com.nutricook.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MealResponse {
  private Long id;
  private String name;
  private String mealType;
  private Integer calories;
  private Double proteinG;
  private Double carbsG;
  private Double fatG;
  private Double sugarG;
  private Double fiberG;
  private Integer sortOrder;
  private String aiSuggestion;
}
