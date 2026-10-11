package com.nutricook.domain.achievement;

import com.nutricook.domain.model.AchievementType;
import com.nutricook.domain.rules.TrackingRules;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One evaluator per achievement type (BR-ACH-05). Construction fails when a type has no evaluator
 * or two, so a new type can never silently stay unearnable.
 */
public final class EvaluatorRegistry {

  private final Map<AchievementType, AchievementEvaluator> evaluators;

  public EvaluatorRegistry(List<AchievementEvaluator> all) {
    Map<AchievementType, AchievementEvaluator> map = new EnumMap<>(AchievementType.class);
    for (AchievementEvaluator e : all) {
      if (map.put(e.type(), e) != null) {
        throw new IllegalStateException("Two evaluators for " + e.type());
      }
    }
    for (AchievementType type : AchievementType.values()) {
      if (!map.containsKey(type)) {
        throw new IllegalStateException("No evaluator for " + type);
      }
    }
    this.evaluators = map;
  }

  public static EvaluatorRegistry standard(TrackingRules rules) {
    return new EvaluatorRegistry(
        List.of(
            new LoggingStreakEvaluator(),
            new PlansGeneratedEvaluator(),
            new CalorieGoalEvaluator(),
            new HydrationEvaluator(rules.hydrationDayMl()),
            new WeightMilestoneEvaluator()));
  }

  public double currentValue(AchievementType type, EvaluationContext ctx) {
    return evaluators.get(type).currentValue(ctx);
  }

  /** Earned when the current value reaches the threshold. */
  public boolean earned(AchievementType type, int threshold, EvaluationContext ctx) {
    return currentValue(type, ctx) >= threshold;
  }
}
