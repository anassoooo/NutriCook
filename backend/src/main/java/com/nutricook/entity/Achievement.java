package com.nutricook.entity;

import com.nutricook.entity.enums.AchievementType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  private String iconName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AchievementType type;

  /** Threshold value to earn this achievement (meaning depends on type). */
  @Column(nullable = false)
  private Integer threshold;
}
