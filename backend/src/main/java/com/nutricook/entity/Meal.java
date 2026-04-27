package com.nutricook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nutricook.entity.enums.MealType;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "meals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"dietPlan", "mealFoodItems"})
public class Meal {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "diet_plan_id", nullable = false)
  private DietPlan dietPlan;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MealType mealType;

  private Integer calories;
  private Double proteinG;
  private Double carbsG;
  private Double fatG;
  private Double sugarG;
  private Double fiberG;
  private Integer sortOrder;

  @Column(columnDefinition = "TEXT")
  private String aiSuggestion;

  @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<MealFoodItem> mealFoodItems = new ArrayList<>();
}
