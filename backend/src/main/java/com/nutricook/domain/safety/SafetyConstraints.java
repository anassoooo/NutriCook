package com.nutricook.domain.safety;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Every hard constraint that applies to one user, merged from their restrictions and health
 * conditions (BR-SAF-02/06, BR-PRF-07). Each constraint remembers which restriction or condition it
 * came from, so violations can name it.
 */
public final class SafetyConstraints {

  /** A numeric ceiling and the condition that set it. */
  public record Limit(double value, String source) {}

  public static final SafetyConstraints NONE = from(List.of(), List.of());

  private final Map<Allergen, String> excludedAllergens;
  private final Map<DietFlag, String> requiredFlags;
  private final Limit sugarPerMeal;
  private final Limit fatPerMeal;
  private final Limit sodiumPerDay;

  private SafetyConstraints(
      Map<Allergen, String> excludedAllergens,
      Map<DietFlag, String> requiredFlags,
      Limit sugarPerMeal,
      Limit fatPerMeal,
      Limit sodiumPerDay) {
    this.excludedAllergens = Collections.unmodifiableMap(excludedAllergens);
    this.requiredFlags = Collections.unmodifiableMap(requiredFlags);
    this.sugarPerMeal = sugarPerMeal;
    this.fatPerMeal = fatPerMeal;
    this.sodiumPerDay = sodiumPerDay;
  }

  /** Merges all rules; when several set the same limit, the strictest (lowest) wins. */
  public static SafetyConstraints from(
      Collection<RestrictionRule> restrictions, Collection<ConditionRule> conditions) {
    Map<Allergen, String> allergens = new EnumMap<>(Allergen.class);
    Map<DietFlag, String> flags = new EnumMap<>(DietFlag.class);
    for (RestrictionRule r : restrictions) {
      r.excludedAllergens().forEach(a -> allergens.putIfAbsent(a, r.name()));
      if (r.requiredFlag() != null) flags.putIfAbsent(r.requiredFlag(), r.name());
    }
    Limit sugar = null;
    Limit fat = null;
    Limit sodium = null;
    for (ConditionRule c : conditions) {
      // BR-PRF-07: e.g. Celiac Disease excludes GLUTEN even without a Gluten-Free restriction
      c.impliedAllergens().forEach(a -> allergens.putIfAbsent(a, c.name()));
      sugar = stricter(sugar, c.sugarPerMealG(), c.name());
      fat = stricter(fat, c.fatPerMealG(), c.name());
      sodium = stricter(sodium, c.sodiumPerDayMg(), c.name());
    }
    return new SafetyConstraints(allergens, flags, sugar, fat, sodium);
  }

  private static Limit stricter(Limit current, Double value, String source) {
    if (value == null) return current;
    return current == null || value < current.value() ? new Limit(value, source) : current;
  }

  /** Allergen → the restriction or condition that excludes it. */
  public Map<Allergen, String> excludedAllergens() {
    return excludedAllergens;
  }

  /** Required flag → the restriction that requires it. */
  public Map<DietFlag, String> requiredFlags() {
    return requiredFlags;
  }

  /** BR-SAF-03: users with any allergy or intolerance never get foods with unverified allergens. */
  public boolean failClosedOnUnknownAllergens() {
    return !excludedAllergens.isEmpty();
  }

  public boolean kosher() {
    return requiredFlags.containsKey(DietFlag.KOSHER);
  }

  /** May be null (no limit). */
  public Limit sugarPerMeal() {
    return sugarPerMeal;
  }

  /** May be null (no limit). */
  public Limit fatPerMeal() {
    return fatPerMeal;
  }

  /** May be null (no limit). */
  public Limit sodiumPerDay() {
    return sodiumPerDay;
  }
}
