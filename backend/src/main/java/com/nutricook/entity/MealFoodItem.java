package com.nutricook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
}
