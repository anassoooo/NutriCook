package com.nutricook.service;

import com.nutricook.domain.model.PlanStatus;
import com.nutricook.domain.plan.PlanLifecycle;
import com.nutricook.domain.safety.PlanSafetyValidator;
import com.nutricook.domain.safety.SafetyConstraints;
import com.nutricook.domain.safety.Violation;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.Meal;
import com.nutricook.entity.enums.AiStatus;
import com.nutricook.exception.ResourceNotFoundException;
import com.nutricook.repository.DietPlanRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only class that writes plans (plan D3, enforced by ArchitectureTest). Anything that changes a
 * plan's food goes through {@link #persist}, which runs the safety validator first (BR-SAF-05);
 * state changes go through the lifecycle state machine (BR-LIF-01).
 */
@Service
@RequiredArgsConstructor
public class PlanCommandService {

  private final DietPlanRepository plans;

  /** Validates the whole plan, then saves it. Nothing unsafe is ever persisted. */
  @Transactional
  public DietPlan persist(DietPlan plan, SafetyConstraints constraints) {
    List<Violation> violations =
        PlanSafetyValidator.validate(FoodMapper.toDraft(plan), constraints);
    if (!violations.isEmpty()) {
      throw SafetyService.infeasible("This plan breaks a safety rule.", violations.get(0));
    }
    return plans.saveAndFlush(plan);
  }

  /** ACTIVE → SUPERSEDED when the day is regenerated (BR-LIF-02). Flushed before the new insert. */
  @Transactional
  public void supersede(DietPlan plan) {
    plan.setStatus(PlanLifecycle.transition(plan.getStatus(), PlanStatus.SUPERSEDED));
    plans.saveAndFlush(plan);
  }

  @Transactional
  public void archive(DietPlan plan) {
    plan.setStatus(PlanLifecycle.transition(plan.getStatus(), PlanStatus.ARCHIVED));
    plans.save(plan);
  }

  /**
   * Stores the optional AI text in its own transaction, after the plan was committed (BR-LIF-07).
   * Touches no nutrition data, so no safety validation is needed.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveAiText(
      Long planId, String explanation, Map<Long, String> mealTips, AiStatus status) {
    DietPlan plan =
        plans
            .findByIdWithMeals(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
    plan.setAiExplanation(explanation);
    for (Meal meal : plan.getMeals()) {
      if (mealTips.containsKey(meal.getId())) meal.setAiSuggestion(mealTips.get(meal.getId()));
    }
    plan.setAiStatus(status);
    plans.save(plan);
  }
}
