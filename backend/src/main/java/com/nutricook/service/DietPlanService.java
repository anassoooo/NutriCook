package com.nutricook.service;

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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DietPlanService {

  private final DietPlanRepository planRepository;
  private final FoodItemRepository foodRepository;
  private final UserRepository userRepository;
  private final UserProfileService profileService;
  private final NutritionService nutritionService;
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

    planRepository.deleteAll(planRepository.findByUserIdAndValidForDate(userId, date));

    int tdee = nutritionService.computeTDEE(profile);
    List<FoodItem> foods = selectCompatibleFoods(profile);

    List<Meal> meals = buildMeals(foods, tdee, profile);

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
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Plan does not belong to this user");
    }
    return plan;
  }

  public List<DietPlan> listForUser(Long userId) {
    return planRepository.findByUserIdOrderByGeneratedAtDesc(userId);
  }

  @Transactional
  public ChatResponse chat(Long userId, Long planId, String message) {
    DietPlan plan = getByIdForUser(userId, planId);
    UserProfile profile = profileService.getByUserId(userId);

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

    GroqAiService.ChatResult result = groqService.chat(plan, profile, message, progressContext);

    DietPlanResponse updatedPlanResponse = null;
    if (result.swapMealType() != null && result.swapFoodName() != null) {
      boolean swapped = applySwap(plan, result.swapMealType(), result.swapFoodName());
      if (swapped) {
        DietPlan refreshed = planRepository.findByIdWithMeals(planId).orElse(plan);
        updatedPlanResponse = toResponse(refreshed);
      }
    }

    return ChatResponse.builder().reply(result.reply()).updatedPlan(updatedPlanResponse).build();
  }

  private boolean applySwap(DietPlan plan, String mealTypeName, String foodName) {
    MealType mealType;
    try {
      mealType = MealType.valueOf(mealTypeName.toUpperCase());
    } catch (IllegalArgumentException e) {
      return false;
    }

    Meal meal =
        plan.getMeals().stream().filter(m -> m.getMealType() == mealType).findFirst().orElse(null);
    if (meal == null) return false;

    FoodItem food = foodRepository.findFirstByNameContainingIgnoreCase(foodName).orElse(null);
    if (food == null) return false;

    int targetCal = meal.getCalories() != null ? meal.getCalories() : 400;
    double grams =
        food.getCaloriesPer100g() > 0 ? (targetCal / food.getCaloriesPer100g()) * 100 : 100;

    MealFoodItem newMfi =
        MealFoodItem.builder()
            .foodItem(food)
            .quantityG(Math.round(grams * 10.0) / 10.0)
            .meal(meal)
            .build();

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
    return true;
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

  private List<FoodItem> selectCompatibleFoods(UserProfile profile) {
    List<FoodItem> allFoods = foodRepository.findAll();
    Set<String> restrictionNames =
        profile.getDietaryRestrictions().stream()
            .map(r -> r.getName().toLowerCase())
            .collect(Collectors.toSet());

    return allFoods.stream()
        .filter(f -> isFoodCompatible(f, restrictionNames))
        .collect(Collectors.toList());
  }

  private boolean isFoodCompatible(FoodItem food, Set<String> restrictionNames) {
    String cat = food.getCategory() != null ? food.getCategory().toLowerCase() : "";
    if (restrictionNames.contains("vegan")
        && (cat.equals("meat")
            || cat.equals("seafood")
            || cat.equals("dairy")
            || cat.equals("egg"))) return false;
    if (restrictionNames.contains("vegetarian") && (cat.equals("meat") || cat.equals("seafood")))
      return false;
    if (restrictionNames.contains("gluten-free") && cat.equals("grain")) return false;
    if (restrictionNames.contains("lactose-free") && cat.equals("dairy")) return false;
    if (restrictionNames.contains("nut allergy") && cat.equals("nut")) return false;
    return true;
  }

  private List<Meal> buildMeals(List<FoodItem> foods, int tdee, UserProfile profile) {
    if (foods.isEmpty()) foods = foodRepository.findAll();
    Random rng = new Random();
    List<Meal> meals = new ArrayList<>();
    int sortOrder = 1;

    for (MealType type : MealType.values()) {
      int targetCal = (int) (tdee * MEAL_CALORIE_RATIO.get(type));
      FoodItem chosen = foods.get(rng.nextInt(foods.size()));
      double cal100 = chosen.getCaloriesPer100g();
      double grams = cal100 > 0 ? (targetCal / cal100) * 100 : 100.0;

      MealFoodItem mfi =
          MealFoodItem.builder()
              .foodItem(chosen)
              .quantityG(Math.round(grams * 10.0) / 10.0)
              .build();

      List<MealFoodItem> items = new ArrayList<>();
      items.add(mfi);
      Meal meal =
          Meal.builder()
              .name(
                  type.name().charAt(0) + type.name().substring(1).toLowerCase().replace("_", " "))
              .mealType(type)
              .sortOrder(sortOrder++)
              .mealFoodItems(items)
              .build();
      mfi.setMeal(meal);

      nutritionService.computeMealNutrition(meal);

      Set<HealthCondition> conditions = profile.getHealthConditions();
      if (!nutritionService.isMealCompatible(meal, conditions)) {
        for (FoodItem alt : foods) {
          if (alt.equals(chosen)) continue;
          double altCal = alt.getCaloriesPer100g();
          if (altCal <= 0) continue;
          double altG = (targetCal / altCal) * 100;
          MealFoodItem altMfi = MealFoodItem.builder().foodItem(alt).quantityG(altG).build();
          meal.getMealFoodItems().set(0, altMfi);
          altMfi.setMeal(meal);
          nutritionService.computeMealNutrition(meal);
          if (nutritionService.isMealCompatible(meal, conditions)) break;
        }
      }

      meals.add(meal);
    }
    return meals;
  }
}
