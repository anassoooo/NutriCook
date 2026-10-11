package com.nutricook.domain.achievement;

import com.nutricook.domain.model.AchievementType;
import java.time.LocalDate;
import java.util.Map;

/**
 * BR-ACH-01: consecutive days with an entry, ending today. Until today is logged the streak ends
 * yesterday, so it does not drop to 0 every morning.
 */
public final class LoggingStreakEvaluator implements AchievementEvaluator {

  @Override
  public AchievementType type() {
    return AchievementType.LOGGING_STREAK;
  }

  @Override
  public double currentValue(EvaluationContext ctx) {
    Map<LocalDate, DayRecord> byDate = ctx.byDate();
    LocalDate day = byDate.containsKey(ctx.today()) ? ctx.today() : ctx.today().minusDays(1);
    int streak = 0;
    while (byDate.containsKey(day) && streak < EvaluationContext.HISTORY_DAYS) {
      streak++;
      day = day.minusDays(1);
    }
    return streak;
  }
}
