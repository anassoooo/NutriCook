package com.nutricook.service;

import com.nutricook.config.LimitsProperties;
import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.PlanDraft;
import com.nutricook.domain.model.PlanStatus;
import com.nutricook.domain.model.Targets;
import com.nutricook.domain.plan.PlanComposer;
import com.nutricook.domain.plan.PlanLifecycle;
import com.nutricook.domain.plan.PlanSeed;
import com.nutricook.domain.safety.SafetyConstraints;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.FoodItem;
import com.nutricook.entity.Meal;
import com.nutricook.entity.MealFoodItem;
import com.nutricook.entity.User;
import com.nutricook.entity.UserProfile;
import com.nutricook.entity.enums.AiStatus;
import com.nutricook.entity.enums.MealType;
import com.nutricook.exception.ResourceNotFoundException;
import com.nutricook.repository.DietPlanRepository;
import com.nutricook.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Generates a user's plan for a day (US3). Order of checks: date window (BR-LIF-03), quota
 * (BR-LIF-06), targets (BR-PRF-01/02), safety (BR-SAF), composition (BR-CMP), then supersede the
 * previous plan and persist through the safety gate. AI text is added after commit (BR-LIF-07).
 */
@Service
@Slf4j
public class PlanGenerationService {

  private final DietPlanRepository plans;
  private final UserRepository users;
  private final UserProfileService profileService;
  private final TargetService targetService;
  private final SafetyService safetyService;
  private final PlanCommandService commands;
  private final PlanComposer composer;
  private final UserClock userClock;
  private final LimitsProperties limits;
  private final ApplicationEventPublisher events;
  private final TransactionTemplate tx;
  private final Timer generationTimer;

  public PlanGenerationService(
      DietPlanRepository plans,
      UserRepository users,
      UserProfileService profileService,
      TargetService targetService,
      SafetyService safetyService,
      PlanCommandService commands,
      PlanComposer composer,
      UserClock userClock,
      LimitsProperties limits,
      ApplicationEventPublisher events,
      PlatformTransactionManager txManager,
      MeterRegistry meters) {
    this.plans = plans;
    this.users = users;
    this.profileService = profileService;
    this.targetService = targetService;
    this.safetyService = safetyService;
    this.commands = commands;
    this.composer = composer;
    this.userClock = userClock;
    this.limits = limits;
    this.events = events;
    this.tx = new TransactionTemplate(txManager);
    this.generationTimer = meters.timer("nutricook.plan.generation");
  }

  /**
   * @param requestedDate null means the user's today
   */
  public DietPlan generate(Long userId, LocalDate requestedDate) {
    try {
      return tx.execute(status -> generateOnce(userId, requestedDate));
    } catch (DataIntegrityViolationException e) {
      // Two concurrent requests for the same day: the database kept one ACTIVE plan
      // (ux_active_plan).
      // Retrying supersedes the winner, so exactly one ACTIVE plan remains (BR-LIF-02, plan D5).
      log.info("Concurrent generation for user {}; retrying once", userId);
      return tx.execute(status -> generateOnce(userId, requestedDate));
    }
  }

  private DietPlan generateOnce(Long userId, LocalDate requestedDate) {
    long start = System.nanoTime();
    User user =
        users
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    UserProfile profile = profileService.getByUserId(userId);
    LocalDate today = userClock.today(profile.getTimezone());
    LocalDate date = requestedDate != null ? requestedDate : today;

    PlanLifecycle.assertGeneratable(date, today);
    enforceQuota(userId, today, profile.getTimezone());

    Targets targets = targetService.forProfile(profile);
    SafetyConstraints constraints = safetyService.constraintsFor(profile);
    List<FoodItem> foods = safetyService.eligibleFoods(constraints);
    Map<Long, FoodItem> byId =
        foods.stream().collect(Collectors.toMap(FoodItem::getId, Function.identity()));

    int seq = Math.toIntExact(plans.countByUserIdAndValidForDate(userId, date)) + 1;
    long seed = PlanSeed.of(userId, date, seq);
    PlanDraft draft =
        composer.compose(
            targets, constraints, foods.stream().map(FoodMapper::toView).toList(), date, seed);

    plans
        .findByUserIdAndValidForDateAndStatus(userId, date, PlanStatus.ACTIVE)
        .ifPresent(commands::supersede);

    DietPlan saved =
        commands.persist(toEntity(user, draft, byId, seq, seed, targets.kcal()), constraints);
    events.publishEvent(new PlanGeneratedEvent(saved.getId(), userId));
    long ms = (System.nanoTime() - start) / 1_000_000;
    generationTimer.record(ms, java.util.concurrent.TimeUnit.MILLISECONDS);
    log.info(
        "plan_generated planId={} userId={} date={} seq={} seed={} kcal={} target={} warnings={} ms={}",
        saved.getId(),
        userId,
        date,
        seq,
        seed,
        saved.getTotalCalories(),
        targets.kcal(),
        draft.warnings().stream().map(w -> w.code().name()).toList(),
        ms);
    return saved;
  }

