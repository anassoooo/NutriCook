package com.nutricook.domain.model;

import java.time.LocalDate;

/**
 * The body and goal data the nutrition rules need. Any component may be {@code null}: completeness
 * is a business rule (BR-PRF-01), checked by the target calculation, not by this type.
 */
public record NutritionProfile(
    LocalDate dateOfBirth,
    Sex sex,
    Double weightKg,
    Double heightCm,
    ActivityLevel activityLevel,
    Goal goal) {}
