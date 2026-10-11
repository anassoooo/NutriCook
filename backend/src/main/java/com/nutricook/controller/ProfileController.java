package com.nutricook.controller;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.model.Targets;
import com.nutricook.dto.request.ProfileRequest;
import com.nutricook.dto.response.ProfileResponse;
import com.nutricook.dto.response.TargetsResponse;
import com.nutricook.entity.UserProfile;
import com.nutricook.service.TargetService;
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
  private final TargetService targetService;

  /** Embeds targets, or the reason they cannot be computed yet (BR-PRF-08). */
  private ProfileResponse view(UserProfile profile) {
    try {
      Targets targets = targetService.forProfile(profile);
      return ProfileResponse.of(
          profile, TargetsResponse.of(targets, targetService.perSlot(targets)), null);
    } catch (DomainException e) {
      return ProfileResponse.of(
          profile,
          null,
          new ProfileResponse.TargetsError(e.code().name(), e.getMessage(), e.details()));
    }
  }

  @GetMapping
  public ResponseEntity<ProfileResponse> get(@PathVariable Long userId) {
    return ResponseEntity.ok(view(profileService.getByUserId(userId)));
  }

  /** Partial update: omitted (null) fields keep their value (BR-PRF-05). */
  @PutMapping
  public ResponseEntity<ProfileResponse> update(
      @PathVariable Long userId, @Valid @RequestBody ProfileRequest request) {
    return ResponseEntity.ok(view(profileService.update(userId, request)));
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
