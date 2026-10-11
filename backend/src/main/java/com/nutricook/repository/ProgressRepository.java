package com.nutricook.repository;

import com.nutricook.entity.ProgressEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProgressRepository extends JpaRepository<ProgressEntry, Long> {
  Optional<ProgressEntry> findByUserIdAndDate(Long userId, LocalDate date);

  List<ProgressEntry> findByUserIdAndDateBetweenOrderByDateAsc(
      Long userId, LocalDate from, LocalDate to);

  long countByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to);

  /** The first weigh-in ever, the reference of weight milestones (BR-ACH-05). */
  Optional<ProgressEntry> findFirstByUserIdAndWeightKgIsNotNullOrderByDateAsc(Long userId);

  /** The most recent day, other than {@code date}, with a weight logged (BR-PRG-04). */
  @Query(
      "SELECT MAX(e.date) FROM ProgressEntry e"
          + " WHERE e.user.id = :userId AND e.weightKg IS NOT NULL AND e.date <> :date")
  LocalDate latestWeighInExcluding(Long userId, LocalDate date);
}
