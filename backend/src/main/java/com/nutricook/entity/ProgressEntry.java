package com.nutricook.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(
    name = "progress_entries",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_user_progress_date",
            columnNames = {"user_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class ProgressEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private LocalDate date;

  private Double weightKg;

  /** Derived from checked meals unless {@link #caloriesOverridden} (BR-PRG-05). */
  private Integer caloriesConsumed;

  /** The user typed {@link #caloriesConsumed} themselves. */
  @Column(nullable = false)
  @Builder.Default
  private boolean caloriesOverridden = false;

  private Integer waterMl;

  /** Always derived: the number of checked meals (BR-PRG-05). */
  @Column(nullable = false)
  @Builder.Default
  private Integer mealsCompleted = 0;

  private Integer exerciseMinutes;

  /** The target this day is judged against (BR-PRG-06). */
  private Integer targetCalories;

  @Column(columnDefinition = "TEXT")
  private String notes;
}
