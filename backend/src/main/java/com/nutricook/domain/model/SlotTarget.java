package com.nutricook.domain.model;

/** The share of the daily targets one meal slot aims for (BR-CMP-01). */
public record SlotTarget(
    Slot slot, double share, int kcal, int proteinG, int carbsG, int fatG, int fiberG) {}
