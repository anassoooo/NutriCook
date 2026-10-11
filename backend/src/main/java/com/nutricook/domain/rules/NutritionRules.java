package com.nutricook.domain.rules;

import com.nutricook.domain.model.Goal;
import com.nutricook.domain.model.Slot;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * Every tunable number behind targets and plan composition (business-rules.md §13, Constitution I).
 * Bound from {@code app.nutrition.*}; the constructor rejects values outside safe ranges, so an
 * invalid configuration stops the application at startup.
 */
public record NutritionRules(
    GoalAdjustment goalAdjustment,
    MinKcal minKcal,
    ProteinPerKg proteinPerKg,
    double proteinMaxPerKg,
    double proteinMaxEnergyShare,
    double fatEnergyShare,
    double fatMinPerKg,
    double fiberPer1000Kcal,
    Map<Slot, Double> slotShares,
    Tolerance tolerance,
    int composerMaxAttempts) {

  private static final double SUM_EPS = 1e-9;

  public NutritionRules {
    require(goalAdjustment != null, "goal-adjustment is required");
    require(minKcal != null, "min-kcal is required");
    require(proteinPerKg != null, "protein-per-kg is required");
    require(tolerance != null, "tolerance is required");
    require(
        proteinMaxPerKg >= 1.0 && proteinMaxPerKg <= 3.0, "protein-max-per-kg must be in [1, 3]");
    require(
        proteinMaxEnergyShare > 0.10 && proteinMaxEnergyShare <= 0.40,
        "protein-max-energy-share must be in (0.10, 0.40]");
    // AMDR for fat is 20-35 % of energy
    require(
        fatEnergyShare >= 0.20 && fatEnergyShare <= 0.35,
        "fat-energy-share must be in [0.20, 0.35]");
    require(fatMinPerKg > 0 && fatMinPerKg <= 1.5, "fat-min-per-kg must be in (0, 1.5]");
    require(
        fiberPer1000Kcal > 0 && fiberPer1000Kcal <= 30, "fiber-per-1000-kcal must be in (0, 30]");
    require(composerMaxAttempts >= 1, "composer-max-attempts must be >= 1");
    // BR-CMP-01: every slot has a positive share and the shares sum to exactly 1
    require(slotShares != null, "slot-shares is required");
    require(
        slotShares.keySet().equals(EnumSet.allOf(Slot.class)),
        "slot-shares must define every slot " + EnumSet.allOf(Slot.class));
    require(
        slotShares.values().stream().allMatch(v -> v != null && v > 0),
        "every slot share must be > 0");
    double sum = slotShares.values().stream().mapToDouble(Double::doubleValue).sum();
    require(Math.abs(sum - 1.0) < SUM_EPS, "slot-shares must sum to 1.0 (got " + sum + ")");
    slotShares = Map.copyOf(new EnumMap<>(slotShares));
  }

  public double share(Slot slot) {
    return slotShares.get(slot);
  }

  public double proteinPerKg(Goal goal) {
    return switch (goal) {
      case LOSE_WEIGHT -> proteinPerKg.lose();
      case GAIN_MUSCLE -> proteinPerKg.gain();
      case MAINTAIN_WEIGHT -> proteinPerKg.maintain();
      case IMPROVE_HEALTH -> proteinPerKg.improve();
    };
  }

  /** kcal added to TDEE per goal (BR-NRG-03). */
  public record GoalAdjustment(int lose, int gain) {
    public GoalAdjustment {
      require(lose <= 0 && lose >= -1000, "goal-adjustment.lose must be in [-1000, 0]");
      require(gain >= 0 && gain <= 1000, "goal-adjustment.gain must be in [0, 1000]");
    }
  }

  /** Sex-specific minimum intake (BR-NRG-04). */
  public record MinKcal(int male, int other) {
    public MinKcal {
      require(male >= 1000 && other >= 1000, "min-kcal values must be >= 1000");
    }
  }

  /** Protein grams per kg body weight by goal (BR-MAC-02). */
  public record ProteinPerKg(double lose, double gain, double maintain, double improve) {
    public ProteinPerKg {
      for (double v : new double[] {lose, gain, maintain, improve}) {
        require(v >= 0.8 && v <= 2.2, "protein-per-kg values must be in [0.8, 2.2]");
      }
    }
  }

  /** Accuracy tolerances for generated plans (BR-CMP-05). */
  public record Tolerance(double dayOk, double dayMax, double meal) {
    public Tolerance {
      require(dayOk > 0 && dayOk <= dayMax && dayMax < 0.5, "require 0 < day-ok <= day-max < 0.5");
      require(meal > 0 && meal < 0.5, "tolerance.meal must be in (0, 0.5)");
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException("app.nutrition: " + message);
  }
}
