package com.nutricook.repository;

import com.nutricook.entity.FoodItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
  List<FoodItem> findByCategory(String category);
}
