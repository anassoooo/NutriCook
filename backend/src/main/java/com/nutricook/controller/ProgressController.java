package com.nutricook.controller;

import com.nutricook.dto.request.ProgressRequest;
import com.nutricook.dto.response.AchievementResponse;
import com.nutricook.dto.response.ProgressResponse;
import com.nutricook.entity.ProgressEntry;
import com.nutricook.entity.UserAchievement;
import com.nutricook.service.AchievementService;
import com.nutricook.service.ProgressService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/progress")
@RequiredArgsConstructor
@PreAuthorize("@security.isOwner(authentication, #userId)")
public class ProgressController {

  private final ProgressService progressService;
  private final AchievementService achievementService;

  @PostMapping
  public ResponseEntity<Map<String, Object>> log(
      @PathVariable Long userId, @Valid @RequestBody ProgressRequest request) {
    ProgressEntry entry = progressService.log(userId, request);
    List<UserAchievement> newAchievements = achievementService.checkAndAward(userId);
    return ResponseEntity.ok(
        Map.of(
            "progress", toResponse(entry),
            "newAchievements", newAchievements.stream().map(this::achievementResponse).toList()));
  }

  @GetMapping("/weekly")
  public ResponseEntity<List<ProgressResponse>> weekly(
      @PathVariable Long userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    return ResponseEntity.ok(
        progressService.getRange(userId, start, end).stream().map(this::toResponse).toList());
  }

  private ProgressResponse toResponse(ProgressEntry e) {
    return ProgressResponse.builder()
        .id(e.getId())
        .date(e.getDate())
        .weightKg(e.getWeightKg())
        .caloriesConsumed(e.getCaloriesConsumed())
        .waterMl(e.getWaterMl())
        .mealsCompleted(e.getMealsCompleted())
        .exerciseMinutes(e.getExerciseMinutes())
        .notes(e.getNotes())
        .build();
  }

  private AchievementResponse achievementResponse(UserAchievement ua) {
    return AchievementResponse.builder()
        .id(ua.getAchievement().getId())
        .name(ua.getAchievement().getName())
        .description(ua.getAchievement().getDescription())
        .iconName(ua.getAchievement().getIconName())
        .type(ua.getAchievement().getType().name())
        .threshold(ua.getAchievement().getThreshold())
        .earnedAt(ua.getEarnedAt())
        .build();
  }
}
