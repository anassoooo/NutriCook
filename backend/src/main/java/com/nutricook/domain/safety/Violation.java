package com.nutricook.domain.safety;

/**
 * A broken hard constraint.
 *
 * @param constraint the restriction or condition that was violated, e.g. "Nut Allergy"
 * @param message a sentence a user can read
 */
public record Violation(Type type, String constraint, String message) {

  public enum Type {
    /** The food contains an allergen the user must avoid (BR-SAF-02). */
    ALLERGEN,
    /** Allergen data is not verified and the user has an allergy/intolerance (BR-SAF-03). */
    UNKNOWN_ALLERGENS,
    /** The food lacks a required diet flag, e.g. VEGAN or HALAL (BR-SAF-02/10). */
    DIET_FLAG,
    SUGAR_PER_MEAL,
    FAT_PER_MEAL,
    SODIUM_PER_DAY,
    /** Kosher: meat and milk in the same meal (BR-SAF-09). */
    KOSHER_MEAT_WITH_MILK
  }
}
