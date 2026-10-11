package com.nutricook.entity;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodState;
import com.nutricook.domain.model.Slot;
import com.nutricook.entity.enums.FoodOrigin;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * A catalog food (BR-CAT-01). Seeded entries are maintained in db/catalog/foods.csv. Safety is
 * decided from {@link #allergens}, {@link #allergensKnown} and {@link #dietFlags}; {@link
 * #category} is a display label only (BR-SAF-01).
 */
@Entity
@Table(name = "food_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name;

  private String category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private FoodRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private FoodState state;

  @Column(nullable = false)
  private Double caloriesPer100g;

  @Column(nullable = false)
  private Double proteinPer100g;

  @Column(nullable = false)
  private Double carbsPer100g;

  @Column(nullable = false)
  private Double fatPer100g;

  @Builder.Default private Double sugarPer100g = 0.0;

  @Builder.Default private Double fiberPer100g = 0.0;

  @Builder.Default private Double sodiumMgPer100g = 0.0;

  @Column(name = "portion_min_g", nullable = false)
  private Double portionMinG;

  @Column(name = "portion_max_g", nullable = false)
  private Double portionMaxG;

  @Column(name = "portion_step_g", nullable = false)
  private Double portionStepG;

  @Column(name = "default_portion_g", nullable = false)
  private Double defaultPortionG;

  /** Dataset the values come from, e.g. "USDA SR Legacy". */
  private String source;

  /** SEED foods are maintained in foods.csv; ADMIN foods through the admin API (BR-CAT-05). */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 8)
  @Builder.Default
  private FoodOrigin origin = FoodOrigin.ADMIN;

  /** Inactive foods are never offered for new plans but stay in history (BR-CAT-04). */
  @Column(nullable = false)
  @Builder.Default
  private boolean active = true;

  /** False until the allergen list is verified; unverified foods fail closed (BR-SAF-03). */
  @Column(nullable = false)
  @Builder.Default
  private boolean allergensKnown = false;

  /** Land-animal meat (BR-SAF-09/10). */
  @Column(nullable = false)
  @Builder.Default
  private boolean meat = false;

  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SUBSELECT)
  @CollectionTable(name = "food_allergens", joinColumns = @JoinColumn(name = "food_id"))
  @Column(name = "allergen", length = 16)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private Set<Allergen> allergens = new HashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SUBSELECT)
  @CollectionTable(name = "food_diet_flags", joinColumns = @JoinColumn(name = "food_id"))
  @Column(name = "diet_flag", length = 16)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private Set<DietFlag> dietFlags = new HashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SUBSELECT)
  @CollectionTable(name = "food_suitable_slots", joinColumns = @JoinColumn(name = "food_id"))
  @Column(name = "slot", length = 16)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private Set<Slot> suitableSlots = new HashSet<>();
}
