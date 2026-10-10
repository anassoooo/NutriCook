package com.nutricook.domain.rules;

/**
 * Tunables for progress logging and achievements (business-rules.md §13). Bound from {@code
 * app.tracking.*}.
 *
 * @param backfillDays how many past days a progress entry may be created/edited for (BR-PRG-02)
 * @param hydrationDayMl water needed for a day to count as a hydration day (BR-ACH-04)
 */
public record TrackingRules(int backfillDays, int hydrationDayMl) {
  public TrackingRules {
    if (backfillDays < 0 || backfillDays > 365) {
      throw new IllegalArgumentException("app.tracking.backfill-days must be in [0, 365]");
    }
    if (hydrationDayMl < 500 || hydrationDayMl > 6000) {
      throw new IllegalArgumentException("app.tracking.hydration-day-ml must be in [500, 6000]");
    }
  }
}
