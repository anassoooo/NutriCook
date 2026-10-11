package com.nutricook.domain.achievement;

import com.nutricook.domain.model.AchievementType;

/** Measures one achievement type. An achievement is earned when the value reaches its threshold. */
public interface AchievementEvaluator {

  AchievementType type();

  /** The user's current value, in the threshold's unit (days, plans, kg). Never negative. */
  double currentValue(EvaluationContext ctx);
}
