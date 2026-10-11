package com.nutricook.controller;

import com.nutricook.dto.response.AchievementResponse;
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

  @GetMapping("/api/achievements")
  public ResponseEntity<List<AchievementResponse>> all() {
    return ResponseEntity.ok(
        achievementService.allAchievements().stream().map(AchievementResponse::of).toList());
  }

  /**
   * Every achievement with the user's progress: {@code currentValue}, {@code threshold} and {@code
   * earned} (BR-ACH-01..05).
   */
  @GetMapping("/api/users/{userId}/achievements")
  @PreAuthorize("@security.isOwner(authentication, #userId)")
  public ResponseEntity<List<AchievementResponse>> userAchievements(@PathVariable Long userId) {
    return ResponseEntity.ok(
        achievementService.standingsFor(userId).stream().map(AchievementResponse::of).toList());
  }
}
