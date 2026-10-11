package com.nutricook.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutricook.domain.fixtures.TestRules;
import com.nutricook.domain.model.Goal;
import com.nutricook.domain.model.Slot;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The tunables of business-rules.md §13 refuse values outside their allowed ranges, so a bad
 * configuration stops the application at startup instead of producing unsafe plans.
 */
class RulesValidationTest {

  /** The configured defaults, as mutable arguments. */
  private static final class Args {
    NutritionRules.GoalAdjustment goal = new NutritionRules.GoalAdjustment(-500, 300);
    NutritionRules.MinKcal minKcal = new NutritionRules.MinKcal(1500, 1200);
    NutritionRules.ProteinPerKg protein = new NutritionRules.ProteinPerKg(1.8, 2.0, 1.2, 1.0);
    double proteinMaxPerKg = 2.2;
    double proteinMaxShare = 0.35;
    double fatShare = 0.30;
    double fatMinPerKg = 0.6;
    double fiber = 14;
    Map<Slot, Double> shares = defaultShares();
    NutritionRules.Tolerance tolerance = new NutritionRules.Tolerance(0.05, 0.10, 0.15);
    int attempts = 50;

    NutritionRules build() {
      return new NutritionRules(
          goal,
          minKcal,
          protein,
          proteinMaxPerKg,
          proteinMaxShare,
          fatShare,
          fatMinPerKg,
          fiber,
          shares,
          tolerance,
          attempts);
    }
  }

  private static Map<Slot, Double> defaultShares() {
    Map<Slot, Double> shares = new EnumMap<>(Slot.class);
    shares.put(Slot.BREAKFAST, 0.25);
    shares.put(Slot.MORNING_SNACK, 0.10);
    shares.put(Slot.LUNCH, 0.30);
    shares.put(Slot.AFTERNOON_SNACK, 0.10);
    shares.put(Slot.DINNER, 0.25);
    return shares;
  }

  private static void rejects(Consumer<Args> change, String message) {
    Args args = new Args();
    change.accept(args);
    assertThatThrownBy(args::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(message);
  }

  @Test
  void theConfiguredDefaultsAreValid() {
    assertThatCode(() -> new Args().build()).doesNotThrowAnyException();
    NutritionRules rules = TestRules.nutrition();
    assertThat(rules.share(Slot.LUNCH)).isEqualTo(0.30);
    assertThat(rules.proteinPerKg(Goal.LOSE_WEIGHT)).isEqualTo(1.8);
    assertThat(rules.proteinPerKg(Goal.GAIN_MUSCLE)).isEqualTo(2.0);
    assertThat(rules.proteinPerKg(Goal.MAINTAIN_WEIGHT)).isEqualTo(1.2);
    assertThat(rules.proteinPerKg(Goal.IMPROVE_HEALTH)).isEqualTo(1.0);
  }

  @Test
  void BR_CMP_01_slotSharesCoverEverySlotArePositiveAndSumToOne() {
    rejects(a -> a.shares = null, "slot-shares is required");
    rejects(a -> a.shares.remove(Slot.DINNER), "must define every slot");
    rejects(
        a -> {
          a.shares.put(Slot.LUNCH, 0.0);
          a.shares.put(Slot.DINNER, 0.55);
        },
        "every slot share must be > 0");
    rejects(a -> a.shares.put(Slot.LUNCH, 0.35), "must sum to 1.0");
  }

  @Test
  void BR_MAC_02_03_05_macroTunablesStayInSafeRanges() {
    rejects(a -> a.proteinMaxPerKg = 0.9, "protein-max-per-kg");
    rejects(a -> a.proteinMaxPerKg = 3.1, "protein-max-per-kg");
    rejects(a -> a.proteinMaxShare = 0.10, "protein-max-energy-share");
    rejects(a -> a.proteinMaxShare = 0.41, "protein-max-energy-share");
    rejects(a -> a.fatShare = 0.19, "fat-energy-share");
    rejects(a -> a.fatShare = 0.36, "fat-energy-share");
    rejects(a -> a.fatMinPerKg = 0, "fat-min-per-kg");
    rejects(a -> a.fatMinPerKg = 1.6, "fat-min-per-kg");
    rejects(a -> a.fiber = 0, "fiber-per-1000-kcal");
    rejects(a -> a.fiber = 31, "fiber-per-1000-kcal");
  }

  @Test
  void BR_NRG_03_04_energyTunablesStayInSafeRanges() {
    assertThatThrownBy(() -> new NutritionRules.GoalAdjustment(100, 300))
        .hasMessageContaining("goal-adjustment.lose");
    assertThatThrownBy(() -> new NutritionRules.GoalAdjustment(-1001, 300))
        .hasMessageContaining("goal-adjustment.lose");
    assertThatThrownBy(() -> new NutritionRules.GoalAdjustment(-500, -1))
        .hasMessageContaining("goal-adjustment.gain");
    assertThatThrownBy(() -> new NutritionRules.GoalAdjustment(-500, 1001))
        .hasMessageContaining("goal-adjustment.gain");
    assertThatThrownBy(() -> new NutritionRules.MinKcal(999, 1200))
        .hasMessageContaining("min-kcal");
    assertThatThrownBy(() -> new NutritionRules.MinKcal(1500, 999))
        .hasMessageContaining("min-kcal");
    assertThatThrownBy(() -> new NutritionRules.ProteinPerKg(0.7, 2.0, 1.2, 1.0))
        .hasMessageContaining("protein-per-kg");
    assertThatThrownBy(() -> new NutritionRules.ProteinPerKg(1.8, 2.3, 1.2, 1.0))
        .hasMessageContaining("protein-per-kg");
  }

  @Test
  void BR_CMP_05_tolerancesAreOrderedAndBelowHalf() {
    assertThatThrownBy(() -> new NutritionRules.Tolerance(0, 0.10, 0.15))
        .hasMessageContaining("day-ok");
    assertThatThrownBy(() -> new NutritionRules.Tolerance(0.12, 0.10, 0.15))
        .hasMessageContaining("day-ok <= day-max");
    assertThatThrownBy(() -> new NutritionRules.Tolerance(0.05, 0.5, 0.15))
        .hasMessageContaining("day-max < 0.5");
    assertThatThrownBy(() -> new NutritionRules.Tolerance(0.05, 0.10, 0))
        .hasMessageContaining("tolerance.meal");
    assertThatThrownBy(() -> new NutritionRules.Tolerance(0.05, 0.10, 0.5))
        .hasMessageContaining("tolerance.meal");
    rejects(a -> a.attempts = 0, "composer-max-attempts");
  }

  @Test
  void requiredGroupsMustBePresent() {
    rejects(a -> a.goal = null, "goal-adjustment is required");
    rejects(a -> a.minKcal = null, "min-kcal is required");
    rejects(a -> a.protein = null, "protein-per-kg is required");
    rejects(a -> a.tolerance = null, "tolerance is required");
  }

  @ParameterizedTest(name = "backfill={0}, hydration={1}: valid={2}")
  @CsvSource({
    "30,  2000, true",
    "0,   500,  true",
    "365, 6000, true",
    "-1,  2000, false",
    "366, 2000, false",
    "30,  499,  false",
    "30,  6001, false"
  })
  void BR_PRG_02_BR_ACH_04_trackingTunablesStayInRange(int backfill, int hydration, boolean valid) {
    if (valid) {
      assertThatCode(() -> new TrackingRules(backfill, hydration)).doesNotThrowAnyException();
    } else {
      assertThatThrownBy(() -> new TrackingRules(backfill, hydration))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("app.tracking");
    }
  }
}
