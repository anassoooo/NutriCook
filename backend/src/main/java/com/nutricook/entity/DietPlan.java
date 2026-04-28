package com.nutricook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nutricook.entity.enums.PlanStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

  @OneToMany(mappedBy = "dietPlan", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Meal> meals = new ArrayList<>();
}
