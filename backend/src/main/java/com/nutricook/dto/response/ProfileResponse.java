package com.nutricook.dto.response;

import com.nutricook.entity.DietaryRestriction;
import com.nutricook.entity.HealthCondition;
import com.nutricook.entity.UserProfile;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The profile plus its computed targets (BR-PRF-08). When targets cannot be computed, {@code
 * targets} is null and {@code targetsError} explains why (e.g. PROFILE_INCOMPLETE with the fields
 * still missing), so onboarding can guide the user.
 */
public record ProfileResponse(
    Long id,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    String gender,
    Double weightKg,
    Double heightCm,
    String activityLevel,
    String healthGoal,
    String timezone,
    LocalDateTime updatedAt,
    List<HealthCondition> healthConditions,
    List<DietaryRestriction> dietaryRestrictions,
    TargetsResponse targets,
    TargetsError targetsError,
    String disclaimer) {

  public record TargetsError(String code, String message, Map<String, Object> details) {}

  public static ProfileResponse of(UserProfile p, TargetsResponse targets, TargetsError error) {
    return new ProfileResponse(
        p.getId(),
        p.getFirstName(),
        p.getLastName(),
        p.getDateOfBirth(),
        p.getGender() == null ? null : p.getGender().name(),
        p.getWeightKg(),
        p.getHeightCm(),
        p.getActivityLevel() == null ? null : p.getActivityLevel().name(),
        p.getHealthGoal() == null ? null : p.getHealthGoal().name(),
        p.getTimezone(),
        p.getUpdatedAt(),
        p.getHealthConditions().stream()
            .sorted(Comparator.comparing(HealthCondition::getId))
            .toList(),
        p.getDietaryRestrictions().stream()
            .sorted(Comparator.comparing(DietaryRestriction::getId))
            .toList(),
        targets,
        error,
        p.getHealthConditions().isEmpty() ? null : "NOT_MEDICAL_ADVICE");
  }
}
