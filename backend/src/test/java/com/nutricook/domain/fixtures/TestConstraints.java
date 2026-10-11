package com.nutricook.domain.fixtures;

import static com.nutricook.domain.model.Allergen.*;

import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.safety.ConditionRule;
import com.nutricook.domain.safety.RestrictionRule;
import java.util.EnumSet;
import java.util.Set;

/** The restrictions and conditions exactly as V4__reference_data.sql seeds them. */
public final class TestConstraints {

  private TestConstraints() {}

  public static final RestrictionRule VEGAN =
      new RestrictionRule("Vegan", Set.of(), DietFlag.VEGAN);
  public static final RestrictionRule VEGETARIAN =
      new RestrictionRule("Vegetarian", Set.of(), DietFlag.VEGETARIAN);
  public static final RestrictionRule GLUTEN_FREE =
      new RestrictionRule("Gluten-Free", EnumSet.of(GLUTEN), null);
  public static final RestrictionRule LACTOSE_FREE =
      new RestrictionRule("Lactose-Free", EnumSet.of(MILK), null);
  public static final RestrictionRule NUT_ALLERGY =
      new RestrictionRule("Nut Allergy", EnumSet.of(PEANUTS, TREE_NUTS), null);
  public static final RestrictionRule SHELLFISH_ALLERGY =
      new RestrictionRule("Shellfish Allergy", EnumSet.of(CRUSTACEANS, MOLLUSCS), null);
  public static final RestrictionRule EGG_ALLERGY =
      new RestrictionRule("Egg Allergy", EnumSet.of(EGGS), null);
  public static final RestrictionRule SOY_ALLERGY =
      new RestrictionRule("Soy Allergy", EnumSet.of(SOY), null);
  public static final RestrictionRule FISH_ALLERGY =
      new RestrictionRule("Fish Allergy", EnumSet.of(FISH), null);
  public static final RestrictionRule SESAME_ALLERGY =
      new RestrictionRule("Sesame Allergy", EnumSet.of(SESAME), null);
  public static final RestrictionRule HALAL =
      new RestrictionRule("Halal", Set.of(), DietFlag.HALAL);
  public static final RestrictionRule KOSHER =
      new RestrictionRule("Kosher", Set.of(), DietFlag.KOSHER);

  public static final ConditionRule DIABETES =
      new ConditionRule("Diabetes", 15.0, null, null, Set.of());
  public static final ConditionRule HIGH_CHOLESTEROL =
      new ConditionRule("High Cholesterol", null, 25.0, null, Set.of());
  public static final ConditionRule HYPERTENSION =
      new ConditionRule("Hypertension", null, null, 2000.0, Set.of());
  public static final ConditionRule CELIAC =
      new ConditionRule("Celiac Disease", null, null, null, EnumSet.of(GLUTEN));
  public static final ConditionRule LACTOSE_INTOLERANCE =
      new ConditionRule("Lactose Intolerance", null, null, null, EnumSet.of(MILK));
}
