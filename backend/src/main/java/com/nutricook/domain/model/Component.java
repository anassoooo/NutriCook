package com.nutricook.domain.model;

import java.util.Objects;

/** One food and its portion inside a meal. */
public record Component(FoodView food, double grams) {

  public Component {
    Objects.requireNonNull(food, "food");
    if (!(grams > 0) || !Double.isFinite(grams)) {
      throw new IllegalArgumentException("grams must be > 0: " + grams);
    }
  }

  public FoodRole role() {
    return food.role();
  }

  /** Unrounded nutrients of this portion (BR-CMP-09). */
  public Nutrients nutrients() {
    return food.nutrientsFor(grams);
  }
}
