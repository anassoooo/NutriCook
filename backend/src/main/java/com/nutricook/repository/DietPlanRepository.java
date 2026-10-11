package com.nutricook.repository;

import com.nutricook.domain.model.PlanStatus;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.enums.AiStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DietPlanRepository extends JpaRepository<DietPlan, Long> {

  @Query(
      "SELECT DISTINCT p FROM DietPlan p LEFT JOIN FETCH p.meals WHERE p.user.id = :userId ORDER BY p.generatedAt DESC")
  List<DietPlan> findByUserIdOrderByGeneratedAtDesc(Long userId);

  List<DietPlan> findByUserIdAndStatus(Long userId, PlanStatus status);

  Optional<DietPlan> findByUserIdAndValidForDateAndStatus(
      Long userId, LocalDate date, PlanStatus status);

  @Query("SELECT p FROM DietPlan p LEFT JOIN FETCH p.meals WHERE p.id = :id")
  Optional<DietPlan> findByIdWithMeals(Long id);

  List<DietPlan> findByUserIdAndValidForDate(Long userId, LocalDate date);

  /** Plans already generated for this user-day: the next generation sequence (BR-CMP-07). */
  long countByUserIdAndValidForDate(Long userId, LocalDate date);

  /** Generations since a moment, for the daily quota (BR-LIF-06). */
  long countByUserIdAndGeneratedAtGreaterThanEqual(Long userId, LocalDateTime since);

  /**
   * Writes only the AI columns. The AI text arrives asynchronously; saving the whole row would undo
   * a meal check-off or a swap made in the meantime (lost update).
   */
  @Modifying
  @Query("UPDATE DietPlan p SET p.aiExplanation = :text, p.aiStatus = :status WHERE p.id = :planId")
  int updateAiText(Long planId, String text, AiStatus status);

  /** The tip of one meal of the plan; see {@link #updateAiText}. */
  @Modifying
  @Query("UPDATE Meal m SET m.aiSuggestion = :tip WHERE m.id = :mealId AND m.dietPlan.id = :planId")
  int updateMealTip(Long planId, Long mealId, String tip);
}
