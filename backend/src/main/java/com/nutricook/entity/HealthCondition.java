package com.nutricook.entity;

import com.nutricook.domain.model.Allergen;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/** A health condition and the hard constraints it implies (BR-SAF-06, BR-PRF-07). */
@Entity
@Table(name = "health_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCondition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name;

  private String description;

  /** Max grams of total sugar per meal. Null means no limit. */
  @Column(name = "sugar_limit_per_meal_g")
  private Double sugarLimitPerMealG;

  /** Max grams of fat per meal. Null means no limit. */
  @Column(name = "fat_limit_per_meal_g")
  private Double fatLimitPerMealG;

  /** Max milligrams of sodium per day. Null means no limit. */
  @Column(name = "sodium_limit_per_day_mg")
  private Double sodiumLimitPerDayMg;

  /** Allergens excluded because of this condition, e.g. Celiac Disease ⇒ GLUTEN. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "condition_implied_allergens",
      joinColumns = @JoinColumn(name = "condition_id"))
  @Column(name = "allergen", length = 16)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private Set<Allergen> impliedAllergenExclusions = new HashSet<>();
}
