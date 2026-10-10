package com.nutricook.service;

import com.nutricook.config.LimitsProperties;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.Meal;
import com.nutricook.entity.UserProfile;
import com.nutricook.entity.enums.AiStatus;
import com.nutricook.repository.DietPlanRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Adds the AI explanation and meal tips after the plan is committed (BR-LIF-07). Each call has a
 * timeout; any failure leaves the plan intact with {@code aiStatus = UNAVAILABLE} (BR-AI-05).
 */
@Service
@Slf4j
public class PlanEnrichmentService {

  private final DietPlanRepository plans;
  private final UserProfileService profiles;
  private final GroqAiService ai;
  private final PlanCommandService commands;
  private final LimitsProperties limits;
  private final Executor aiCalls;

  public PlanEnrichmentService(
      DietPlanRepository plans,
      UserProfileService profiles,
      GroqAiService ai,
      PlanCommandService commands,
      LimitsProperties limits,
      @Qualifier("aiCallExecutor") Executor aiCalls) {
    this.plans = plans;
    this.profiles = profiles;
    this.ai = ai;
    this.commands = commands;
    this.limits = limits;
    this.aiCalls = aiCalls;
  }

  @Async("aiEnrichmentExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onPlanGenerated(PlanGeneratedEvent event) {
    enrich(event.planId(), event.userId());
  }

  void enrich(Long planId, Long userId) {
    DietPlan plan = plans.findByIdWithMeals(planId).orElse(null);
    if (plan == null) return;
    UserProfile profile = profiles.getByUserId(userId);

    String explanation = withTimeout(() -> ai.getPlanExplanation(plan, profile));
    Map<Long, String> tips = new HashMap<>();
    if (explanation != null) {
      for (Meal meal : plan.getMeals()) {
        String tip = withTimeout(() -> ai.getMealSuggestion(meal, profile));
        if (tip != null) tips.put(meal.getId(), tip);
      }
    }
    AiStatus status = explanation != null ? AiStatus.READY : AiStatus.UNAVAILABLE;
    commands.saveAiText(planId, explanation, tips, status);
    log.info("AI enrichment for plan {}: {} ({} meal tips)", planId, status, tips.size());
  }

  /** Null on timeout or failure; the call is abandoned, never waited on longer (BR-AI-05). */
  private String withTimeout(Supplier<String> call) {
    try {
      return CompletableFuture.supplyAsync(call, aiCalls)
          .get(limits.aiTimeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (Exception e) {
      log.warn("AI call failed or timed out: {}", e.getClass().getSimpleName());
      return null;
    }
  }
}
