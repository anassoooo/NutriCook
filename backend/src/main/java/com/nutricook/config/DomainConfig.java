package com.nutricook.config;

import com.nutricook.domain.energy.TargetCalculator;
import com.nutricook.domain.plan.PlanComposer;
import com.nutricook.domain.rules.NutritionRules;
import com.nutricook.domain.rules.TrackingRules;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Wires the framework-free domain: binds the rule records from configuration (they validate
 * themselves, so bad values fail startup) and provides the single source of time (BR-TIM-03).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LimitsProperties.class)
public class DomainConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  NutritionRules nutritionRules(Environment env) {
    return Binder.get(env)
        .bind("app.nutrition", NutritionRules.class)
        .orElseThrow(() -> new IllegalStateException("Missing configuration: app.nutrition"));
  }

  @Bean
  TargetCalculator targetCalculator(NutritionRules rules) {
    return new TargetCalculator(rules);
  }

  @Bean
  PlanComposer planComposer(NutritionRules rules) {
    return new PlanComposer(rules);
  }

  @Bean
  TrackingRules trackingRules(Environment env) {
    return Binder.get(env)
        .bind("app.tracking", TrackingRules.class)
        .orElseThrow(() -> new IllegalStateException("Missing configuration: app.tracking"));
  }
}
