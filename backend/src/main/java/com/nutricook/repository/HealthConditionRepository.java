package com.nutricook.repository;

import com.nutricook.entity.HealthCondition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthConditionRepository extends JpaRepository<HealthCondition, Long> {
  Optional<HealthCondition> findByName(String name);
}
