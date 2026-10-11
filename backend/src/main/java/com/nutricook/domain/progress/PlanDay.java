package com.nutricook.domain.progress;

import java.util.List;

/**
 * The ACTIVE plan of a day, as progress tracking sees it.
 *
 * @param targetCalories the calorie target the plan was built for (BR-PRG-06)
 * @param checkedMealKcal the calories of each meal the user checked off (BR-PRG-05)
 */
public record PlanDay(int targetCalories, List<Integer> checkedMealKcal) {
  public PlanDay {
    checkedMealKcal = List.copyOf(checkedMealKcal);
  }
}
