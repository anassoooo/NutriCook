package com.nutricook.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutricook.domain.model.Goal;
import com.nutricook.domain.model.Slot;
import com.nutricook.domain.rules.NutritionRules;
import com.nutricook.domain.rules.TrackingRules;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Configuration binding of the rule tunables (business-rules.md §13). */
class DomainConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
          .withUserConfiguration(DomainConfig.class);

  @Test
  void defaultsInApplicationYmlMatchTheCatalogue() {
    runner.run(
        ctx -> {
          assertThat(ctx).hasNotFailed().hasSingleBean(Clock.class);
          NutritionRules r = ctx.getBean(NutritionRules.class);
          assertThat(r.goalAdjustment().lose()).isEqualTo(-500);
          assertThat(r.goalAdjustment().gain()).isEqualTo(300);
          assertThat(r.minKcal().male()).isEqualTo(1500);
          assertThat(r.minKcal().other()).isEqualTo(1200);
          assertThat(r.proteinPerKg(Goal.LOSE_WEIGHT)).isEqualTo(1.8);
          assertThat(r.proteinPerKg(Goal.GAIN_MUSCLE)).isEqualTo(2.0);
          assertThat(r.proteinPerKg(Goal.MAINTAIN_WEIGHT)).isEqualTo(1.2);
          assertThat(r.proteinPerKg(Goal.IMPROVE_HEALTH)).isEqualTo(1.0);
          assertThat(r.fatEnergyShare()).isEqualTo(0.30);
          assertThat(r.share(Slot.LUNCH)).isEqualTo(0.30);
          assertThat(r.tolerance().dayOk()).isEqualTo(0.05);
          assertThat(r.tolerance().dayMax()).isEqualTo(0.10);
          assertThat(r.tolerance().meal()).isEqualTo(0.15);

          TrackingRules t = ctx.getBean(TrackingRules.class);
          assertThat(t.backfillDays()).isEqualTo(30);
          assertThat(t.hydrationDayMl()).isEqualTo(2000);

          LimitsProperties l = ctx.getBean(LimitsProperties.class);
          assertThat(l.generationsPerDay()).isEqualTo(10);
          assertThat(l.chatPerHour()).isEqualTo(30);
          assertThat(l.aiTimeout()).isEqualTo(Duration.ofSeconds(10));
        });
  }

  @Test
  void BR_CMP_01_slotSharesMustSumToOne() {
    runner
        .withPropertyValues("app.nutrition.slot-shares.BREAKFAST=0.15") // total becomes 0.90
        .run(
            ctx ->
                assertThat(ctx)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("slot-shares must sum to 1.0"));
  }

  @Test
  void BR_CMP_01_everySlotNeedsAShare() {
    Map<Slot, Double> shares = new EnumMap<>(Slot.class);
    shares.put(Slot.BREAKFAST, 0.35);
    shares.put(Slot.LUNCH, 0.35);
    shares.put(Slot.DINNER, 0.30); // sums to 1, but the snacks are missing

    assertThatThrownBy(() -> rulesWith(shares))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must define every slot");
  }

  @Test
  void fatShareOutsideAmdrStopsStartup() {
    runner
        .withPropertyValues("app.nutrition.fat-energy-share=0.5")
        .run(
            ctx ->
                assertThat(ctx)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("fat-energy-share"));
  }

  @Test
  void invalidLimitStopsStartup() {
    runner.withPropertyValues("app.limits.chat-per-hour=0").run(ctx -> assertThat(ctx).hasFailed());
  }

  private static NutritionRules rulesWith(Map<Slot, Double> shares) {
    return new NutritionRules(
        new NutritionRules.GoalAdjustment(-500, 300),
        new NutritionRules.MinKcal(1500, 1200),
        new NutritionRules.ProteinPerKg(1.8, 2.0, 1.2, 1.0),
        2.2,
        0.35,
        0.30,
        0.6,
        14,
        shares,
        new NutritionRules.Tolerance(0.05, 0.10, 0.15),
        50);
  }
}
