package com.nutricook.domain.safety;

import static com.nutricook.domain.fixtures.TestConstraints.*;
import static com.nutricook.domain.fixtures.TestFoods.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.model.FoodView;
import java.util.List;
import org.junit.jupiter.api.Test;

/** BR-SAF-01/02/03/10, BR-PRF-07: which foods a user may ever be offered. */
class EligibilityPolicyTest {

  private static SafetyConstraints only(RestrictionRule... rules) {
    return SafetyConstraints.from(List.of(rules), List.of());
  }

  private static SafetyConstraints conditions(ConditionRule... rules) {
    return SafetyConstraints.from(List.of(), List.of(rules));
  }

  @Test
  void BR_SAF_02_nutAllergyExcludesPeanutButter() {
    List<Violation> v = EligibilityPolicy.check(peanutButter(), only(NUT_ALLERGY));

    assertThat(v)
        .singleElement()
        .satisfies(
            x -> {
              assertThat(x.type()).isEqualTo(Violation.Type.ALLERGEN);
              assertThat(x.constraint()).isEqualTo("Nut Allergy");
              assertThat(x.message())
                  .isEqualTo("Peanut Butter contains peanuts, excluded by Nut Allergy.");
            });
  }

  @Test
  void BR_SAF_02_nutAllergyExcludesTreeNuts() {
    assertThat(EligibilityPolicy.allows(almonds(), only(NUT_ALLERGY))).isFalse();
  }

  @Test
  void BR_SAF_02_glutenFreeAllowsRiceQuinoaAndCorn() {
    for (FoodView f : List.of(whiteRice(), quinoa(), cornTortilla())) {
      assertThat(EligibilityPolicy.allows(f, only(GLUTEN_FREE))).as(f.name()).isTrue();
    }
  }

  @Test
  void BR_SAF_02_glutenFreeExcludesWheatCouscousAndSeitan() {
    for (FoodView f : List.of(wholeWheatBread(), couscous(), seitan())) {
      assertThat(EligibilityPolicy.allows(f, only(GLUTEN_FREE))).as(f.name()).isFalse();
    }
  }

  @Test
  void BR_SAF_02_lactoseFreeExcludesDairy() {
    assertThat(EligibilityPolicy.allows(greekYogurt(), only(LACTOSE_FREE))).isFalse();
    assertThat(EligibilityPolicy.allows(cheddar(), only(LACTOSE_FREE))).isFalse();
    assertThat(EligibilityPolicy.allows(tofu(), only(LACTOSE_FREE))).isTrue();
  }

  @Test
  void BR_SAF_02_shellfishAllergyCoversCrustaceansAndMolluscs() {
    assertThat(EligibilityPolicy.allows(shrimp(), only(SHELLFISH_ALLERGY))).isFalse();
    assertThat(EligibilityPolicy.allows(octopus(), only(SHELLFISH_ALLERGY))).isFalse();
    assertThat(EligibilityPolicy.allows(salmon(), only(SHELLFISH_ALLERGY))).isTrue();
  }

  @Test
  void BR_SAF_02_otherAllergiesUseTheirAllergen() {
    assertThat(EligibilityPolicy.allows(salmon(), only(FISH_ALLERGY))).isFalse();
    assertThat(EligibilityPolicy.allows(tofu(), only(SOY_ALLERGY))).isFalse();
    assertThat(EligibilityPolicy.allows(tahini(), only(SESAME_ALLERGY))).isFalse();
    assertThat(EligibilityPolicy.allows(lentils(), only(EGG_ALLERGY, SOY_ALLERGY))).isTrue();
  }

  @Test
  void BR_SAF_02_veganRequiresTheVeganFlag() {
    assertThat(EligibilityPolicy.allows(lentils(), only(VEGAN))).isTrue();
    assertThat(EligibilityPolicy.allows(greekYogurt(), only(VEGAN))).isFalse();
    // bread may contain milk or honey, so it is not flagged vegan (BR-SAF-10)
    assertThat(EligibilityPolicy.allows(wholeWheatBread(), only(VEGAN))).isFalse();
  }

