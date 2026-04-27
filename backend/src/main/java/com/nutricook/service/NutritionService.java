package com.nutricook.service;

import com.nutricook.entity.HealthCondition;
import com.nutricook.entity.Meal;
import com.nutricook.entity.MealFoodItem;
import com.nutricook.entity.UserProfile;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class NutritionService {

  public record NutritionTotals(
      int calories, double proteinG, double carbsG, double fatG, double sugarG) {}

  public NutritionTotals computeTotals(List<Meal> meals) {
    int cal = meals.stream().mapToInt(m -> m.getCalories() != null ? m.getCalories() : 0).sum();
    double protein =
        meals.stream().mapToDouble(m -> m.getProteinG() != null ? m.getProteinG() : 0).sum();
    double carbs = meals.stream().mapToDouble(m -> m.getCarbsG() != null ? m.getCarbsG() : 0).sum();
    double fat = meals.stream().mapToDouble(m -> m.getFatG() != null ? m.getFatG() : 0).sum();
    double sugar = meals.stream().mapToDouble(m -> m.getSugarG() != null ? m.getSugarG() : 0).sum();
    return new NutritionTotals(cal, round(protein), round(carbs), round(fat), round(sugar));
  }

  public void computeMealNutrition(Meal meal) {
    List<MealFoodItem> items = meal.getMealFoodItems();
    if (items == null || items.isEmpty()) return;

    double cal = 0, protein = 0, carbs = 0, fat = 0, sugar = 0, fiber = 0;
    for (MealFoodItem mfi : items) {
      double ratio = mfi.getQuantityG() / 100.0;
      cal += mfi.getFoodItem().getCaloriesPer100g() * ratio;
      protein += mfi.getFoodItem().getProteinPer100g() * ratio;
      carbs += mfi.getFoodItem().getCarbsPer100g() * ratio;
      fat += mfi.getFoodItem().getFatPer100g() * ratio;
      sugar += mfi.getFoodItem().getSugarPer100g() * ratio;
      fiber += mfi.getFoodItem().getFiberPer100g() * ratio;
    }
    meal.setCalories((int) Math.round(cal));
    meal.setProteinG(round(protein));
    meal.setCarbsG(round(carbs));
    meal.setFatG(round(fat));
    meal.setSugarG(round(sugar));
    meal.setFiberG(round(fiber));
  }

  public boolean isMealCompatible(Meal meal, Set<HealthCondition> conditions) {
    for (HealthCondition c : conditions) {
      if (c.getSugarLimitG() != null
          && meal.getSugarG() != null
          && meal.getSugarG() > c.getSugarLimitG()) return false;
      if (c.getFatLimitG() != null && meal.getFatG() != null && meal.getFatG() > c.getFatLimitG())
        return false;
    }
    return true;
  }

  /** Harris-Benedict TDEE calculation. */
  public int computeTDEE(UserProfile profile) {
    if (profile.getWeightKg() == null
        || profile.getHeightCm() == null
        || profile.getDateOfBirth() == null) {
      return 2000;
    }
    int age = java.time.LocalDate.now().getYear() - profile.getDateOfBirth().getYear();
    double bmr;
    if (profile.getGender() == com.nutricook.entity.enums.Gender.FEMALE) {
      bmr = 655 + (9.6 * profile.getWeightKg()) + (1.8 * profile.getHeightCm()) - (4.7 * age);
    } else {
      bmr = 66 + (13.7 * profile.getWeightKg()) + (5 * profile.getHeightCm()) - (6.8 * age);
    }
    double multiplier =
        switch (profile.getActivityLevel() != null
            ? profile.getActivityLevel()
            : com.nutricook.entity.enums.ActivityLevel.SEDENTARY) {
          case SEDENTARY -> 1.2;
          case LIGHTLY_ACTIVE -> 1.375;
          case MODERATELY_ACTIVE -> 1.55;
          case VERY_ACTIVE -> 1.725;
          case EXTRA_ACTIVE -> 1.9;
        };
    double tdee = bmr * multiplier;
    if (profile.getHealthGoal() != null) {
      tdee =
          switch (profile.getHealthGoal()) {
            case LOSE_WEIGHT -> tdee - 500;
            case GAIN_MUSCLE -> tdee + 300;
            default -> tdee;
          };
    }
    return Math.max(1200, (int) Math.round(tdee));
  }

  private double round(double v) {
    return Math.round(v * 10.0) / 10.0;
  }
}
