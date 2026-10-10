package com.nutricook.domain.fixtures;

import com.nutricook.domain.model.Slot;
import com.nutricook.domain.rules.NutritionRules;
import com.nutricook.domain.rules.TrackingRules;
import java.util.EnumMap;
import java.util.Map;

/**
 * The catalogue defaults (business-rules.md §13) for pure domain tests. DomainConfigTest asserts
 * these equal what application.yml binds, so domain tests always exercise the real configuration.
 */
public final class TestRules {

  private TestRules() {}

  public static NutritionRules nutrition() {
    Map<Slot, Double> shares = new EnumMap<>(Slot.class);
    shares.put(Slot.BREAKFAST, 0.25);
    shares.put(Slot.MORNING_SNACK, 0.10);
    shares.put(Slot.LUNCH, 0.30);
    shares.put(Slot.AFTERNOON_SNACK, 0.10);
    shares.put(Slot.DINNER, 0.25);
    return new NutritionRules(
        new NutritionRules.GoalAdjustment(-500, 300),
        new NutritionRules.MinKcal(1500, 1200),
        new NutritionRules.ProteinPerKg(1.8, 2.0, 1.2, 1.0),
        2.2,
        0.35,
        0.30,
        0.6,
        14,
        shares,
        new NutritionRules.Tolerance(0.05, 0.10, 0.15),
        50);
  }

  public static TrackingRules tracking() {
    return new TrackingRules(30, 2000);
  }
}
