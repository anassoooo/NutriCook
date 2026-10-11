package com.nutricook.controller;

import com.nutricook.dto.request.ChatRequest;
import com.nutricook.dto.request.MealCompletionRequest;
import com.nutricook.dto.request.SwapRequest;
import com.nutricook.dto.response.AchievementResponse;
import com.nutricook.dto.response.AlternativesResponse;
import com.nutricook.dto.response.ChatResponse;
import com.nutricook.dto.response.DietPlanResponse;
import com.nutricook.dto.response.MealCompletionResponse;
import com.nutricook.dto.response.ProgressResponse;
import com.nutricook.service.AchievementService;
import com.nutricook.service.ChatService;
import com.nutricook.service.DietPlanService;
import com.nutricook.service.PlanGenerationService;
import com.nutricook.service.ProgressService;
import com.nutricook.service.SwapService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/diet-plans")
@RequiredArgsConstructor
@PreAuthorize("@security.isOwner(authentication, #userId)")
public class DietController {

  private final DietPlanService dietPlanService;
  private final PlanGenerationService generationService;
  private final AchievementService achievementService;
  private final ChatService chatService;
  private final SwapService swapService;
  private final ProgressService progressService;

  /**
   * Generates (or regenerates) the plan for a day. Without {@code date}, the user's today. Returns
   * 201 immediately; the AI text follows asynchronously ({@code aiStatus}).
   */
  @PostMapping("/generate")
  public ResponseEntity<DietPlanResponse> generate(
      @PathVariable Long userId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    DietPlanResponse response =
        DietPlanResponse.of(
            generationService.generate(userId, date), dietPlanService.disclaimerFor(userId));
    achievementService.checkAndAward(userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<List<DietPlanResponse>> list(@PathVariable Long userId) {
    String disclaimer = dietPlanService.disclaimerFor(userId);
    return ResponseEntity.ok(
        dietPlanService.listForUser(userId).stream()
            .map(p -> DietPlanResponse.of(p, disclaimer))
            .toList());
  }

  @GetMapping("/{planId}")
  public ResponseEntity<DietPlanResponse> get(
      @PathVariable Long userId, @PathVariable Long planId) {
    return ResponseEntity.ok(
        DietPlanResponse.of(
            dietPlanService.getByIdForUser(userId, planId), dietPlanService.disclaimerFor(userId)));
  }

  @PostMapping("/{planId}/chat")
  public ResponseEntity<ChatResponse> chat(
      @PathVariable Long userId, @PathVariable Long planId, @Valid @RequestBody ChatRequest req) {
    return ResponseEntity.ok(chatService.chat(userId, planId, req.getMessage()));
  }

  /** Manual swap: the same rules as an AI swap (BR-AI-03, BR-LIF-04, BR-SAF-05). */
  @PostMapping("/{planId}/meals/{mealId}/swap")
  public ResponseEntity<DietPlanResponse> swap(
      @PathVariable Long userId,
      @PathVariable Long planId,
      @PathVariable Long mealId,
      @Valid @RequestBody SwapRequest req) {
    return ResponseEntity.ok(
        DietPlanResponse.of(
            swapService.swap(userId, planId, mealId, req.foodId()),
            dietPlanService.disclaimerFor(userId)));
  }

  /** Eligible replacements for each food in the meal, closest in energy first. */
  @GetMapping("/{planId}/meals/{mealId}/alternatives")
  public ResponseEntity<List<AlternativesResponse>> alternatives(
      @PathVariable Long userId, @PathVariable Long planId, @PathVariable Long mealId) {
    return ResponseEntity.ok(swapService.alternatives(userId, planId, mealId));
  }

  /**
   * Checks a meal off (or unchecks it) and updates the day's progress (BR-PRG-05). Returns the
   * plan, the day's entry and any achievement this earned.
   */
  @PutMapping("/{planId}/meals/{mealId}/completion")
  public ResponseEntity<MealCompletionResponse> completion(
      @PathVariable Long userId,
      @PathVariable Long planId,
      @PathVariable Long mealId,
      @Valid @RequestBody MealCompletionRequest req) {
    ProgressService.MealCompletion result =
        progressService.setMealCompleted(userId, planId, mealId, req.completed());
    List<AchievementResponse> earned =
        achievementService.checkAndAward(userId).stream().map(AchievementResponse::of).toList();
    return ResponseEntity.ok(
        new MealCompletionResponse(
            DietPlanResponse.of(result.plan(), dietPlanService.disclaimerFor(userId)),
            ProgressResponse.of(result.progress()),
            earned));
  }

  @PatchMapping("/{planId}/archive")
  public ResponseEntity<Void> archive(@PathVariable Long userId, @PathVariable Long planId) {
    dietPlanService.archive(userId, planId);
    return ResponseEntity.noContent().build();
  }
}
