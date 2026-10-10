package com.nutricook.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PortionRuleTest {

  private final PortionRule rule = new PortionRule(50, 250, 5, 150);

  @ParameterizedTest(name = "{0} g -> {1} g")
  @CsvSource({
    "152.4, 150", // rounds to nearest step
    "152.5, 155", // half-up
    "10,    50", // clamped to min
    "999,   250", // clamped to max
    "250,   250"
  })
  void BR_CMP_03_normalizeRoundsToStepThenClamps(double in, double expected) {
    assertThat(rule.normalize(in)).isEqualTo(expected);
    assertThat(rule.allows(rule.normalize(in))).isTrue();
  }

  @Test
  void BR_CMP_03_allowsOnlyStepMultiplesWithinBounds() {
    assertThat(rule.allows(155)).isTrue();
    assertThat(rule.allows(152)).isFalse();
    assertThat(rule.allows(45)).isFalse();
    assertThat(rule.allows(255)).isFalse();
  }

  @Test
  void invalidRulesAreRejected() {
    assertThatThrownBy(() -> new PortionRule(0, 100, 5, 50))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PortionRule(100, 50, 5, 75))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PortionRule(52, 100, 5, 75))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PortionRule(50, 100, 5, 120))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
