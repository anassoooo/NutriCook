package com.nutricook.domain.safety;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.FoodView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Decides whether one food may appear in a user's plan at all (BR-SAF-01..03, BR-SAF-10).
 *
 * <p>Uses only structured attributes (allergens, verification status, diet flags), never the name
 * or category. Fails closed: missing data means "not allowed".
 */
public final class EligibilityPolicy {

  private EligibilityPolicy() {}

  public static boolean allows(FoodView food, SafetyConstraints c) {
    return check(food, c).isEmpty();
  }

  /** Every reason the food is not allowed; empty when it is. */
  public static List<Violation> check(FoodView food, SafetyConstraints c) {
    List<Violation> violations = new ArrayList<>();

    if (c.failClosedOnUnknownAllergens() && !food.allergensKnown()) {
      String source = c.excludedAllergens().values().iterator().next();
      violations.add(
          new Violation(
              Violation.Type.UNKNOWN_ALLERGENS,
              source,
              food.name() + " has unverified allergen information."));
    }

    for (Map.Entry<Allergen, String> excluded : c.excludedAllergens().entrySet()) {
      if (food.contains(excluded.getKey())) {
        violations.add(
            new Violation(
                Violation.Type.ALLERGEN,
                excluded.getValue(),
                food.name()
                    + " contains "
                    + label(excluded.getKey())
                    + ", excluded by "
                    + excluded.getValue()
                    + "."));
      }
    }

    for (Map.Entry<DietFlag, String> required : c.requiredFlags().entrySet()) {
      if (!food.has(required.getKey())) {
        violations.add(
            new Violation(
                Violation.Type.DIET_FLAG,
                required.getValue(),
                food.name() + " is not " + label(required.getKey()) + "."));
      }
    }
    return violations;
  }

  static String label(Enum<?> e) {
    return e.name().toLowerCase().replace('_', ' ');
  }
}
