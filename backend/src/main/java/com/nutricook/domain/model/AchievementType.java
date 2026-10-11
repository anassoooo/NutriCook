package com.nutricook.domain.model;

/** What an achievement measures. Each type has exactly one evaluator (BR-ACH-05). */
public enum AchievementType {
  /** Consecutive days with a progress entry (BR-ACH-01). */
  LOGGING_STREAK,
  /** Distinct days with a plan (BR-ACH-02). */
  PLANS_GENERATED,
  /** Days within 10% of the day's calorie target, rolling 30 days (BR-ACH-03). */
  CALORIE_GOAL,
  /** Days with enough water, rolling 30 days (BR-ACH-04). */
  HYDRATION,
  /** Kilograms changed in the goal's direction since the first weigh-in (BR-ACH-05). */
  WEIGHT_MILESTONE
}
