package com.nutricook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nutricook.domain.model.FoodRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Table(name = "meal_food_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealFoodItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meal_id", nullable = false)
  private Meal meal;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "food_item_id", nullable = false)
  private FoodItem foodItem;

  @Positive
  @Column(nullable = false)
  private Double quantityG;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private FoodRole role;

  // Nutrition snapshot of this portion at generation time: catalog edits never rewrite history
  // (BR-LIF-05). Rounded to 0.1.
  @Column(nullable = false)
  private Double kcal;

  @Column(name = "protein_g", nullable = false)
  private Double proteinG;

  @Column(name = "carbs_g", nullable = false)
  private Double carbsG;

  @Column(name = "fat_g", nullable = false)
  private Double fatG;

  @Column(name = "sugar_g", nullable = false)
  private Double sugarG;

  @Column(name = "fiber_g", nullable = false)
  private Double fiberG;

  @Column(name = "sodium_mg", nullable = false)
  private Double sodiumMg;
}
