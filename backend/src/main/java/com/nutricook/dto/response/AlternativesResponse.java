package com.nutricook.dto.response;

import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.FoodRole;
import java.util.List;

/** Foods that could replace the {@code role} component of a meal (FR-003). */
public record AlternativesResponse(FoodRole role, List<Option> options) {

  /** A replacement at the portion it would get, so the user sees what they will eat. */
  public record Option(Long foodId, String foodName, double grams, double kcal, double proteinG) {

    public static Option of(Component c) {
      var n = c.nutrients();
      return new Option(
          c.food().id(),
          c.food().name(),
          c.grams(),
          Math.round(n.kcal() * 10) / 10.0,
          Math.round(n.proteinG() * 10) / 10.0);
    }
  }
}
