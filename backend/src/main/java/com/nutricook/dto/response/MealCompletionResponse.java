package com.nutricook.dto.response;

import java.util.List;

/** The result of checking a meal off: the plan, the day's progress and new achievements. */
public record MealCompletionResponse(
    DietPlanResponse plan, ProgressResponse progress, List<AchievementResponse> newAchievements) {}
