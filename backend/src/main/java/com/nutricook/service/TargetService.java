package com.nutricook.service;

import com.nutricook.domain.energy.TargetCalculator;
import com.nutricook.domain.model.Slot;
import com.nutricook.domain.model.SlotTarget;
import com.nutricook.domain.model.Targets;
import com.nutricook.entity.UserProfile;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A user's daily targets, the single source every screen and the plan generator use (BR-PRF-08).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TargetService {

  private final UserProfileService profileService;
  private final TargetCalculator calculator;
  private final UserClock userClock;

  /**
   * @throws com.nutricook.domain.DomainException PROFILE_INCOMPLETE or PROFILE_INELIGIBLE
   */
  public Targets forUser(Long userId) {
    return forProfile(profileService.getByUserId(userId));
  }

  public Targets forProfile(UserProfile profile) {
    return calculator.compute(
        ProfileMapper.toNutritionProfile(profile), userClock.today(profile.getTimezone()));
  }

  public Map<Slot, SlotTarget> perSlot(Targets targets) {
    return calculator.perSlot(targets);
  }
}
