package com.nutricook.dto.response;

import com.nutricook.entity.Meal;
import java.util.Comparator;
import java.util.List;
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
  private Double sodiumMg;
  private Integer sortOrder;
  private String aiSuggestion;

  /** Checked off by the user (BR-PRG-05). */
  private boolean completed;

  /** The foods and portions in this meal: protein, carb, produce, then fat (BR-CMP-02). */
  private List<ComponentResponse> components;

  public static MealResponse of(Meal meal) {
    return MealResponse.builder()
        .id(meal.getId())
        .name(meal.getName())
        .mealType(meal.getMealType().name())
        .calories(meal.getCalories())
        .proteinG(meal.getProteinG())
        .carbsG(meal.getCarbsG())
        .fatG(meal.getFatG())
        .sugarG(meal.getSugarG())
        .fiberG(meal.getFiberG())
        .sodiumMg(meal.getSodiumMg())
        .sortOrder(meal.getSortOrder())
        .aiSuggestion(meal.getAiSuggestion())
        .completed(meal.isCompleted())
        .components(
            meal.getMealFoodItems().stream()
                .map(ComponentResponse::of)
                .sorted(Comparator.comparing(c -> c.role() == null ? 9 : c.role().ordinal()))
                .toList())
        .build();
  }
}
