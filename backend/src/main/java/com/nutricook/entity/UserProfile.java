package com.nutricook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nutricook.entity.enums.ActivityLevel;
import com.nutricook.entity.enums.Gender;
import com.nutricook.entity.enums.HealthGoal;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "healthConditions", "dietaryRestrictions"})
public class UserProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  private String firstName;
  private String lastName;
  private LocalDate dateOfBirth;

  @Enumerated(EnumType.STRING)
  private Gender gender;

  private Double weightKg;

  private Double heightCm;

  @Enumerated(EnumType.STRING)
  private ActivityLevel activityLevel;

  @Enumerated(EnumType.STRING)
  private HealthGoal healthGoal;

  /** IANA zone id; defines the user's "today" (BR-TIM-01). Ranges are validated at the API. */
  @Column(nullable = false, length = 64)
  @Builder.Default
  private String timezone = "UTC";

  @UpdateTimestamp private LocalDateTime updatedAt;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_health_conditions",
      joinColumns = @JoinColumn(name = "user_profile_id"),
      inverseJoinColumns = @JoinColumn(name = "health_condition_id"))
  @Builder.Default
  private Set<HealthCondition> healthConditions = new HashSet<>();

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_dietary_restrictions",
      joinColumns = @JoinColumn(name = "user_profile_id"),
      inverseJoinColumns = @JoinColumn(name = "dietary_restriction_id"))
  @Builder.Default
  private Set<DietaryRestriction> dietaryRestrictions = new HashSet<>();
}
