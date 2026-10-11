package com.nutricook.service;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.entity.*;
import com.nutricook.exception.ResourceNotFoundException;
import com.nutricook.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plan reads, archiving and the disclaimer. Generation lives in {@link PlanGenerationService},
 * swaps in {@link SwapService}, chat in {@link ChatService}; every write goes through {@link
 * PlanCommandService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DietPlanService {

  public static final String DISCLAIMER = "NOT_MEDICAL_ADVICE";

  private final DietPlanRepository planRepository;
  private final UserProfileService profileService;
  private final PlanCommandService commands;

  @Transactional(readOnly = true)
  public DietPlan getByIdForUser(Long userId, Long planId) {
    DietPlan plan =
        planRepository
            .findByIdWithMeals(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
    if (!plan.getUser().getId().equals(userId)) {
      throw new DomainException(ErrorCode.FORBIDDEN, "This plan belongs to another user.");
    }
    return loaded(plan);
  }

  @Transactional(readOnly = true)
  public List<DietPlan> listForUser(Long userId) {
    List<DietPlan> plans = planRepository.findByUserIdOrderByGeneratedAtDesc(userId);
    plans.forEach(DietPlanService::loaded);
    return plans;
  }

  /** Meals and their components, so responses can be built after the transaction. */
  private static DietPlan loaded(DietPlan plan) {
    plan.getMeals().forEach(m -> Hibernate.initialize(m.getMealFoodItems()));
    return plan;
  }

  /** BR-SAF-08: health-condition users always see the disclaimer. */
  public String disclaimerFor(Long userId) {
    return profileService.getByUserId(userId).getHealthConditions().isEmpty() ? null : DISCLAIMER;
  }

  @Transactional
  public void archive(Long userId, Long planId) {
    commands.archive(getByIdForUser(userId, planId));
  }
}
