package com.nutricook.service;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /** Validates the whole plan, then saves it. Nothing unsafe is ever persisted. */
  @Transactional
  public DietPlan persist(DietPlan plan, SafetyConstraints constraints) {
    List<Violation> violations =
        PlanSafetyValidator.validate(FoodMapper.toDraft(plan), constraints);
    if (!violations.isEmpty()) {
      throw SafetyService.infeasible("This plan breaks a safety rule.", violations.get(0));
    }
    DietPlan saved = plans.saveAndFlush(plan);
    events.publishEvent(new DayPlanChangedEvent(userIdOf(plan), plan.getValidForDate()));
    return saved;
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
    plans.saveAndFlush(plan);
    events.publishEvent(new DayPlanChangedEvent(userIdOf(plan), plan.getValidForDate()));
  }

  /**
   * Checks a meal off, or unchecks it (BR-PRG-05). Only the ACTIVE plan of a day records what was
   * eaten; the caller checks the date window. No food changes, so no safety validation is needed.
   */
  @Transactional
  public void setMealCompleted(DietPlan plan, Meal meal, boolean completed) {
    if (plan.getStatus() != PlanStatus.ACTIVE) {
      throw new DomainException(
          ErrorCode.PLAN_READ_ONLY,
          "Only the current plan of a day can be checked off.",
          Map.of("status", plan.getStatus().name()));
    }
    if (meal.isCompleted() == completed) return;
    meal.setCompleted(completed);
    meal.setCompletedAt(completed ? LocalDateTime.now(clock) : null);
    plans.saveAndFlush(plan);
  }

  private static Long userIdOf(DietPlan plan) {
    return plan.getUser().getId();
  }

  /**
   * Stores the optional AI text in its own transaction, after the plan was committed (BR-LIF-07).
   * Touches no nutrition data, so no safety validation is needed. Only the AI columns are written,
   * so a check-off or swap made while the AI was answering is kept.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveAiText(
      Long planId, String explanation, Map<Long, String> mealTips, AiStatus status) {
    if (plans.updateAiText(planId, explanation, status) == 0) {
      throw new ResourceNotFoundException("Plan not found: " + planId);
    }
    mealTips.forEach((mealId, tip) -> plans.updateMealTip(planId, mealId, tip));
  }
}
