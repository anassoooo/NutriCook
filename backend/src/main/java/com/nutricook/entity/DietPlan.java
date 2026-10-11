package com.nutricook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nutricook.domain.model.PlanStatus;
import com.nutricook.domain.model.Warning;
import com.nutricook.entity.enums.AiStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "diet_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "meals"})
public class DietPlan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String title;

  private Integer totalCalories;
  private Double totalProteinG;
  private Double totalCarbsG;
  private Double totalFatG;
  private Double totalSugarG;
  private Double totalFiberG;

  @Column(name = "total_sodium_mg")
  private Double totalSodiumMg;

  /** The calorie target this plan was built for (BR-PRG-06). */
  private Integer targetCalories;

  @Column(name = "valid_for_date", nullable = false)
  private LocalDate validForDate;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime generatedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private PlanStatus status = PlanStatus.ACTIVE;

  @Column(columnDefinition = "TEXT")
  private String aiExplanation;

  /** Whether the optional AI text is still being written (BR-LIF-07). */
  @Enumerated(EnumType.STRING)
  @Column(name = "ai_status", nullable = false, length = 16)
  @Builder.Default
  private AiStatus aiStatus = AiStatus.PENDING;

  /** 1 for the first plan of a user-day, 2 for the first regeneration, ... (BR-CMP-07). */
  @Column(name = "generation_seq", nullable = false)
  @Builder.Default
  private Integer generationSeq = 1;

  /** Composer seed, so any plan can be reproduced exactly (BR-CMP-07). */
  private Long seed;

  /** Non-blocking issues, e.g. TARGET_DEVIATION (BR-CMP-05). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  @Builder.Default
  private List<Warning> warnings = new ArrayList<>();

  @OneToMany(mappedBy = "dietPlan", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Meal> meals = new ArrayList<>();
}
