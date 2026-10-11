package com.nutricook.service;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.safety.ConditionRule;
import com.nutricook.domain.safety.EligibilityPolicy;
import com.nutricook.domain.safety.RestrictionRule;
import com.nutricook.domain.safety.SafetyConstraints;
import com.nutricook.domain.safety.Violation;
import com.nutricook.entity.FoodItem;
import com.nutricook.entity.UserProfile;
import com.nutricook.repository.FoodItemRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges persisted restrictions, conditions and foods to the domain safety rules (BR-SAF). Every
 * food offered to a user, by the generator or the AI, comes from {@link #eligibleFoods}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafetyService {

  private final FoodItemRepository foodRepository;

  public SafetyConstraints constraintsFor(UserProfile profile) {
    List<RestrictionRule> restrictions =
        profile.getDietaryRestrictions().stream()
            .map(
                r ->
                    new RestrictionRule(
                        r.getName(), r.getExcludedAllergens(), r.getRequiredDietFlag()))
            .toList();
    List<ConditionRule> conditions =
        profile.getHealthConditions().stream()
            .map(
                c ->
                    new ConditionRule(
                        c.getName(),
                        c.getSugarLimitPerMealG(),
                        c.getFatLimitPerMealG(),
                        c.getSodiumLimitPerDayMg(),
                        c.getImpliedAllergenExclusions()))
            .toList();
    return SafetyConstraints.from(restrictions, conditions);
  }

  /** Active catalog foods that pass every hard constraint (BR-SAF-01..03, BR-CAT-04). */
  public List<FoodItem> eligibleFoods(SafetyConstraints c) {
    return foodRepository.findByActiveTrue().stream()
        .filter(f -> EligibilityPolicy.allows(FoodMapper.toView(f), c))
        .toList();
  }

  /**
   * The error for a plan that cannot be made safe (BR-SAF-04). Never relax a constraint instead.
   */
  static DomainException infeasible(String message, Violation cause) {
    return new DomainException(
        ErrorCode.PLAN_INFEASIBLE,
        message,
        cause == null
            ? Map.of()
            : Map.of("constraint", cause.constraint(), "reason", cause.message()));
  }
}
