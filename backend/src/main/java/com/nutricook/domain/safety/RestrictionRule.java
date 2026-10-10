package com.nutricook.domain.safety;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import java.util.Objects;
import java.util.Set;

/**
 * What one dietary restriction requires (BR-SAF-02): allergens a food must not contain and/or a
 * diet flag it must carry.
 *
 * @param requiredFlag may be null
 */
public record RestrictionRule(String name, Set<Allergen> excludedAllergens, DietFlag requiredFlag) {
  public RestrictionRule {
    Objects.requireNonNull(name, "name");
    excludedAllergens = Set.copyOf(excludedAllergens);
  }
}
