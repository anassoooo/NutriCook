package com.nutricook.domain.model;

/** The five daily meal slots, in eating order (BR-CMP-01/02). */
public enum Slot {
  BREAKFAST(true),
  MORNING_SNACK(false),
  LUNCH(true),
  AFTERNOON_SNACK(false),
  DINNER(true);

  private final boolean main;

  Slot(boolean main) {
    this.main = main;
  }

  /** Main meals need PROTEIN + CARB + PRODUCE; snacks have 1–2 components (BR-CMP-02). */
  public boolean isMain() {
    return main;
  }
}
