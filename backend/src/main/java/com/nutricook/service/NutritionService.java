package com.nutricook.service;

import com.nutricook.entity.Meal;
import com.nutricook.entity.MealFoodItem;
import java.util.List;
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

  private double round(double v) {
    return Math.round(v * 10.0) / 10.0;
  }
}
