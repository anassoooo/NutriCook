package com.nutricook.controller;

import com.nutricook.dto.request.ChatRequest;
import com.nutricook.dto.response.ChatResponse;
import com.nutricook.dto.response.DietPlanResponse;
import com.nutricook.service.AchievementService;
import com.nutricook.service.DietPlanService;
import com.nutricook.service.PlanGenerationService;
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
    return ResponseEntity.ok(dietPlanService.chat(userId, planId, req.getMessage()));
  }

  @PatchMapping("/{planId}/archive")
  public ResponseEntity<Void> archive(@PathVariable Long userId, @PathVariable Long planId) {
    dietPlanService.archive(userId, planId);
    return ResponseEntity.noContent().build();
  }
}
