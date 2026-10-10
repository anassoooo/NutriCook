package com.nutricook.service;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.PlanDraft;
import com.nutricook.domain.model.Slot;
import com.nutricook.domain.safety.PlanSafetyValidator;
import com.nutricook.domain.safety.SafetyConstraints;
import com.nutricook.domain.safety.Violation;
import com.nutricook.dto.response.ChatResponse;
import com.nutricook.dto.response.DietPlanResponse;
import com.nutricook.dto.response.MealResponse;
import com.nutricook.entity.*;
import com.nutricook.entity.enums.MealType;
import com.nutricook.entity.enums.PlanStatus;
import com.nutricook.exception.ResourceNotFoundException;
import com.nutricook.repository.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plan generation, chat swaps and plan reads.
 *
 * <p>T039 safety hotfix: every food comes from {@link SafetyService#eligibleFoods} and every plan
 * is checked by {@link PlanSafetyValidator} before it is saved; there is no fallback to unsafe
 * foods (BR-SAF-04/05/07). The one-food-per-meal composition is replaced by the domain composer in
 * US3.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DietPlanService {

  public static final String DISCLAIMER = "NOT_MEDICAL_ADVICE";

  private final DietPlanRepository planRepository;
  private final UserRepository userRepository;
  private final UserProfileService profileService;
  private final NutritionService nutritionService;
  private final TargetService targetService;
  private final SafetyService safetyService;
  private final GroqAiService groqService;
  private final ProgressRepository progressRepository;

  private static final Map<MealType, Double> MEAL_CALORIE_RATIO =
      Map.of(
          MealType.BREAKFAST, 0.25,
          MealType.MORNING_SNACK, 0.10,
          MealType.LUNCH, 0.30,
          MealType.AFTERNOON_SNACK, 0.10,
          MealType.DINNER, 0.25);

  @Transactional
  public DietPlan generate(Long userId, LocalDate date) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    UserProfile profile = profileService.getByUserId(userId);

    // Single source of targets; fails with PROFILE_INCOMPLETE/INELIGIBLE (BR-PRF-01/02)
    int targetKcal = targetService.forProfile(profile).kcal();

    SafetyConstraints constraints = safetyService.constraintsFor(profile);
    List<FoodItem> foods = safetyService.eligibleFoods(constraints);
    if (foods.isEmpty()) {
      // BR-SAF-04: fail closed, never fall back to the full catalog
      throw SafetyService.infeasible(
          "No food in the catalog satisfies all of your restrictions.", null);
    }

    List<Meal> meals = buildMeals(foods, targetKcal, constraints);

    // BR-SAF-05: the whole day, including per-day limits, before anything is persisted
    List<Violation> violations =
        PlanSafetyValidator.validate(FoodMapper.toDraft(date, meals), constraints);
    if (!violations.isEmpty()) {
      throw SafetyService.infeasible(
          "We could not build a plan within your health limits.", violations.get(0));
    }

    planRepository.deleteAll(planRepository.findByUserIdAndValidForDate(userId, date));

    DietPlan plan =
        DietPlan.builder()
            .user(user)
            .title("Diet Plan for " + date)
            .validForDate(date)
            .status(PlanStatus.ACTIVE)
            .meals(meals)
            .build();
    meals.forEach(m -> m.setDietPlan(plan));

    NutritionService.NutritionTotals totals = nutritionService.computeTotals(meals);
    plan.setTotalCalories(totals.calories());
    plan.setTotalProteinG(totals.proteinG());
    plan.setTotalCarbsG(totals.carbsG());
    plan.setTotalFatG(totals.fatG());
    plan.setTotalSugarG(totals.sugarG());

    for (Meal meal : meals) {
      String suggestion = groqService.getMealSuggestion(meal, profile);
      meal.setAiSuggestion(suggestion);
    }

    String explanation = groqService.getPlanExplanation(plan, profile);
    plan.setAiExplanation(explanation);

    DietPlan saved = planRepository.save(plan);
    return planRepository.findByIdWithMeals(saved.getId()).orElse(saved);
  }

  public DietPlan getById(Long planId) {
    return planRepository
        .findByIdWithMeals(planId)
        .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
  }

  public DietPlan getByIdForUser(Long userId, Long planId) {
    DietPlan plan = getById(planId);
    if (!plan.getUser().getId().equals(userId)) {
      throw new DomainException(ErrorCode.FORBIDDEN, "This plan belongs to another user.");
    }
    return plan;
  }

  public List<DietPlan> listForUser(Long userId) {
    return planRepository.findByUserIdOrderByGeneratedAtDesc(userId);
  }

  /** BR-SAF-08: health-condition users always see the disclaimer. */
  public String disclaimerFor(Long userId) {
    return profileService.getByUserId(userId).getHealthConditions().isEmpty() ? null : DISCLAIMER;
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
    DietPlanResponse updatedPlanResponse = null;
    if (result.swapMealType() != null && result.swapFoodName() != null) {
      Optional<String> rejection =
          applySwap(plan, result.swapMealType(), result.swapFoodName(), eligible, constraints);
      if (rejection.isPresent()) {
        reply = reply + "\n\n(I couldn't make that swap: " + rejection.get() + ")";
      } else {
        DietPlan refreshed = planRepository.findByIdWithMeals(planId).orElse(plan);
        updatedPlanResponse = toResponse(refreshed);
      }
    }

    return ChatResponse.builder().reply(reply).updatedPlan(updatedPlanResponse).build();
  }

  /**
   * Replaces a meal's food with an eligible one. Validated before anything changes (BR-SAF-05).
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

    // Only foods this user may eat can be chosen, whatever the model asked for
    FoodItem food =
        eligible.stream()
            .filter(f -> f.getName().equalsIgnoreCase(foodName.trim()))
            .findFirst()
            .or(
                () ->
                    eligible.stream()
                        .filter(
                            f ->
                                f.getName()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(foodName.trim().toLowerCase(Locale.ROOT)))
                        .findFirst())
            .orElse(null);
    if (food == null) {
      return Optional.of(foodName + " isn't available with your restrictions.");
    }

    int targetCal = meal.getCalories() != null ? meal.getCalories() : 400;
    double grams =
        food.getCaloriesPer100g() > 0 ? (targetCal / food.getCaloriesPer100g()) * 100 : 100;
    grams = Math.round(grams * 10.0) / 10.0;

    // Validate the plan as it would be after the swap, before touching any entity
    MealDraft swappedMeal =
        new MealDraft(
            Slot.valueOf(mealType.name()), List.of(new Component(FoodMapper.toView(food), grams)));
    List<MealDraft> draftMeals = new ArrayList<>();
    for (Meal m : plan.getMeals()) {
      draftMeals.add(m == meal ? swappedMeal : FoodMapper.toDraft(m));
    }
    List<Violation> violations =
        PlanSafetyValidator.validate(
            new PlanDraft(plan.getValidForDate(), draftMeals, List.of()), constraints);
    if (!violations.isEmpty()) return Optional.of(violations.get(0).message());

    MealFoodItem newMfi = MealFoodItem.builder().foodItem(food).quantityG(grams).meal(meal).build();
    meal.getMealFoodItems().clear();
    meal.getMealFoodItems().add(newMfi);
    nutritionService.computeMealNutrition(meal);

    NutritionService.NutritionTotals totals = nutritionService.computeTotals(plan.getMeals());
    plan.setTotalCalories(totals.calories());
    plan.setTotalProteinG(totals.proteinG());
    plan.setTotalCarbsG(totals.carbsG());
    plan.setTotalFatG(totals.fatG());
    plan.setTotalSugarG(totals.sugarG());

    planRepository.save(plan);
    return Optional.empty();
  }

  private DietPlanResponse toResponse(DietPlan plan) {
    return DietPlanResponse.builder()
        .id(plan.getId())
        .title(plan.getTitle())
        .totalCalories(plan.getTotalCalories())
        .totalProteinG(plan.getTotalProteinG())
        .totalCarbsG(plan.getTotalCarbsG())
        .totalFatG(plan.getTotalFatG())
        .totalSugarG(plan.getTotalSugarG())
        .validForDate(plan.getValidForDate())
        .generatedAt(plan.getGeneratedAt())
        .status(plan.getStatus().name())
        .aiExplanation(plan.getAiExplanation())
        .disclaimer(disclaimerFor(plan.getUser().getId()))
        .meals(plan.getMeals().stream().map(this::mealToResponse).toList())
        .build();
  }

  private MealResponse mealToResponse(Meal meal) {
    return MealResponse.builder()
        .id(meal.getId())
        .name(meal.getName())
        .mealType(meal.getMealType().name())
        .calories(meal.getCalories())
        .proteinG(meal.getProteinG())
        .carbsG(meal.getCarbsG())
        .fatG(meal.getFatG())
        .sugarG(meal.getSugarG())
        .fiberG(meal.getFiberG())
        .sortOrder(meal.getSortOrder())
        .aiSuggestion(meal.getAiSuggestion())
        .build();
  }

  @Transactional
  public void archive(Long userId, Long planId) {
    DietPlan plan = getByIdForUser(userId, planId);
    plan.setStatus(PlanStatus.ARCHIVED);
    planRepository.save(plan);
  }

  /**
   * One eligible food per meal, sized to the slot's calories (replaced by the composer in US3). For
   * each slot, candidates are tried in random order until the meal passes the validator.
   */
  private List<Meal> buildMeals(
      List<FoodItem> foods, int targetKcal, SafetyConstraints constraints) {
    Random rng = new Random();
    List<Meal> meals = new ArrayList<>();
    int sortOrder = 1;

    for (MealType type : MealType.values()) {
      int targetCal = (int) (targetKcal * MEAL_CALORIE_RATIO.get(type));
      List<FoodItem> candidates = new ArrayList<>(foods);
      Collections.shuffle(candidates, rng);

      Meal meal = null;
      Violation lastViolation = null;
      for (FoodItem candidate : candidates) {
        double cal100 = candidate.getCaloriesPer100g();
        if (cal100 <= 0) continue;
        double grams = Math.round((targetCal / cal100) * 100 * 10.0) / 10.0;
        MealDraft draft =
            new MealDraft(
                Slot.valueOf(type.name()),
                List.of(new Component(FoodMapper.toView(candidate), grams)));
        List<Violation> violations = PlanSafetyValidator.validateMeal(draft, constraints);
        if (!violations.isEmpty()) {
          lastViolation = violations.get(0);
          continue;
        }
        meal = toMeal(type, sortOrder++, candidate, grams);
        break;
      }
      if (meal == null) {
        throw SafetyService.infeasible(
            "We could not build a safe "
                + type.name().toLowerCase(Locale.ROOT).replace('_', ' ')
                + " with your restrictions.",
            lastViolation);
      }
      meals.add(meal);
    }
    return meals;
  }

  private Meal toMeal(MealType type, int sortOrder, FoodItem food, double grams) {
    MealFoodItem mfi = MealFoodItem.builder().foodItem(food).quantityG(grams).build();
    List<MealFoodItem> items = new ArrayList<>();
    items.add(mfi);
    Meal meal =
        Meal.builder()
            .name(type.name().charAt(0) + type.name().substring(1).toLowerCase().replace("_", " "))
            .mealType(type)
            .sortOrder(sortOrder)
            .mealFoodItems(items)
            .build();
    mfi.setMeal(meal);
    nutritionService.computeMealNutrition(meal);
    return meal;
  }
}
