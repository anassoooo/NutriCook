package com.nutricook.domain.plan;

import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.MealDraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sizes the foods chosen for one meal (BR-CMP-02/03). Every portion is snapped to the food's step
 * and clamped to its bounds, so the result is always a legal portion.
 *
 * <ul>
 *   <li>Main meal: PRODUCE at its default portion; PROTEIN sized to the slot's protein goal minus
 *       what the other foods bring (at most half the meal's energy); CARB fills the remaining
 *       energy; an optional FAT closes any gap that is left.
 *   <li>Snack: PRODUCE at its default portion, the other component fills the slot's energy.
 * </ul>
 */
public final class PortionSolver {

  private PortionSolver() {}

  public static MealDraft solve(
      SlotGoal goal, Map<FoodRole, FoodView> picks, FoodView optionalFat) {
    List<Component> components = new ArrayList<>();
    FoodView produce = picks.get(FoodRole.PRODUCE);

    if (goal.slot().isMain()) {
      FoodView protein = picks.get(FoodRole.PROTEIN);
      FoodView carb = picks.get(FoodRole.CARB);
      Component v = new Component(produce, produce.portion().defaultG());

      // PROTEIN covers what the produce does not, but never more than half the meal's energy
      Component p = sizeProtein(protein, goal, protein(v));
      Component c = fill(carb, goal.kcal() - kcal(p) - kcal(v));
      // Second pass: the carb brings protein too; trim the protein portion if it overshoots
      if (protein(p) + protein(v) + protein(c) > goal.proteinG() * (1 + PROTEIN_SLACK)) {
        p = sizeProtein(protein, goal, protein(v) + protein(c));
        c = fill(carb, goal.kcal() - kcal(p) - kcal(v));
      }
      components.add(p);
      components.add(c);
      components.add(v);

      double gap = goal.kcal() - kcal(p) - kcal(v) - kcal(c);
      if (optionalFat != null) {
        double smallestFatPortion = optionalFat.nutrientsFor(optionalFat.portion().minG()).kcal();
        if (gap > smallestFatPortion / 2) components.add(fill(optionalFat, gap));
      }
    } else {
      FoodView other =
          picks.containsKey(FoodRole.PROTEIN)
              ? picks.get(FoodRole.PROTEIN)
              : picks.get(FoodRole.FAT);
      Component v = new Component(produce, produce.portion().defaultG());
      components.add(fill(other, goal.kcal() - kcal(v)));
      components.add(v);
    }
    return new MealDraft(goal.slot(), components);
  }

  /** Protein above the slot goal tolerated before the portion is trimmed. */
  static final double PROTEIN_SLACK = 0.15;

  /** The protein food may use at most this share of the meal's energy. */
  static final double MAX_PROTEIN_ENERGY_SHARE = 0.5;

  private static Component sizeProtein(FoodView protein, SlotGoal goal, double proteinFromOthers) {
    double perGram = protein.per100g().proteinG() / 100;
    double kcalPerGram = protein.per100g().kcal() / 100;
    double grams =
        perGram > 0
            ? Math.max(0, goal.proteinG() - proteinFromOthers) / perGram
            : protein.portion().defaultG();
    if (kcalPerGram > 0)
      grams = Math.min(grams, MAX_PROTEIN_ENERGY_SHARE * goal.kcal() / kcalPerGram);
    return new Component(protein, protein.portion().normalize(grams));
  }

  /** The legal portion of {@code food} closest to {@code kcal} of energy. */
  static Component fill(FoodView food, double kcal) {
    double kcalPerGram = food.per100g().kcal() / 100;
    double grams = kcal <= 0 || kcalPerGram <= 0 ? food.portion().minG() : kcal / kcalPerGram;
    return new Component(food, food.portion().normalize(grams));
  }

  private static double kcal(Component c) {
    return c.nutrients().kcal();
  }

  private static double protein(Component c) {
    return c.nutrients().proteinG();
  }
}
