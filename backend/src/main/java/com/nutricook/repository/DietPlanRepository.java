package com.nutricook.repository;

import com.nutricook.entity.DietPlan;
import com.nutricook.entity.enums.PlanStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
