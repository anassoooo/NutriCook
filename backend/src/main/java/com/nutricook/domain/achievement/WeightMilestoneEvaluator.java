package com.nutricook.domain.achievement;

import com.nutricook.domain.model.AchievementType;

/**
 * BR-ACH-05: kilograms changed since the first logged weight, in the goal's direction: down for
 * losing weight, up for gaining muscle. Other goals have no direction, so the value stays 0. The
 * value is rounded down to 0.1 kg, so a milestone is never earned by rounding.
 */
public final class WeightMilestoneEvaluator implements AchievementEvaluator {

  @Override
  public AchievementType type() {
    return AchievementType.WEIGHT_MILESTONE;
  }

  @Override
  public double currentValue(EvaluationContext ctx) {
    Double first = ctx.firstWeightKg();
    Double latest = ctx.latestWeightKg();
    if (first == null || latest == null || ctx.goal() == null) return 0;
    double change =
        switch (ctx.goal()) {
          case LOSE_WEIGHT -> first - latest;
          case GAIN_MUSCLE -> latest - first;
          case MAINTAIN_WEIGHT, IMPROVE_HEALTH -> 0;
        };
    // the epsilon absorbs binary noise such as 80.0 - 77.9 = 2.0999...
    return Math.max(0, Math.floor(change * 10 + 1e-6) / 10);
  }
}
