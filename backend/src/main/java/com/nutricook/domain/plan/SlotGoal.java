package com.nutricook.domain.plan;

import com.nutricook.domain.model.Slot;

/**
 * What one meal slot aims for.
 *
 * @param proteinG protein the meal is sized for; 0 for snacks, whose protein comes on top
 */
public record SlotGoal(Slot slot, double kcal, double proteinG) {}
