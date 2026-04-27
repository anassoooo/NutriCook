package com.nutricook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "user_achievements",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_user_achievement",
            columnNames = {"user_id", "achievement_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class UserAchievement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "achievement_id", nullable = false)
  private Achievement achievement;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime earnedAt;
}
