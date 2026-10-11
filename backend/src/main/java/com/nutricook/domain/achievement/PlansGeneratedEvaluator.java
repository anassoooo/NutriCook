package com.nutricook.domain.achievement;

import com.nutricook.domain.model.AchievementType;

/** BR-ACH-02: distinct days with a plan; regenerating a day does not count again. */
public final class PlansGeneratedEvaluator implements AchievementEvaluator {

  @Override
  public AchievementType type() {
    return AchievementType.PLANS_GENERATED;
  }

  @Override
  public double currentValue(EvaluationContext ctx) {
    return ctx.plannedDays();
  }
}
