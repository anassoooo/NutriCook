package com.nutricook.entity;

import com.nutricook.entity.enums.RestrictionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dietary_restrictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietaryRestriction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RestrictionType type;

  private String description;
}
