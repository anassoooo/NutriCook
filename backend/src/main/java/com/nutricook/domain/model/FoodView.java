package com.nutricook.domain.model;

import java.util.Objects;
import java.util.Set;

/**
 * Read-only view of a catalog food, as the business rules see it (BR-CAT-01).
 *
 * @param allergensKnown false when the allergen data has not been verified: such a food is
 *     ineligible for users with any allergy or intolerance (BR-SAF-03)
 * @param meat land-animal meat, needed for the kosher meat/milk rule (BR-SAF-09) and religious
 *     certification (BR-SAF-10)
 */
public record FoodView(
    long id,
    String name,
    String category,
    FoodRole role,
    FoodState state,
    Nutrients per100g,
    Set<Allergen> allergens,
    boolean allergensKnown,
    Set<DietFlag> dietFlags,
    boolean meat,
    PortionRule portion,
    Set<Slot> suitableSlots) {

  public FoodView {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(per100g, "per100g");
    Objects.requireNonNull(portion, "portion");
    allergens = Set.copyOf(allergens);
    dietFlags = Set.copyOf(dietFlags);
    suitableSlots = Set.copyOf(suitableSlots);
  }

  public Nutrients nutrientsFor(double grams) {
    return per100g.forGrams(grams);
  }

  public boolean contains(Allergen allergen) {
    return allergens.contains(allergen);
  }

  public boolean has(DietFlag flag) {
    return dietFlags.contains(flag);
  }
}
