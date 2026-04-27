package com.nutricook.repository;

import com.nutricook.entity.ProgressEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressRepository extends JpaRepository<ProgressEntry, Long> {
  Optional<ProgressEntry> findByUserIdAndDate(Long userId, LocalDate date);

  List<ProgressEntry> findByUserIdAndDateBetweenOrderByDateAsc(
      Long userId, LocalDate from, LocalDate to);

  long countByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to);
}
