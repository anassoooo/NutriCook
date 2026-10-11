package com.nutricook.entity;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.entity.enums.RestrictionType;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/**
 * A dietary restriction and what it excludes (BR-SAF-02). The mapping is data: allergens a food
 * must not contain, and/or a diet flag it must carry. Never matched by name.
 */
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

  /** A food must carry this flag, e.g. Vegan ⇒ VEGAN. Null when not applicable. */
  @Enumerated(EnumType.STRING)
  @Column(name = "required_diet_flag", length = 16)
  private DietFlag requiredDietFlag;

  /** A food must contain none of these, e.g. Nut Allergy ⇒ PEANUTS, TREE_NUTS. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "restriction_excluded_allergens",
      joinColumns = @JoinColumn(name = "restriction_id"))
  @Column(name = "allergen", length = 16)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private Set<Allergen> excludedAllergens = new HashSet<>();
}
