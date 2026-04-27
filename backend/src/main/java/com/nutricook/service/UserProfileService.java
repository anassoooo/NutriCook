package com.nutricook.service;

import com.nutricook.dto.request.ProfileRequest;
import com.nutricook.entity.DietaryRestriction;
import com.nutricook.entity.HealthCondition;
import com.nutricook.entity.UserProfile;
import com.nutricook.entity.enums.ActivityLevel;
import com.nutricook.entity.enums.Gender;
import com.nutricook.entity.enums.HealthGoal;
import com.nutricook.exception.ResourceNotFoundException;
import com.nutricook.repository.DietaryRestrictionRepository;
import com.nutricook.repository.HealthConditionRepository;
import com.nutricook.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserProfileRepository profileRepository;
  private final HealthConditionRepository conditionRepository;
  private final DietaryRestrictionRepository restrictionRepository;

  public UserProfile getByUserId(Long userId) {
    return profileRepository
        .findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));
  }

  @Transactional
  public UserProfile update(Long userId, ProfileRequest req) {
    UserProfile profile = getByUserId(userId);
    if (req.getFirstName() != null) profile.setFirstName(req.getFirstName());
    if (req.getLastName() != null) profile.setLastName(req.getLastName());
    if (req.getDateOfBirth() != null) profile.setDateOfBirth(req.getDateOfBirth());
    if (req.getGender() != null) profile.setGender(Gender.valueOf(req.getGender().toUpperCase()));
    if (req.getWeightKg() != null) profile.setWeightKg(req.getWeightKg());
    if (req.getHeightCm() != null) profile.setHeightCm(req.getHeightCm());
    if (req.getActivityLevel() != null)
      profile.setActivityLevel(ActivityLevel.valueOf(req.getActivityLevel().toUpperCase()));
    if (req.getHealthGoal() != null)
      profile.setHealthGoal(HealthGoal.valueOf(req.getHealthGoal().toUpperCase()));
    return profileRepository.save(profile);
  }

  @Transactional
  public void addHealthCondition(Long userId, Long conditionId) {
    UserProfile profile = getByUserId(userId);
    HealthCondition condition =
        conditionRepository
            .findById(conditionId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Condition not found: " + conditionId));
    profile.getHealthConditions().add(condition);
    profileRepository.save(profile);
  }

  @Transactional
  public void removeHealthCondition(Long userId, Long conditionId) {
    UserProfile profile = getByUserId(userId);
    profile.getHealthConditions().removeIf(c -> c.getId().equals(conditionId));
    profileRepository.save(profile);
  }

  @Transactional
  public void addDietaryRestriction(Long userId, Long restrictionId) {
    UserProfile profile = getByUserId(userId);
    DietaryRestriction restriction =
        restrictionRepository
            .findById(restrictionId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Restriction not found: " + restrictionId));
    profile.getDietaryRestrictions().add(restriction);
    profileRepository.save(profile);
  }

  @Transactional
  public void removeDietaryRestriction(Long userId, Long restrictionId) {
    UserProfile profile = getByUserId(userId);
    profile.getDietaryRestrictions().removeIf(r -> r.getId().equals(restrictionId));
    profileRepository.save(profile);
  }
}
