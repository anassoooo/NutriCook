package com.nutricook.dto.response;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodState;
import com.nutricook.domain.model.Slot;
import com.nutricook.entity.FoodItem;
import com.nutricook.entity.enums.FoodOrigin;
import java.util.Set;
import java.util.TreeSet;

/** A catalog food for administration (BR-CAT-01). */
public record FoodResponse(
    Long id,
    String name,
    String category,
    FoodRole role,
    FoodState state,
    double caloriesPer100g,
    double proteinPer100g,
    double carbsPer100g,
    double fatPer100g,
    double sugarPer100g,
    double fiberPer100g,
    double sodiumMgPer100g,
    double portionMinG,
    double portionMaxG,
    double portionStepG,
    double defaultPortionG,
    Set<Allergen> allergens,
    Set<DietFlag> dietFlags,
    Set<Slot> suitableSlots,
    boolean meat,
    String source,
    FoodOrigin origin,
    boolean active) {

  public static FoodResponse of(FoodItem f) {
    return new FoodResponse(
        f.getId(),
        f.getName(),
        f.getCategory(),
        f.getRole(),
        f.getState(),
        f.getCaloriesPer100g(),
        f.getProteinPer100g(),
        f.getCarbsPer100g(),
        f.getFatPer100g(),
        orZero(f.getSugarPer100g()),
        orZero(f.getFiberPer100g()),
        orZero(f.getSodiumMgPer100g()),
        f.getPortionMinG(),
        f.getPortionMaxG(),
        f.getPortionStepG(),
        f.getDefaultPortionG(),
        new TreeSet<>(f.getAllergens()),
        new TreeSet<>(f.getDietFlags()),
        new TreeSet<>(f.getSuitableSlots()),
        f.isMeat(),
        f.getSource(),
        f.getOrigin(),
        f.isActive());
  }

  private static double orZero(Double v) {
    return v == null ? 0 : v;
  }
}
