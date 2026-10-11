package com.nutricook.controller;

import com.nutricook.dto.request.ProgressRequest;
import com.nutricook.dto.response.AchievementResponse;
import com.nutricook.dto.response.ProgressResponse;
import com.nutricook.entity.ProgressEntry;
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
    List<AchievementResponse> newAchievements =
        achievementService.checkAndAward(userId).stream().map(AchievementResponse::of).toList();
    return ResponseEntity.ok(
        Map.of("progress", ProgressResponse.of(entry), "newAchievements", newAchievements));
  }

  @GetMapping("/weekly")
  public ResponseEntity<List<ProgressResponse>> weekly(
      @PathVariable Long userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    return ResponseEntity.ok(
        progressService.getRange(userId, start, end).stream().map(ProgressResponse::of).toList());
  }
}
