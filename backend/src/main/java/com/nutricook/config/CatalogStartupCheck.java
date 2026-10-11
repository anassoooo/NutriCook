package com.nutricook.config;

import com.nutricook.entity.FoodItem;
import com.nutricook.repository.FoodItemRepository;
import com.nutricook.service.CatalogAdminService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refuses to start when an active food fails the quality gate (BR-CAT-02). The admin API cannot
 * save such a food, so this catches a bad seed or a manual database edit before any plan uses it.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class CatalogStartupCheck implements ApplicationRunner {

  private final FoodItemRepository foods;

  @Override
  @Transactional(readOnly = true)
  public void run(ApplicationArguments args) {
    List<FoodItem> active = foods.findByActiveTrue();
    Map<String, List<String>> failures = new LinkedHashMap<>();
    for (FoodItem food : active) {
      List<String> problems = CatalogAdminService.problemsOf(food);
      if (!problems.isEmpty()) failures.put(food.getName(), problems);
    }
    if (!failures.isEmpty()) {
      throw new IllegalStateException(
          "Active foods fail the catalog quality gate (BR-CAT-02); fix or deactivate them: "
              + failures);
    }
    log.info("Catalog check: {} active foods pass the quality gate", active.size());
  }
}