  @Test
  void BR_SAF_02_vegetarianRequiresTheVegetarianFlag() {
    assertThat(EligibilityPolicy.allows(greekYogurt(), only(VEGETARIAN))).isTrue();
    assertThat(EligibilityPolicy.allows(chickenBreast(), only(VEGETARIAN))).isFalse();
    assertThat(EligibilityPolicy.allows(salmon(), only(VEGETARIAN))).isFalse();
  }

  @Test
  void BR_SAF_02_halalRequiresTheHalalFlag() {
    assertThat(EligibilityPolicy.allows(chickenBreast(), only(HALAL))).isFalse();
    assertThat(EligibilityPolicy.allows(chickenBreastHalal(), only(HALAL))).isTrue();
    assertThat(EligibilityPolicy.allows(salmon(), only(HALAL))).isTrue();
  }

  @Test
  void BR_SAF_10_shellfishIsNeverKosherOrHalal() {
    assertThat(EligibilityPolicy.allows(shrimp(), only(KOSHER))).isFalse();
    assertThat(EligibilityPolicy.allows(shrimp(), only(HALAL))).isFalse();
    assertThat(EligibilityPolicy.allows(salmon(), only(KOSHER))).isTrue();
  }

  @Test
  void BR_SAF_10_cheeseCarriesNoReligiousOrVegetarianFlag() {
    assertThat(EligibilityPolicy.allows(cheddar(), only(VEGETARIAN))).isFalse();
    assertThat(EligibilityPolicy.allows(cheddar(), only(HALAL))).isFalse();
    assertThat(EligibilityPolicy.allows(cheddar(), only(KOSHER))).isFalse();
  }

  @Test
  void BR_SAF_03_unverifiedAllergensAreIneligibleForAllergicUsers() {
    List<Violation> v = EligibilityPolicy.check(unverified("Mystery Greens"), only(NUT_ALLERGY));
    assertThat(v).extracting(Violation::type).containsExactly(Violation.Type.UNKNOWN_ALLERGENS);
  }

  @Test
  void BR_SAF_03_unverifiedAllergensAreFineWithoutAllergies() {
    assertThat(EligibilityPolicy.allows(unverified("Mystery Greens"), only(VEGAN))).isTrue();
    assertThat(EligibilityPolicy.allows(unverified("Mystery Greens"), SafetyConstraints.NONE))
        .isTrue();
  }

  @Test
  void BR_PRF_07_celiacDiseaseImpliesGlutenExclusion() {
    List<Violation> v = EligibilityPolicy.check(couscous(), conditions(CELIAC));
    assertThat(v)
        .singleElement()
        .satisfies(
            x -> {
              assertThat(x.type()).isEqualTo(Violation.Type.ALLERGEN);
              assertThat(x.constraint()).isEqualTo("Celiac Disease");
            });
  }

  @Test
  void BR_PRF_07_lactoseIntoleranceImpliesMilkExclusion() {
    assertThat(EligibilityPolicy.allows(greekYogurt(), conditions(LACTOSE_INTOLERANCE))).isFalse();
  }

  @Test
  void BR_SAF_01_eligibilityIgnoresNameAndCategory() {
    // Category says "legume" and the name says nothing about nuts: only the allergen data counts
    assertThat(peanutButter().category()).isEqualTo("legume");
    assertThat(EligibilityPolicy.allows(peanutButter(), only(NUT_ALLERGY))).isFalse();
    // Category says "grain": still allowed for Gluten-Free because it has no GLUTEN allergen
    assertThat(whiteRice().category()).isEqualTo("grain");
    assertThat(EligibilityPolicy.allows(whiteRice(), only(GLUTEN_FREE))).isTrue();
  }

  @Test
  void everyViolationIsReportedNotJustTheFirst() {
    // Peanut butter for a nut-allergic kosher user: allergen and missing flag
    List<Violation> v = EligibilityPolicy.check(peanutButter(), only(NUT_ALLERGY, KOSHER));
    assertThat(v)
        .extracting(Violation::type)
        .containsExactlyInAnyOrder(Violation.Type.ALLERGEN, Violation.Type.DIET_FLAG);
  }
}
