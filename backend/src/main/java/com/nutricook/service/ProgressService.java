package com.nutricook.service;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.model.PlanStatus;
import com.nutricook.domain.progress.DayLog;
import com.nutricook.domain.progress.LogUpdate;
import com.nutricook.domain.progress.PlanDay;
import com.nutricook.domain.progress.ProgressRules;
import com.nutricook.domain.rules.TrackingRules;
import com.nutricook.dto.request.ProgressRequest;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.Meal;
import com.nutricook.entity.ProgressEntry;
import com.nutricook.entity.UserProfile;
import com.nutricook.exception.ResourceNotFoundException;
import com.nutricook.repository.DietPlanRepository;
import com.nutricook.repository.ProgressRepository;
import com.nutricook.repository.UserProfileRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Progress tracking (business-rules.md §8). Dates are the user's own days (BR-TIM-01); the rules
 * live in {@link ProgressRules}. A day's entry follows its ACTIVE plan: checked meals give the
 * calories and the plan gives the target (BR-PRG-05/06).
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

  /** The longest range one read may ask for. */
  static final int MAX_RANGE_DAYS = 366;

  private static final Set<ErrorCode> NO_TARGET =
      Set.of(ErrorCode.PROFILE_INCOMPLETE, ErrorCode.PROFILE_INELIGIBLE);

  private final ProgressRepository entries;
  private final DietPlanRepository plans;
  private final DietPlanService planService;
  private final PlanCommandService commands;
  private final UserProfileService profiles;
  private final UserProfileRepository profileRepository;
  private final TargetService targets;
  private final TrackingRules rules;
  private final UserClock userClock;

  /** What a meal check-off changed: the plan, and the day's entry. */
  public record MealCompletion(DietPlan plan, ProgressEntry progress) {}

  /**
   * Logs a day, merging with what is already there (BR-PRG-01..04).
   *
   * @throws DomainException VALIDATION_FAILED for a date outside the window or values out of range
   */
  @Transactional
  public ProgressEntry log(Long userId, ProgressRequest req) {
    UserProfile profile = profiles.getByUserId(userId);
    LocalDate date = req.getDate();
    ProgressRules.assertLoggable(date, today(profile), rules.backfillDays());
    LogUpdate update = req.toUpdate();
    ProgressRules.assertWithinBounds(update);

    ProgressEntry entry = findOrCreate(profile, date);
    ProgressEntry saved =
        save(entry, derive(profile, date, ProgressRules.merge(toLog(entry), update)));
    if (update.weightKg() != null) updateProfileWeight(profile, date, update.weightKg());
    return saved;
  }

  /**
   * Checks a meal off or unchecks it, then refreshes the day's entry, creating it if needed
   * (BR-PRG-05). Only days inside the logging window can be checked off (BR-PRG-02).
   *
   * @throws DomainException PLAN_READ_ONLY for a plan that is not ACTIVE, VALIDATION_FAILED for a
   *     day outside the window, NOT_FOUND, FORBIDDEN
   */
  @Transactional
  public MealCompletion setMealCompleted(Long userId, Long planId, Long mealId, boolean completed) {
    DietPlan plan = planService.getByIdForUser(userId, planId);
    UserProfile profile = profiles.getByUserId(userId);
    LocalDate date = plan.getValidForDate();
    ProgressRules.assertLoggable(date, today(profile), rules.backfillDays());
    Meal meal =
        plan.getMeals().stream()
            .filter(m -> m.getId().equals(mealId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Meal not found: " + mealId));

    commands.setMealCompleted(plan, meal, completed);
    ProgressEntry entry = findOrCreate(profile, date);
    return new MealCompletion(plan, save(entry, derive(profile, date, toLog(entry))));
  }

  /**
   * A plan for the day was regenerated, swapped or archived: the entry, if there is one, follows
   * the new ACTIVE plan (or the lack of one). Runs inside the plan's transaction.
   */
  @EventListener
  public void onDayPlanChanged(DayPlanChangedEvent event) {
    entries
        .findByUserIdAndDate(event.userId(), event.date())
        .ifPresent(
            entry ->
                save(
                    entry,
                    derive(profiles.getByUserId(event.userId()), event.date(), toLog(entry))));
  }

  @Transactional(readOnly = true)
  public List<ProgressEntry> getRange(Long userId, LocalDate start, LocalDate end) {
    if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          "The range must end on or after its start and span at most " + MAX_RANGE_DAYS + " days.",
          Map.of("field", "end"));
    }
    return entries.findByUserIdAndDateBetweenOrderByDateAsc(userId, start, end);
  }

  private LocalDate today(UserProfile profile) {
    return userClock.today(profile.getTimezone());
  }

  private ProgressEntry findOrCreate(UserProfile profile, LocalDate date) {
    return entries
        .findByUserIdAndDate(profile.getUser().getId(), date)
        .orElseGet(() -> ProgressEntry.builder().user(profile.getUser()).date(date).build());
  }

  /** BR-PRG-05/06: the day's ACTIVE plan decides the derived numbers. */
  private DayLog derive(UserProfile profile, LocalDate date, DayLog log) {
    PlanDay plan =
        plans
            .findByUserIdAndValidForDateAndStatus(
                profile.getUser().getId(), date, PlanStatus.ACTIVE)
            .map(ProgressService::toPlanDay)
            .orElse(null);
    Integer profileTarget =
        plan == null && log.targetCalories() == null ? profileTarget(profile) : null;
    return ProgressRules.derive(log, plan, profileTarget);
  }

  private static PlanDay toPlanDay(DietPlan plan) {
    int target =
        plan.getTargetCalories() != null ? plan.getTargetCalories() : plan.getTotalCalories();
    return new PlanDay(
        target,
        plan.getMeals().stream()
            .filter(Meal::isCompleted)
            .map(m -> m.getCalories() == null ? 0 : m.getCalories())
            .toList());
  }

  /** The profile's target today, or {@code null} while the profile cannot produce one. */
  private Integer profileTarget(UserProfile profile) {
    try {
      return targets.forProfile(profile).kcal();
    } catch (DomainException e) {
      if (NO_TARGET.contains(e.code())) return null;
      throw e;
    }
  }

  /** BR-PRG-04: the latest weigh-in becomes the profile weight, so future targets follow it. */
  private void updateProfileWeight(UserProfile profile, LocalDate date, double weightKg) {
    LocalDate latestOther = entries.latestWeighInExcluding(profile.getUser().getId(), date);
    if (ProgressRules.isLatestWeighIn(date, latestOther)) {
      profile.setWeightKg(weightKg);
      profileRepository.save(profile);
    }
  }

  private static DayLog toLog(ProgressEntry e) {
    return new DayLog(
        e.getCaloriesConsumed(),
        e.isCaloriesOverridden(),
        e.getWaterMl(),
        e.getExerciseMinutes(),
        e.getWeightKg(),
        e.getNotes(),
        e.getMealsCompleted() == null ? 0 : e.getMealsCompleted(),
        e.getTargetCalories());
  }

  private ProgressEntry save(ProgressEntry e, DayLog log) {
    e.setCaloriesConsumed(log.caloriesConsumed());
    e.setCaloriesOverridden(log.caloriesOverridden());
    e.setWaterMl(log.waterMl());
    e.setExerciseMinutes(log.exerciseMinutes());
    e.setWeightKg(log.weightKg());
    e.setNotes(log.notes());
    e.setMealsCompleted(log.mealsCompleted());
    e.setTargetCalories(log.targetCalories());
    return entries.save(e);
  }
}
