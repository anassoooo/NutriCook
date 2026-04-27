package com.nutricook.entity;

import jakarta.persistence.*;
import lombok.*;

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

  /** Max grams of sugar per meal. Null means no limit. */
  private Double sugarLimitG;

  /** Max grams of fat per meal. Null means no limit. */
  private Double fatLimitG;
}