  /** BR-LIF-06: successful generations since the start of the user's day. */
  private void enforceQuota(Long userId, LocalDate today, String timezone) {
    LocalDateTime startOfDay =
        today
            .atStartOfDay(UserClock.zoneOrUtc(timezone))
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime();
    long used = plans.countByUserIdAndGeneratedAtGreaterThanEqual(userId, startOfDay);
    if (used >= limits.generationsPerDay()) {
      throw new DomainException(
          ErrorCode.GENERATION_LIMIT,
          "You have generated "
              + limits.generationsPerDay()
              + " plans today, which is the daily limit.",
          Map.of("limit", limits.generationsPerDay()));
    }
  }

  private static DietPlan toEntity(
      User user, PlanDraft draft, Map<Long, FoodItem> foods, int seq, long seed, int targetKcal) {
    Nutrients totals = draft.totals();
    DietPlan plan =
        DietPlan.builder()
            .user(user)
            .title("Diet Plan for " + draft.date())
            .validForDate(draft.date())
            .status(PlanStatus.ACTIVE)
            .aiStatus(AiStatus.PENDING)
            .generationSeq(seq)
            .seed(seed)
            .targetCalories(targetKcal)
            .warnings(new ArrayList<>(draft.warnings()))
            .totalCalories((int) totals.kcal())
            .totalProteinG(totals.proteinG())
            .totalCarbsG(totals.carbsG())
            .totalFatG(totals.fatG())
            .totalSugarG(totals.sugarG())
            .totalFiberG(totals.fiberG())
            .totalSodiumMg(totals.sodiumMg())
            .build();
    for (MealDraft draftMeal : draft.meals()) {
      Meal meal = toMeal(draftMeal, foods);
      meal.setDietPlan(plan);
      plan.getMeals().add(meal);
    }
    return plan;
  }

  private static Meal toMeal(MealDraft draft, Map<Long, FoodItem> foods) {
    Nutrients n = draft.nutrients();
    String label = draft.slot().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    Meal meal =
        Meal.builder()
            .name(Character.toUpperCase(label.charAt(0)) + label.substring(1))
            .mealType(MealType.valueOf(draft.slot().name()))
            .sortOrder(draft.slot().ordinal() + 1)
            .calories((int) n.kcal())
            .proteinG(n.proteinG())
            .carbsG(n.carbsG())
            .fatG(n.fatG())
            .sugarG(n.sugarG())
            .fiberG(n.fiberG())
            .sodiumMg(n.sodiumMg())
            .build();
    for (Component c : draft.components()) {
      MealFoodItem item = snapshot(c, foods.get(c.food().id()));
      item.setMeal(meal);
      meal.getMealFoodItems().add(item);
    }
    return meal;
  }

  /** The component and its nutrition at generation time (BR-LIF-05). */
  static MealFoodItem snapshot(Component c, FoodItem food) {
    Nutrients n = c.nutrients();
    return MealFoodItem.builder()
        .foodItem(food)
        .quantityG(c.grams())
        .role(c.role())
        .kcal(round1(n.kcal()))
        .proteinG(round1(n.proteinG()))
        .carbsG(round1(n.carbsG()))
        .fatG(round1(n.fatG()))
        .sugarG(round1(n.sugarG()))
        .fiberG(round1(n.fiberG()))
        .sodiumMg(round1(n.sodiumMg()))
        .build();
  }

  private static double round1(double v) {
    return Math.round(v * 10) / 10.0;
  }
}
