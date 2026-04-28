package com.nutricook.controller;

import com.nutricook.dto.request.ChatRequest;
import com.nutricook.dto.response.ChatResponse;
import com.nutricook.dto.response.DietPlanResponse;
import com.nutricook.dto.response.MealResponse;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.Meal;
import com.nutricook.service.AchievementService;
import com.nutricook.service.DietPlanService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/diet-plans")
@RequiredArgsConstructor
public class DietController {

  private final DietPlanService dietPlanService;
  private final AchievementService achievementService;

  @PostMapping("/generate")
  public ResponseEntity<DietPlanResponse> generate(
      @PathVariable Long userId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    LocalDate targetDate = date != null ? date : LocalDate.now();
    DietPlanResponse response = toResponse(dietPlanService.generate(userId, targetDate));
    achievementService.checkAndAward(userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<List<DietPlanResponse>> list(@PathVariable Long userId) {
    return ResponseEntity.ok(
        dietPlanService.listForUser(userId).stream().map(this::toResponse).toList());
  }

  @GetMapping("/{planId}")
  public ResponseEntity<DietPlanResponse> get(
      @PathVariable Long userId, @PathVariable Long planId) {
    return ResponseEntity.ok(toResponse(dietPlanService.getById(planId)));
  }

  @PostMapping("/{planId}/chat")
  public ResponseEntity<ChatResponse> chat(
      @PathVariable Long userId, @PathVariable Long planId, @RequestBody ChatRequest req) {
    return ResponseEntity.ok(dietPlanService.chat(userId, planId, req.getMessage()));
  }

  @PatchMapping("/{planId}/archive")
  public ResponseEntity<Void> archive(@PathVariable Long userId, @PathVariable Long planId) {
    dietPlanService.archive(planId);
    return ResponseEntity.noContent().build();
  }

  private DietPlanResponse toResponse(DietPlan plan) {
    return DietPlanResponse.builder()
        .id(plan.getId())
        .title(plan.getTitle())
        .totalCalories(plan.getTotalCalories())
        .totalProteinG(plan.getTotalProteinG())
        .totalCarbsG(plan.getTotalCarbsG())
        .totalFatG(plan.getTotalFatG())
        .totalSugarG(plan.getTotalSugarG())
        .validForDate(plan.getValidForDate())
        .generatedAt(plan.getGeneratedAt())
        .status(plan.getStatus().name())
        .aiExplanation(plan.getAiExplanation())
        .meals(plan.getMeals().stream().map(this::mealToResponse).toList())
        .build();
  }

  private MealResponse mealToResponse(Meal meal) {
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
        .sortOrder(meal.getSortOrder())
        .aiSuggestion(meal.getAiSuggestion())
        .build();
  }
}
