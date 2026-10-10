package com.nutricook.domain.model;

/**
 * Allowed portion sizes for a food (BR-CMP-03): every portion lies in {@code [minG, maxG]} and is a
 * multiple of {@code stepG}. {@code defaultG} is used when a component is not solved for (e.g.
 * PRODUCE).
 */
public record PortionRule(double minG, double maxG, double stepG, double defaultG) {

  private static final double EPS = 1e-9;

  public PortionRule {
    if (!(stepG > 0)) throw new IllegalArgumentException("stepG must be > 0");
    if (!(minG > 0) || minG > maxG) {
      throw new IllegalArgumentException("require 0 < minG <= maxG: " + minG + ", " + maxG);
    }
    if (!isMultiple(minG, stepG) || !isMultiple(maxG, stepG) || !isMultiple(defaultG, stepG)) {
      throw new IllegalArgumentException("minG, maxG and defaultG must be multiples of stepG");
    }
    if (defaultG < minG || defaultG > maxG) {
      throw new IllegalArgumentException("defaultG must lie within [minG, maxG]");
    }
  }

  /** Rounds to the nearest step (half-up), then clamps into bounds. Always a legal portion. */
  public double normalize(double grams) {
    double stepped = Math.floor(grams / stepG + 0.5) * stepG;
    return Math.min(maxG, Math.max(minG, stepped));
  }

  public boolean allows(double grams) {
    return grams >= minG - EPS && grams <= maxG + EPS && isMultiple(grams, stepG);
  }

  private static boolean isMultiple(double value, double step) {
    double q = value / step;
    return Math.abs(q - Math.rint(q)) < 1e-6;
  }
}
