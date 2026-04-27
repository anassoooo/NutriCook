package com.nutricook.repository;

import com.nutricook.entity.DietaryRestriction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DietaryRestrictionRepository extends JpaRepository<DietaryRestriction, Long> {
  Optional<DietaryRestriction> findByName(String name);
}
