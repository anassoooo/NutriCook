package com.nutricook.service;

import com.nutricook.domain.achievement.DayRecord;
import com.nutricook.domain.achievement.EvaluationContext;
import com.nutricook.domain.achievement.EvaluatorRegistry;
import com.nutricook.domain.model.Goal;
import com.nutricook.entity.Achievement;
import com.nutricook.entity.ProgressEntry;
import com.nutricook.entity.UserAchievement;
import com.nutricook.entity.UserProfile;
import com.nutricook.repository.AchievementRepository;
import com.nutricook.repository.DietPlanRepository;
import com.nutricook.repository.ProgressRepository;
import com.nutricook.repository.UserAchievementRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Achievements (business-rules.md §9). The user's history is loaded once, with a fixed number of
 * queries whatever its length (BR-ACH-07); the domain evaluators decide (BR-ACH-01..05); awards are
 * idempotent and permanent (BR-ACH-06).
 */
@Service
@RequiredArgsConstructor
public class AchievementService {

  private final AchievementRepository achievements;
  private final UserAchievementRepository awards;
  private final ProgressRepository progress;
  private final DietPlanRepository plans;
  private final UserProfileService profiles;
  private final EvaluatorRegistry registry;
  private final UserClock userClock;
  private final Clock clock;

  /** One achievement as a user sees it: how far they are and whether they earned it. */
  public record Standing(
      Achievement achievement, double currentValue, boolean earned, LocalDateTime earnedAt) {}

  public List<Achievement> allAchievements() {
    return achievements.findAll();
  }

  /** Every achievement with the user's current value, earned or not. */
  @Transactional(readOnly = true)
  public List<Standing> standingsFor(Long userId) {
    EvaluationContext ctx = context(userId);
    Map<Long, UserAchievement> earned =
        awards.findByUserId(userId).stream()
            .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), Function.identity()));
    return achievements.findAll().stream()
        .map(
            a -> {
              UserAchievement award = earned.get(a.getId());
              return new Standing(
                  a,
                  registry.currentValue(a.getType(), ctx),
                  award != null,
                  award == null ? null : award.getEarnedAt());
            })
        .toList();
  }

  /**
   * Awards what the user has reached and not earned yet. Runs after a progress log, a meal
   * check-off or a plan generation (BR-ACH-07).
   *
   * @return the achievements awarded by this call
   */
  @Transactional
  public List<UserAchievement> checkAndAward(Long userId) {
    EvaluationContext ctx = context(userId);
    Set<Long> alreadyEarned =
        awards.findByUserId(userId).stream()
            .map(ua -> ua.getAchievement().getId())
            .collect(Collectors.toSet());
    LocalDateTime now = LocalDateTime.now(clock);
    List<Long> awarded = new ArrayList<>();
    for (Achievement a : achievements.findAll()) {
      if (alreadyEarned.contains(a.getId())) continue; // permanent: never re-evaluated
      if (registry.earned(a.getType(), a.getThreshold(), ctx)
          && awards.insertIfAbsent(userId, a.getId(), now) == 1) {
        awarded.add(a.getId());
      }
    }
    return awarded.isEmpty() ? List.of() : awards.findByUserIdAndAchievementIdIn(userId, awarded);
  }

  /** The user's history: one range query, one count and one lookup (BR-ACH-07). */
  private EvaluationContext context(Long userId) {
    UserProfile profile = profiles.getByUserId(userId);
    LocalDate today = userClock.today(profile.getTimezone());
    List<DayRecord> days =
        progress
            .findByUserIdAndDateBetweenOrderByDateAsc(
                userId, today.minusDays(EvaluationContext.HISTORY_DAYS - 1L), today)
            .stream()
            .map(AchievementService::toRecord)
            .toList();
    Double firstWeight =
        progress
            .findFirstByUserIdAndWeightKgIsNotNullOrderByDateAsc(userId)
            .map(ProgressEntry::getWeightKg)
            .orElse(null);
    Goal goal =
        profile.getHealthGoal() == null ? null : Goal.valueOf(profile.getHealthGoal().name());
    return new EvaluationContext(today, days, plans.countPlannedDays(userId), firstWeight, goal);
  }

  private static DayRecord toRecord(ProgressEntry e) {
    return new DayRecord(
        e.getDate(),
        e.getCaloriesConsumed(),
        e.getTargetCalories(),
        e.getWaterMl(),
        e.getWeightKg());
  }
}
