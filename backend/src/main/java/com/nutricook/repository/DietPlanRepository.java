package com.nutricook.repository;

import com.nutricook.entity.DietPlan;
import com.nutricook.entity.enums.PlanStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DietPlanRepository extends JpaRepository<DietPlan, Long> {
  List<DietPlan> findByUserIdOrderByGeneratedAtDesc(Long userId);

  List<DietPlan> findByUserIdAndStatus(Long userId, PlanStatus status);

  Optional<DietPlan> findByUserIdAndValidForDateAndStatus(
      Long userId, LocalDate date, PlanStatus status);

  @Modifying
  @Query(
      "UPDATE DietPlan d SET d.status = 'ARCHIVED' WHERE d.user.id = :userId AND d.validForDate = :date AND d.status = 'ACTIVE'")
  void archiveActiveForDate(Long userId, LocalDate date);
}
