package com.nutricook.controller;

import com.nutricook.dto.request.ProfileRequest;
import com.nutricook.entity.UserProfile;
import com.nutricook.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/profile")
@RequiredArgsConstructor
@PreAuthorize("@security.isOwner(authentication, #userId)")
public class ProfileController {

  private final UserProfileService profileService;

  @GetMapping
  public ResponseEntity<UserProfile> get(@PathVariable Long userId) {
    return ResponseEntity.ok(profileService.getByUserId(userId));
  }

  @PutMapping
  public ResponseEntity<UserProfile> update(
      @PathVariable Long userId, @Valid @RequestBody ProfileRequest request) {
    return ResponseEntity.ok(profileService.update(userId, request));
  }

  @PostMapping("/health-conditions/{conditionId}")
  public ResponseEntity<Void> addCondition(
      @PathVariable Long userId, @PathVariable Long conditionId) {
    profileService.addHealthCondition(userId, conditionId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/health-conditions/{conditionId}")
  public ResponseEntity<Void> removeCondition(
      @PathVariable Long userId, @PathVariable Long conditionId) {
    profileService.removeHealthCondition(userId, conditionId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/dietary-restrictions/{restrictionId}")
  public ResponseEntity<Void> addRestriction(
      @PathVariable Long userId, @PathVariable Long restrictionId) {
    profileService.addDietaryRestriction(userId, restrictionId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/dietary-restrictions/{restrictionId}")
  public ResponseEntity<Void> removeRestriction(
      @PathVariable Long userId, @PathVariable Long restrictionId) {
    profileService.removeDietaryRestriction(userId, restrictionId);
    return ResponseEntity.noContent().build();
  }
}
