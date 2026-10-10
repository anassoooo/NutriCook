package com.nutricook.service;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.PlanDraft;
import com.nutricook.domain.model.Slot;
import com.nutricook.domain.safety.PlanSafetyValidator;
import com.nutricook.domain.safety.SafetyConstraints;
import com.nutricook.domain.safety.Violation;
import com.nutricook.dto.response.ChatResponse;
import com.nutricook.dto.response.DietPlanResponse;
import com.nutricook.entity.*;
import com.nutricook.entity.enums.MealType;
import com.nutricook.exception.ResourceNotFoundException;
import com.nutricook.repository.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plan reads and the chat assistant. Generation lives in {@link PlanGenerationService}; every write
 * goes through {@link PlanCommandService}. The chat swap is replaced by role-based swaps in US4.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DietPlanService {

  public static final String DISCLAIMER = "NOT_MEDICAL_ADVICE";

  private final DietPlanRepository planRepository;
  private final UserProfileService profileService;
  private final SafetyService safetyService;
  private final PlanCommandService commands;
  private final GroqAiService groqService;
  private final ProgressRepository progressRepository;

  @Transactional(readOnly = true)
  public DietPlan getByIdForUser(Long userId, Long planId) {
    DietPlan plan =
        planRepository
            .findByIdWithMeals(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
    if (!plan.getUser().getId().equals(userId)) {
      throw new DomainException(ErrorCode.FORBIDDEN, "This plan belongs to another user.");
    }
    return loaded(plan);
  }

  @Transactional(readOnly = true)
  public List<DietPlan> listForUser(Long userId) {
    List<DietPlan> plans = planRepository.findByUserIdOrderByGeneratedAtDesc(userId);
    plans.forEach(DietPlanService::loaded);
    return plans;
  }

  /** Meals and their components, so responses can be built after the transaction. */
  private static DietPlan loaded(DietPlan plan) {
    plan.getMeals().forEach(m -> Hibernate.initialize(m.getMealFoodItems()));
    return plan;
  }

  /** BR-SAF-08: health-condition users always see the disclaimer. */
  public String disclaimerFor(Long userId) {
    return profileService.getByUserId(userId).getHealthConditions().isEmpty() ? null : DISCLAIMER;
  }

  @Transactional
  public void archive(Long userId, Long planId) {
    commands.archive(getByIdForUser(userId, planId));
  }

  @Transactional
  public ChatResponse chat(Long userId, Long planId, String message) {
    DietPlan plan = getByIdForUser(userId, planId);
    UserProfile profile = profileService.getByUserId(userId);
    SafetyConstraints constraints = safetyService.constraintsFor(profile);
    List<FoodItem> eligible = safetyService.eligibleFoods(constraints);

    String progressContext =
        progressRepository
            .findByUserIdAndDate(userId, LocalDate.now())
            .map(
                p ->
                    String.format(
                        "Today's logged progress: %d kcal consumed, %d ml water, %d meals completed, %d min exercise.",
                        p.getCaloriesConsumed() != null ? p.getCaloriesConsumed() : 0,
                        p.getWaterMl() != null ? p.getWaterMl() : 0,
                        p.getMealsCompleted() != null ? p.getMealsCompleted() : 0,
                        p.getExerciseMinutes() != null ? p.getExerciseMinutes() : 0))
            .orElse("No progress logged for today yet.");

    // BR-SAF-07: the model only ever sees foods this user may eat
    List<String> candidates = eligible.stream().map(FoodItem::getName).toList();
    GroqAiService.ChatResult result =
        groqService.chat(plan, profile, message, progressContext, candidates);

    String reply = result.reply();
    DietPlanResponse updatedPlan = null;
    if (result.swapMealType() != null && result.swapFoodName() != null) {
      Optional<String> rejection =
          applySwap(plan, result.swapMealType(), result.swapFoodName(), eligible, constraints);
      if (rejection.isPresent()) {
        reply = reply + "\n\n(I couldn't make that swap: " + rejection.get() + ")";
      } else {
        updatedPlan = DietPlanResponse.of(plan, disclaimerFor(userId));
      }
    }
    return ChatResponse.builder().reply(reply).updatedPlan(updatedPlan).build();
  }

  /**
   * Replaces a meal with one eligible food at a legal portion. Validated as a whole plan before any
   * entity changes, and saved through the safety gate (BR-SAF-05).
   *
   * @return the reason the swap was refused, or empty when it was applied
   */
  private Optional<String> applySwap(
      DietPlan plan,
      String mealTypeName,
      String foodName,
      List<FoodItem> eligible,
      SafetyConstraints constraints) {
    MealType mealType;
    try {
      mealType = MealType.valueOf(mealTypeName.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return Optional.of("I didn't understand which meal to change.");
    }
    Meal meal =
        plan.getMeals().stream().filter(m -> m.getMealType() == mealType).findFirst().orElse(null);
    if (meal == null) return Optional.of("that meal is not in your plan.");

    String wanted = foodName.trim().toLowerCase(Locale.ROOT);
    FoodItem food =
        eligible.stream()
            .filter(f -> f.getName().equalsIgnoreCase(wanted))
            .findFirst()
            .or(
                () ->
                    eligible.stream()
                        .filter(f -> f.getName().toLowerCase(Locale.ROOT).contains(wanted))
                        .findFirst())
            .orElse(null);
    if (food == null) return Optional.of(foodName + " isn't available with your restrictions.");

    FoodView view = FoodMapper.toView(food);
    double targetKcal = meal.getCalories() != null ? meal.getCalories() : 400;
    double grams = view.portion().normalize(targetKcal / (view.per100g().kcal() / 100));
    Component component = new Component(view, grams);
    MealDraft swapped = new MealDraft(Slot.valueOf(mealType.name()), List.of(component));

    List<MealDraft> draftMeals = new ArrayList<>();
    for (Meal m : plan.getMeals()) draftMeals.add(m == meal ? swapped : FoodMapper.toDraft(m));
    PlanDraft draftPlan = new PlanDraft(plan.getValidForDate(), draftMeals, List.of());
    List<Violation> violations = PlanSafetyValidator.validate(draftPlan, constraints);
    if (!violations.isEmpty()) return Optional.of(violations.get(0).message());

    MealFoodItem item = PlanGenerationService.snapshot(component, food);
    item.setMeal(meal);
    meal.getMealFoodItems().clear();
    meal.getMealFoodItems().add(item);
    applyNutrients(meal, swapped.nutrients());
    applyTotals(plan, draftPlan.totals());
    commands.persist(plan, constraints);
    return Optional.empty();
  }

  private static void applyNutrients(Meal meal, Nutrients n) {
    meal.setCalories((int) n.kcal());
    meal.setProteinG(n.proteinG());
    meal.setCarbsG(n.carbsG());
    meal.setFatG(n.fatG());
    meal.setSugarG(n.sugarG());
    meal.setFiberG(n.fiberG());
    meal.setSodiumMg(n.sodiumMg());
  }

  private static void applyTotals(DietPlan plan, Nutrients n) {
    plan.setTotalCalories((int) n.kcal());
    plan.setTotalProteinG(n.proteinG());
    plan.setTotalCarbsG(n.carbsG());
    plan.setTotalFatG(n.fatG());
    plan.setTotalSugarG(n.sugarG());
    plan.setTotalFiberG(n.fiberG());
    plan.setTotalSodiumMg(n.sodiumMg());
  }
}
