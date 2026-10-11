package com.nutricook.dto.request;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodState;
import com.nutricook.domain.model.Slot;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * A catalog food as an admin enters it (BR-CAT-01). Values are per 100 g in the declared {@code
 * state} (BR-CAT-03). Ranges here only reject absurd input; {@code FoodQualityGate} decides whether
 * the values are consistent (BR-CAT-02). Submitting the form verifies the allergen list: an empty
 * list means the food contains none of the 14 (BR-SAF-03).
 */
public record FoodRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 50) String category,
    @NotNull FoodRole role,
    @NotNull FoodState state,
    @NotNull @PositiveOrZero @DecimalMax("900") Double caloriesPer100g,
    @NotNull @PositiveOrZero @DecimalMax("100") Double proteinPer100g,
    @NotNull @PositiveOrZero @DecimalMax("100") Double carbsPer100g,
    @NotNull @PositiveOrZero @DecimalMax("100") Double fatPer100g,
    @NotNull @PositiveOrZero @DecimalMax("100") Double sugarPer100g,
    @NotNull @PositiveOrZero @DecimalMax("100") Double fiberPer100g,
    @NotNull @PositiveOrZero @DecimalMax("40000") Double sodiumMgPer100g,
    @NotNull @Positive @DecimalMax("2000") Double portionMinG,
    @NotNull @Positive @DecimalMax("2000") Double portionMaxG,
    @NotNull @Positive @DecimalMax("500") Double portionStepG,
    @NotNull @Positive @DecimalMax("2000") Double defaultPortionG,
    @NotNull Set<Allergen> allergens,
    @NotNull Set<DietFlag> dietFlags,
    @NotEmpty Set<Slot> suitableSlots,
    boolean meat,
    @NotBlank @Size(max = 100) String source) {}
