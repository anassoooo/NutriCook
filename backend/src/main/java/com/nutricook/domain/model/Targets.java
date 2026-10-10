package com.nutricook.domain.model;

/**
 * Daily targets for one user (BR-NRG, BR-MAC). {@code bmr}, {@code tdee} and {@code floorApplied}
 * are exposed so the UI can explain the numbers.
 */
public record Targets(
    int kcal,
    int proteinG,
    int carbsG,
    int fatG,
    int fiberG,
    double bmr,
    double tdee,
    boolean floorApplied) {}
