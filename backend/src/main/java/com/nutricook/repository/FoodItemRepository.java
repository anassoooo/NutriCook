package com.nutricook.repository;

import com.nutricook.entity.FoodItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FoodItemRepository
    extends JpaRepository<FoodItem, Long>, JpaSpecificationExecutor<FoodItem> {
  List<FoodItem> findByCategory(String category);

  /** Foods that may be offered for new plans (BR-CAT-04). */
  List<FoodItem> findByActiveTrue();

  /** Names are unique regardless of case (BR-CAT-01). */
  boolean existsByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
