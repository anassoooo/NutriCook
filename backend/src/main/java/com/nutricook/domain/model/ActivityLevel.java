package com.nutricook.domain.model;

/** Physical activity level and its TDEE multiplier (BR-NRG-02). */
public enum ActivityLevel {
  SEDENTARY(1.2),
  LIGHTLY_ACTIVE(1.375),
  MODERATELY_ACTIVE(1.55),
  VERY_ACTIVE(1.725),
  EXTRA_ACTIVE(1.9);

  private final double factor;

  ActivityLevel(double factor) {
    this.factor = factor;
  }

  public double factor() {
    return factor;
  }
}
