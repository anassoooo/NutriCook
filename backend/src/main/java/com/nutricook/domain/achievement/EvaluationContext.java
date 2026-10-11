package com.nutricook.domain.achievement;

import com.nutricook.domain.model.Goal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Everything evaluators may look at, loaded once per evaluation (BR-ACH-07).
 *
 * @param today the user's today (BR-TIM-01)
 * @param days logged days of the last {@link #HISTORY_DAYS} days, in any order
 * @param plannedDays distinct days that have a plan (BR-ACH-02)
 * @param firstWeightKg the first weight ever logged, or {@code null}
 * @param goal the profile goal, or {@code null}
 */
public record EvaluationContext(
    LocalDate today, List<DayRecord> days, long plannedDays, Double firstWeightKg, Goal goal) {

  /** How far back the history goes; longer streaks are capped here. */
  public static final int HISTORY_DAYS = 365;

  /** The rolling window of the calorie and hydration achievements (BR-ACH-03/04). */
  public static final int ROLLING_DAYS = 30;

  public EvaluationContext {
    days = List.copyOf(days);
  }

  /** Days by date. */
  public Map<LocalDate, DayRecord> byDate() {
    Map<LocalDate, DayRecord> map = new TreeMap<>();
    days.forEach(d -> map.put(d.date(), d));
    return map;
  }

  /** Days in {@code [today − 29, today]}. */
  public List<DayRecord> rollingWindow() {
    LocalDate from = today.minusDays(ROLLING_DAYS - 1L);
    return days.stream().filter(d -> !d.date().isBefore(from) && !d.date().isAfter(today)).toList();
  }

  /** The most recent weight in the history, or {@code null}. */
  public Double latestWeightKg() {
    return days.stream()
        .filter(d -> d.weightKg() != null && !d.date().isAfter(today))
        .max(Comparator.comparing(DayRecord::date))
        .map(DayRecord::weightKg)
        .orElse(null);
  }
}
