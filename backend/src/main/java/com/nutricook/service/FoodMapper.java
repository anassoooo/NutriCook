package com.nutricook.service;

import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.PlanDraft;
import com.nutricook.domain.model.PortionRule;
import com.nutricook.domain.model.Slot;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.FoodItem;
import com.nutricook.entity.Meal;
import com.nutricook.entity.MealFoodItem;
import java.time.LocalDate;
import java.util.List;

/** Maps catalog and plan entities to the domain's views (plan D1). */
final class FoodMapper {

  private FoodMapper() {}

  static FoodView toView(FoodItem f) {
    return new FoodView(
        f.getId(),
        f.getName(),
        f.getCategory(),
        f.getRole(),
        f.getState(),
        per100g(f),
        f.getAllergens(),
        f.isAllergensKnown(),
        f.getDietFlags(),
        f.isMeat(),
        new PortionRule(
            f.getPortionMinG(), f.getPortionMaxG(), f.getPortionStepG(), f.getDefaultPortionG()),
        f.getSuitableSlots());
  }

  static Nutrients per100g(FoodItem f) {
    return new Nutrients(
        f.getCaloriesPer100g(),
        f.getProteinPer100g(),
        f.getCarbsPer100g(),
        f.getFatPer100g(),
        orZero(f.getSugarPer100g()),
        orZero(f.getFiberPer100g()),
        orZero(f.getSodiumMgPer100g()));
  }

  static MealDraft toDraft(Meal meal) {
    return new MealDraft(
        Slot.valueOf(meal.getMealType().name()),
        meal.getMealFoodItems().stream().map(FoodMapper::toComponent).toList());
  }

  static PlanDraft toDraft(LocalDate date, List<Meal> meals) {
    return new PlanDraft(date, meals.stream().map(FoodMapper::toDraft).toList(), List.of());
  }

  static PlanDraft toDraft(DietPlan plan) {
    return toDraft(plan.getValidForDate(), plan.getMeals());
  }

  private static Component toComponent(MealFoodItem item) {
    return new Component(toView(item.getFoodItem()), item.getQuantityG());
  }

  private static double orZero(Double value) {
    return value == null ? 0 : value;
  }
}
