package com.nutricook.service;

import com.nutricook.domain.model.ActivityLevel;
import com.nutricook.domain.model.Goal;
import com.nutricook.domain.model.NutritionProfile;
import com.nutricook.domain.model.Sex;
import com.nutricook.entity.UserProfile;
import com.nutricook.entity.enums.Gender;

/** Maps the persisted profile to the domain's view of it (plan D1). */
final class ProfileMapper {

  private ProfileMapper() {}

  static NutritionProfile toNutritionProfile(UserProfile p) {
    return new NutritionProfile(
        p.getDateOfBirth(),
        toSex(p.getGender()),
        p.getWeightKg(),
        p.getHeightCm(),
        p.getActivityLevel() == null ? null : ActivityLevel.valueOf(p.getActivityLevel().name()),
        p.getHealthGoal() == null ? null : Goal.valueOf(p.getHealthGoal().name()));
  }

  /** OTHER and PREFER_NOT_TO_SAY use the midpoint equation (BR-NRG-01). */
  static Sex toSex(Gender gender) {
    if (gender == null) return null;
    return switch (gender) {
      case MALE -> Sex.MALE;
      case FEMALE -> Sex.FEMALE;
      case OTHER, PREFER_NOT_TO_SAY -> Sex.UNSPECIFIED;
    };
  }
}
