package com.nutricook.service;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.Slot;
import com.nutricook.domain.plan.PlanLifecycle;
import com.nutricook.domain.plan.SwapPlanner;
import com.nutricook.domain.safety.SafetyConstraints;
import com.nutricook.dto.response.AlternativesResponse;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.FoodItem;
import com.nutricook.entity.Meal;
import com.nutricook.entity.MealFoodItem;
import com.nutricook.entity.UserProfile;
import com.nutricook.exception.ResourceNotFoundException;
import com.nutricook.repository.FoodItemRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Swapping one food in a meal: the single path for manual swaps and AI swaps (BR-AI-03, FR-003).
 * Checks ownership, that the plan may still change (BR-LIF-04), then lets {@link SwapPlanner}
 * decide, and saves through the safety gate (BR-SAF-05).
 */
@Service
@RequiredArgsConstructor
public class SwapService {

  /** Alternatives offered per role. */
  static final int ALTERNATIVES_PER_ROLE = 8;

  private final DietPlanService plans;
  private final UserProfileService profiles;
  private final SafetyService safety;
  private final PlanCommandService commands;
  private final FoodItemRepository foods;
  private final UserClock userClock;

  /**
   * @throws DomainException PLAN_READ_ONLY, SWAP_REJECTED (with {@code reason}), NOT_FOUND
   */
  @Transactional
  public DietPlan swap(Long userId, Long planId, Long mealId, Long foodId) {
    DietPlan plan = plans.getByIdForUser(userId, planId);
    UserProfile profile = profiles.getByUserId(userId);
    PlanLifecycle.assertMutable(
        plan.getStatus(), plan.getValidForDate(), userClock.today(profile.getTimezone()));

    Meal meal = mealOf(plan, mealId);
    FoodItem food =
        foods
            .findById(foodId)
            .filter(FoodItem::isActive)
            .orElseThrow(() -> new ResourceNotFoundException("Food not found: " + foodId));
    SafetyConstraints constraints = safety.constraintsFor(profile);
    Slot slot = Slot.valueOf(meal.getMealType().name());

    SwapPlanner.Result result =
        SwapPlanner.swap(FoodMapper.toDraft(plan), slot, FoodMapper.toView(food), constraints);
    if (result instanceof SwapPlanner.Rejected rejected) {
      throw new DomainException(
          ErrorCode.SWAP_REJECTED, rejected.message(), Map.of("reason", rejected.reason().name()));
    }
    SwapPlanner.Swapped swapped = (SwapPlanner.Swapped) result;
    apply(plan, meal, swapped, food);
    return commands.persist(plan, constraints);
  }

  /** Eligible replacements for each food in the meal, closest in energy first (FR-003). */
  @Transactional(readOnly = true)
  public List<AlternativesResponse> alternatives(Long userId, Long planId, Long mealId) {
    DietPlan plan = plans.getByIdForUser(userId, planId);
    UserProfile profile = profiles.getByUserId(userId);
    Meal meal = mealOf(plan, mealId);
    Slot slot = Slot.valueOf(meal.getMealType().name());
    SafetyConstraints constraints = safety.constraintsFor(profile);
    List<FoodView> catalog =
        safety.eligibleFoods(constraints).stream().map(FoodMapper::toView).toList();

    Map<FoodRole, List<AlternativesResponse.Option>> byRole = new EnumMap<>(FoodRole.class);
    for (MealFoodItem item : meal.getMealFoodItems()) {
      List<Component> options =
          SwapPlanner.alternatives(
              FoodMapper.toDraft(plan),
              slot,
              item.getRole(),
              catalog,
              constraints,
              ALTERNATIVES_PER_ROLE);
      byRole.put(item.getRole(), options.stream().map(AlternativesResponse.Option::of).toList());
    }
    return byRole.entrySet().stream()
        .map(e -> new AlternativesResponse(e.getKey(), e.getValue()))
        .toList();
  }

  private static Meal mealOf(DietPlan plan, Long mealId) {
    return plan.getMeals().stream()
        .filter(m -> m.getId().equals(mealId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Meal not found: " + mealId));
  }

  /** Mirrors the swapped draft onto the entities: one component replaced, totals recomputed. */
  private static void apply(DietPlan plan, Meal meal, SwapPlanner.Swapped swapped, FoodItem food) {
    MealDraft newMeal = swapped.meal();
    Component replacement =
        newMeal.components().stream()
            .filter(c -> c.food().id() == food.getId())
            .findFirst()
            .orElseThrow();
    List<MealFoodItem> items = new ArrayList<>(meal.getMealFoodItems());
    MealFoodItem old =
        items.stream().filter(i -> i.getRole() == replacement.role()).findFirst().orElseThrow();
    MealFoodItem item = PlanGenerationService.snapshot(replacement, food);
    item.setMeal(meal);
    meal.getMealFoodItems().remove(old);
    meal.getMealFoodItems().add(item);

    Nutrients n = newMeal.nutrients();
    meal.setCalories((int) n.kcal());
    meal.setProteinG(n.proteinG());
    meal.setCarbsG(n.carbsG());
    meal.setFatG(n.fatG());
    meal.setSugarG(n.sugarG());
    meal.setFiberG(n.fiberG());
    meal.setSodiumMg(n.sodiumMg());
    meal.setAiSuggestion(null); // the tip described the old meal

    Nutrients t = swapped.plan().totals();
    plan.setTotalCalories((int) t.kcal());
    plan.setTotalProteinG(t.proteinG());
    plan.setTotalCarbsG(t.carbsG());
    plan.setTotalFatG(t.fatG());
    plan.setTotalSugarG(t.sugarG());
    plan.setTotalFiberG(t.fiberG());
    plan.setTotalSodiumMg(t.sodiumMg());
  }
}
