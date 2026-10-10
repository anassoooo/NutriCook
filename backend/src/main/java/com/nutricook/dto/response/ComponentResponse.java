package com.nutricook.dto.response;

import com.nutricook.domain.model.FoodRole;
import com.nutricook.entity.MealFoodItem;

/** One food in a meal, with the nutrition snapshot taken at generation time (BR-LIF-05). */
public record ComponentResponse(
    Long foodId,
    String foodName,
    FoodRole role,
    double grams,
    double kcal,
    double proteinG,
    double carbsG,
    double fatG) {

  public static ComponentResponse of(MealFoodItem item) {
    return new ComponentResponse(
        item.getFoodItem().getId(),
        item.getFoodItem().getName(),
        item.getRole(),
        item.getQuantityG(),
        item.getKcal(),
        item.getProteinG(),
        item.getCarbsG(),
        item.getFatG());
  }
}
