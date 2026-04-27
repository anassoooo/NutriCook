package com.nutricook.entity;

import jakarta.persistence.*;
import lombok.*;

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
}
