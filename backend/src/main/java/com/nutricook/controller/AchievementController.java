package com.nutricook.controller;

import com.nutricook.dto.response.AchievementResponse;
import com.nutricook.entity.Achievement;
import com.nutricook.entity.UserAchievement;
import com.nutricook.repository.AchievementRepository;
import com.nutricook.service.AchievementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AchievementController {

  private final AchievementService achievementService;
  private final AchievementRepository achievementRepository;

  @GetMapping("/api/achievements")
  public ResponseEntity<List<AchievementResponse>> all() {
    return ResponseEntity.ok(
        achievementRepository.findAll().stream().map(this::toResponse).toList());
  }

  @GetMapping("/api/users/{userId}/achievements")
  @PreAuthorize("@security.isOwner(authentication, #userId)")
  public ResponseEntity<List<AchievementResponse>> userAchievements(@PathVariable Long userId) {
    return ResponseEntity.ok(
        achievementService.getUserAchievements(userId).stream()
            .map(ua -> toResponseWithEarnedAt(ua))
            .toList());
  }

  private AchievementResponse toResponse(Achievement a) {
    return AchievementResponse.builder()
        .id(a.getId())
        .name(a.getName())
        .description(a.getDescription())
        .iconName(a.getIconName())
        .type(a.getType().name())
        .threshold(a.getThreshold())
        .build();
  }

  private AchievementResponse toResponseWithEarnedAt(UserAchievement ua) {
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
