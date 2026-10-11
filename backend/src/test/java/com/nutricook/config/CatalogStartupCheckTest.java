package com.nutricook.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodState;
import com.nutricook.domain.model.Slot;
import com.nutricook.entity.FoodItem;
import com.nutricook.repository.FoodItemRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** BR-CAT-02: the application does not start with an active food that fails the gate. */
class CatalogStartupCheckTest {

  private static FoodItem rice(String name, double kcal) {
    return FoodItem.builder()
        .id((long) name.hashCode())
        .name(name)
        .role(FoodRole.CARB)
        .state(FoodState.COOKED)
        .caloriesPer100g(kcal)
        .proteinPer100g(2.7)
        .carbsPer100g(28.0)
        .fatPer100g(0.3)
        .sugarPer100g(0.1)
        .fiberPer100g(0.4)
        .sodiumMgPer100g(1.0)
        .portionMinG(50.0)
        .portionMaxG(300.0)
        .portionStepG(25.0)
        .defaultPortionG(150.0)
        .suitableSlots(Set.of(Slot.LUNCH))
        .allergensKnown(true)
        .source("USDA SR Legacy")
        .build();
  }

  private static CatalogStartupCheck checkWith(FoodItem... active) {
    FoodItemRepository foods = mock(FoodItemRepository.class);
    when(foods.findByActiveTrue()).thenReturn(List.of(active));
    return new CatalogStartupCheck(foods);
  }

  @Test
  void BR_CAT_02_startupAcceptsACleanCatalog() {
    assertThatCode(() -> checkWith(rice("White Rice", 130)).run(null)).doesNotThrowAnyException();
  }

  @Test
  void BR_CAT_02_startupRefusesABadActiveFoodAndNamesIt() {
    assertThatThrownBy(() -> checkWith(rice("White Rice", 130), rice("Bad Rice", 600)).run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Bad Rice")
        .hasMessageContaining("energy 600")
        .hasMessageNotContaining("White Rice");
  }
}
