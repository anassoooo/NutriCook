package com.nutricook.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    planRepository.archiveActiveForDate(userId, date);

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

    return planRepository.save(plan);
  }

  public DietPlan getById(Long planId) {
    return planRepository
        .findById(planId)
        .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
  }

  public List<DietPlan> listForUser(Long userId) {
    return planRepository.findByUserIdOrderByGeneratedAtDesc(userId);
  }

  @Transactional
  public void archive(Long planId) {
    DietPlan plan = getById(planId);
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
    if (restrictionNames.contains("vegan") && cat.contains("meat")) return false;
    if (restrictionNames.contains("vegan") && cat.contains("dairy")) return false;
    if (restrictionNames.contains("vegetarian") && cat.contains("meat")) return false;
    if (restrictionNames.contains("gluten-free") && cat.contains("grain")) return false;
    if (restrictionNames.contains("lactose-free") && cat.contains("dairy")) return false;
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
      double grams = (targetCal / chosen.getCaloriesPer100g()) * 100;

      MealFoodItem mfi =
          MealFoodItem.builder()
              .foodItem(chosen)
              .quantityG(Math.round(grams * 10.0) / 10.0)
              .build();

      Meal meal =
          Meal.builder()
              .name(
                  type.name().charAt(0) + type.name().substring(1).toLowerCase().replace("_", " "))
              .mealType(type)
              .sortOrder(sortOrder++)
              .mealFoodItems(List.of(mfi))
              .build();
      mfi.setMeal(meal);

      nutritionService.computeMealNutrition(meal);

      Set<HealthCondition> conditions = profile.getHealthConditions();
      if (!nutritionService.isMealCompatible(meal, conditions)) {
        for (FoodItem alt : foods) {
          if (alt.equals(chosen)) continue;
          double altG = (targetCal / alt.getCaloriesPer100g()) * 100;
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
