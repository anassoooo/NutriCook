package com.nutricook.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * An amount of energy and nutrients. Immutable; arithmetic returns new instances.
 *
 * <p>Values are kept unrounded through calculations and rounded once with {@link #rounded()} at the
 * meal level (BR-CMP-09): kcal and sodium to integers, grams to one decimal, half-up.
 */
public record Nutrients(
    double kcal,
    double proteinG,
    double carbsG,
    double fatG,
    double sugarG,
    double fiberG,
    double sodiumMg) {

  public static final Nutrients ZERO = new Nutrients(0, 0, 0, 0, 0, 0, 0);

  public Nutrients {
    requireValid("kcal", kcal);
    requireValid("proteinG", proteinG);
    requireValid("carbsG", carbsG);
    requireValid("fatG", fatG);
    requireValid("sugarG", sugarG);
    requireValid("fiberG", fiberG);
    requireValid("sodiumMg", sodiumMg);
  }

  public Nutrients plus(Nutrients o) {
    return new Nutrients(
        kcal + o.kcal,
        proteinG + o.proteinG,
        carbsG + o.carbsG,
        fatG + o.fatG,
        sugarG + o.sugarG,
        fiberG + o.fiberG,
        sodiumMg + o.sodiumMg);
  }

  public Nutrients scale(double factor) {
    if (factor < 0 || !Double.isFinite(factor)) {
      throw new IllegalArgumentException("factor must be finite and >= 0: " + factor);
    }
    return new Nutrients(
        kcal * factor,
        proteinG * factor,
        carbsG * factor,
        fatG * factor,
        sugarG * factor,
        fiberG * factor,
        sodiumMg * factor);
  }

  /** Nutrients of {@code grams} of a food whose values are given per 100 g (BR-CMP-09). */
  public Nutrients forGrams(double grams) {
    return scale(grams / 100.0);
  }

  public static Nutrients sum(Collection<Nutrients> parts) {
    return parts.stream().reduce(ZERO, Nutrients::plus);
  }

  /** Display/storage precision (BR-CMP-09): kcal and sodium integer, grams 0.1. */
  public Nutrients rounded() {
    return new Nutrients(
        round(kcal, 0),
        round(proteinG, 1),
        round(carbsG, 1),
        round(fatG, 1),
        round(sugarG, 1),
        round(fiberG, 1),
        round(sodiumMg, 0));
  }

  private static double round(double value, int decimals) {
    return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
  }

  private static void requireValid(String name, double value) {
    if (!Double.isFinite(value) || value < 0) {
      throw new IllegalArgumentException(name + " must be finite and >= 0: " + value);
    }
  }
}
