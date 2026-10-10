package com.nutricook.controller;

import com.nutricook.dto.response.TargetsResponse;
import com.nutricook.service.TargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Daily calorie and macro targets (BR-NRG, BR-MAC, BR-PRF-08). */
@RestController
@RequiredArgsConstructor
@PreAuthorize("@security.isOwner(authentication, #userId)")
public class TargetController {

  private final TargetService targetService;

  /** 200 with targets, or 422 PROFILE_INCOMPLETE ({@code missingFields}) / PROFILE_INELIGIBLE. */
  @GetMapping("/api/users/{userId}/targets")
  public TargetsResponse get(@PathVariable Long userId) {
    var targets = targetService.forUser(userId);
    return TargetsResponse.of(targets, targetService.perSlot(targets));
  }
}
