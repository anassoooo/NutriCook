package com.nutricook.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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

  @Min(0)
  @Builder.Default
  private Integer caloriesConsumed = 0;

  @Min(0)
  @Builder.Default
  private Integer waterMl = 0;

  @Min(0)
  @Builder.Default
  private Integer mealsCompleted = 0;

  @Min(0)
  private Integer exerciseMinutes;

  @Column(columnDefinition = "TEXT")
  private String notes;
}
