package com.nutricook.dto.response;

import com.nutricook.domain.model.Warning;
import com.nutricook.entity.DietPlan;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
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
  private Double totalFiberG;
  private Double totalSodiumMg;
  private LocalDate validForDate;
  private LocalDateTime generatedAt;
  private String status;

  /** 1 for the day's first plan, 2 after one regeneration, ... (BR-CMP-07). */
  private Integer generationSeq;

  private String aiExplanation;

  /** PENDING while the AI text is being written, then READY or UNAVAILABLE (BR-LIF-07). */
  private String aiStatus;

  /** Non-blocking issues such as TARGET_DEVIATION or LOW_PROTEIN (BR-CMP-05). */
  private List<Warning> warnings;

  /** "NOT_MEDICAL_ADVICE" when the user has a health condition (BR-SAF-08). */
  private String disclaimer;

  private List<MealResponse> meals;

  /** Requires the plan's meals and their components to be loaded. */
  public static DietPlanResponse of(DietPlan plan, String disclaimer) {
    return DietPlanResponse.builder()
        .id(plan.getId())
        .title(plan.getTitle())
        .totalCalories(plan.getTotalCalories())
        .totalProteinG(plan.getTotalProteinG())
        .totalCarbsG(plan.getTotalCarbsG())
        .totalFatG(plan.getTotalFatG())
        .totalSugarG(plan.getTotalSugarG())
        .totalFiberG(plan.getTotalFiberG())
        .totalSodiumMg(plan.getTotalSodiumMg())
        .validForDate(plan.getValidForDate())
        .generatedAt(plan.getGeneratedAt())
        .status(plan.getStatus().name())
        .generationSeq(plan.getGenerationSeq())
        .aiExplanation(plan.getAiExplanation())
        .aiStatus(plan.getAiStatus() == null ? null : plan.getAiStatus().name())
        .warnings(List.copyOf(plan.getWarnings()))
        .disclaimer(disclaimer)
        .meals(
            plan.getMeals().stream()
                .sorted(Comparator.comparing(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
                .map(MealResponse::of)
                .toList())
        .build();
  }
}
