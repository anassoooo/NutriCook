package com.nutricook.domain.achievement;

import com.nutricook.domain.model.AchievementType;

/**
 * BR-ACH-03: days of the last 30 where {@code |eaten − target| ≤ 10% × target} and something was
 * eaten. A day without a target snapshot (BR-PRG-06) cannot be judged, so it does not count.
 */
public final class CalorieGoalEvaluator implements AchievementEvaluator {

  public static final double TOLERANCE = 0.10;

  @Override
  public AchievementType type() {
    return AchievementType.CALORIE_GOAL;
  }

  @Override
  public double currentValue(EvaluationContext ctx) {
    return ctx.rollingWindow().stream().filter(CalorieGoalEvaluator::onTarget).count();
  }

  static boolean onTarget(DayRecord d) {
    Integer eaten = d.caloriesConsumed();
    Integer target = d.targetCalories();
    if (eaten == null || target == null || eaten <= 0 || target <= 0) return false;
    return Math.abs(eaten - target) <= TOLERANCE * target;
  }
}
