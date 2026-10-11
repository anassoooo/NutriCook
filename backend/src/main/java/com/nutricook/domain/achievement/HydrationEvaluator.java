package com.nutricook.domain.achievement;

import com.nutricook.domain.model.AchievementType;

/** BR-ACH-04: days of the last 30 with at least {@code hydrationDayMl} of water. */
public final class HydrationEvaluator implements AchievementEvaluator {

  private final int hydrationDayMl;

  public HydrationEvaluator(int hydrationDayMl) {
    this.hydrationDayMl = hydrationDayMl;
  }

  @Override
  public AchievementType type() {
    return AchievementType.HYDRATION;
  }

  @Override
  public double currentValue(EvaluationContext ctx) {
    return ctx.rollingWindow().stream()
        .filter(d -> d.waterMl() != null && d.waterMl() >= hydrationDayMl)
        .count();
  }
}
