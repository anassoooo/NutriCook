package com.nutricook.domain.safety;

import com.nutricook.domain.model.Allergen;
import java.util.Objects;
import java.util.Set;

/**
 * Hard constraints implied by one health condition (BR-SAF-06, BR-PRF-07). Null limits mean "no
 * limit".
 */
public record ConditionRule(
    String name,
    Double sugarPerMealG,
    Double fatPerMealG,
    Double sodiumPerDayMg,
    Set<Allergen> impliedAllergens) {
  public ConditionRule {
    Objects.requireNonNull(name, "name");
    impliedAllergens = Set.copyOf(impliedAllergens);
  }
}
