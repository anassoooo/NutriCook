package com.nutricook.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.catalog.FoodQualityGate;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.entity.FoodItem;
import com.nutricook.repository.FoodItemRepository;
import com.nutricook.support.AbstractIntegrationTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * The seeded catalog, as Flyway actually applied it to PostgreSQL. Every failure names the food.
 */
class CatalogQualityIT extends AbstractIntegrationTest {

  @Autowired FoodItemRepository foods;

  private List<FoodItem> active;

  @BeforeEach
  void load() {
    active = foods.findByActiveTrue();
  }

  private static Nutrients per100g(FoodItem f) {
    return new Nutrients(
        f.getCaloriesPer100g(),
        f.getProteinPer100g(),
        f.getCarbsPer100g(),
        f.getFatPer100g(),
        f.getSugarPer100g(),
        f.getFiberPer100g(),
        f.getSodiumMgPer100g());
  }

  @Test
  void BR_CAT_02_everyActiveFoodPassesTheQualityGate() {
    Map<String, List<String>> failures = new LinkedHashMap<>();
    for (FoodItem f : active) {
      List<String> problems = FoodQualityGate.problems(per100g(f));
      if (!problems.isEmpty()) failures.put(f.getName(), problems);
    }
    assertThat(failures).as("foods failing BR-CAT-02").isEmpty();
  }

  @Test
  void BR_CAT_02_everyActiveFoodPassesTheWholeGate() {
    Map<String, List<String>> failures = new LinkedHashMap<>();
    for (FoodItem f : active) {
      List<String> problems = com.nutricook.service.CatalogAdminService.problemsOf(f);
      if (!problems.isEmpty()) failures.put(f.getName(), problems);
    }
    assertThat(failures).as("foods failing values, labels, slots or source").isEmpty();
  }

  @Test
  void BR_SAF_03_everyActiveFoodHasVerifiedAllergens() {
    assertThat(active)
        .filteredOn(f -> !f.isAllergensKnown())
        .extracting(FoodItem::getName)
        .as("active foods with unverified allergens")
        .isEmpty();
  }

  @Test
  void BR_CAT_01_requiredAttributesPresent() {
    List<String> missing = new ArrayList<>();
    for (FoodItem f : active) {
      if (f.getRole() == null) missing.add(f.getName() + ": role");
      if (f.getSource() == null || f.getSource().isBlank()) missing.add(f.getName() + ": source");
      if (f.getSuitableSlots().isEmpty()) missing.add(f.getName() + ": slots");
      if (f.getPortionMinG() == null || f.getDefaultPortionG() == null)
        missing.add(f.getName() + ": portion");
    }
    assertThat(missing).isEmpty();
  }

  @Test
  void BR_CAT_03_everyFoodDeclaresTheStateItsValuesReferTo() {
    assertThat(active).allSatisfy(f -> assertThat(f.getState()).as(f.getName()).isNotNull());
  }

  @Test
  void BR_CAT_04_legacyRowsWithoutASourceAreInactive() {
    assertThat(foods.findAll())
        .filteredOn(f -> f.getSource() == null)
        .allSatisfy(f -> assertThat(f.isActive()).as(f.getName()).isFalse());
  }

  @Test
  void everyRoleHasAtLeastThreeVeganFoods() {
    // keeps strict plant-based profiles feasible once plans are composed by role (US3)
    Map<FoodRole, Long> vegan =
        active.stream()
            .filter(f -> f.getDietFlags().contains(DietFlag.VEGAN))
            .collect(Collectors.groupingBy(FoodItem::getRole, Collectors.counting()));
    for (FoodRole role : FoodRole.values()) {
      assertThat(vegan.getOrDefault(role, 0L)).as(role.name()).isGreaterThanOrEqualTo(3);
    }
  }

  @Test
  void databaseMatchesTheCatalogCsv() throws IOException {
    String csv =
        new ClassPathResource("db/catalog/foods.csv").getContentAsString(StandardCharsets.UTF_8);
    List<String> lines =
        csv.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).skip(1).toList();
    Map<String, FoodItem> byName =
        active.stream().collect(Collectors.toMap(FoodItem::getName, Function.identity()));

    assertThat(byName.keySet())
        .containsExactlyInAnyOrderElementsOf(lines.stream().map(l -> l.split(",")[0]).toList());
    for (String line : lines) {
      String[] c = line.split(",", -1);
      FoodItem f = byName.get(c[0]);
      double[] expected = Arrays.stream(c, 4, 11).mapToDouble(Double::parseDouble).toArray();
      Nutrients n = per100g(f);
      assertThat(
              new double[] {
                n.kcal(), n.proteinG(), n.carbsG(), n.fatG(), n.sugarG(), n.fiberG(), n.sodiumMg()
              })
          .as(c[0])
          .containsExactly(expected);
    }
  }
}
