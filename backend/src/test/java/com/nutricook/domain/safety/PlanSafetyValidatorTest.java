package com.nutricook.domain.safety;

import static com.nutricook.domain.fixtures.TestConstraints.*;
import static com.nutricook.domain.fixtures.TestFoods.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.model.Component;
import com.nutricook.domain.model.MealDraft;
import com.nutricook.domain.model.PlanDraft;
import com.nutricook.domain.model.Slot;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** BR-SAF-05/06/09: the gate every plan passes before it is saved. */
class PlanSafetyValidatorTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 10);

  private static SafetyConstraints of(List<RestrictionRule> r, List<ConditionRule> c) {
    return SafetyConstraints.from(r, c);
  }

  private static MealDraft meal(Slot slot, Component... components) {
    return new MealDraft(slot, List.of(components));
  }

  @Test
  void BR_SAF_06_diabetesSugarLimitIsInclusive() {
    SafetyConstraints diabetes = of(List.of(), List.of(DIABETES));
    // Mango: 13.7 g sugar per 100 g. 109.5 g -> 15.0 g (allowed); 110.5 g -> 15.1 g (not)
    MealDraft atLimit = meal(Slot.MORNING_SNACK, new Component(mango(), 109.5));
    MealDraft overLimit = meal(Slot.MORNING_SNACK, new Component(mango(), 110.5));

    assertThat(atLimit.nutrients().sugarG()).isEqualTo(15.0);
    assertThat(PlanSafetyValidator.validateMeal(atLimit, diabetes)).isEmpty();

    assertThat(overLimit.nutrients().sugarG()).isEqualTo(15.1);
    assertThat(PlanSafetyValidator.validateMeal(overLimit, diabetes))
        .singleElement()
        .satisfies(
            v -> {
              assertThat(v.type()).isEqualTo(Violation.Type.SUGAR_PER_MEAL);
              assertThat(v.constraint()).isEqualTo("Diabetes");
            });
  }

  @Test
  void BR_SAF_06_cholesterolFatLimitPerMeal() {
    SafetyConstraints cholesterol = of(List.of(), List.of(HIGH_CHOLESTEROL));
    // Olive oil 30 g = 30 g fat > 25 g
    MealDraft lunch =
        meal(Slot.LUNCH, new Component(chickenBreast(), 150), new Component(oliveOil(), 30));

    assertThat(PlanSafetyValidator.validateMeal(lunch, cholesterol))
        .extracting(Violation::type)
        .containsExactly(Violation.Type.FAT_PER_MEAL);
  }

  @Test
  void BR_SAF_06_hypertensionSodiumLimitIsPerDay() {
    SafetyConstraints hypertension = of(List.of(), List.of(HYPERTENSION));
    // Cheddar 653 mg sodium per 100 g: 80 g = 522.4 mg, rounded per meal to 522 (BR-CMP-09).
    // 4 meals = 2088 mg in the day; each meal alone is fine
    List<MealDraft> meals =
        List.of(
            meal(Slot.BREAKFAST, new Component(cheddar(), 80)),
            meal(Slot.LUNCH, new Component(cheddar(), 80)),
            meal(Slot.AFTERNOON_SNACK, new Component(cheddar(), 80)),
            meal(Slot.DINNER, new Component(cheddar(), 80)));
    PlanDraft day = new PlanDraft(DAY, meals, List.of());

    meals.forEach(m -> assertThat(PlanSafetyValidator.validateMeal(m, hypertension)).isEmpty());
    assertThat(PlanSafetyValidator.validate(day, hypertension))
        .singleElement()
        .satisfies(
            v -> {
              assertThat(v.type()).isEqualTo(Violation.Type.SODIUM_PER_DAY);
              assertThat(v.message()).contains("2088 mg").contains("2000 mg");
            });
  }

  @Test
  void BR_SAF_09_kosherForbidsMeatWithMilkInOneMeal() {
    SafetyConstraints kosher = of(List.of(KOSHER), List.of());
    // Both foods fail the KOSHER flag too; the meat/milk rule is reported in addition
    MealDraft lunch =
        meal(Slot.LUNCH, new Component(chickenBreast(), 150), new Component(cheddar(), 20));

    assertThat(PlanSafetyValidator.validateMeal(lunch, kosher))
        .extracting(Violation::type)
        .contains(Violation.Type.KOSHER_MEAT_WITH_MILK);
  }

  @Test
  void BR_SAF_09_meatWithMilkIsFineWhenNotKosher() {
    MealDraft lunch =
        meal(Slot.LUNCH, new Component(chickenBreast(), 150), new Component(cheddar(), 20));
    assertThat(PlanSafetyValidator.validateMeal(lunch, SafetyConstraints.NONE)).isEmpty();
  }

  @Test
  void BR_SAF_05_validatorChecksEveryComponentAndReportsAllViolations() {
    SafetyConstraints c = of(List.of(NUT_ALLERGY, GLUTEN_FREE), List.of(DIABETES));
    MealDraft breakfast =
        meal(
            Slot.BREAKFAST,
            new Component(wholeWheatBread(), 60),
            new Component(peanutButter(), 15),
            new Component(mango(), 150)); // 20.6 g sugar in mango alone

    assertThat(PlanSafetyValidator.validateMeal(breakfast, c))
        .extracting(Violation::constraint)
        .containsExactlyInAnyOrder("Gluten-Free", "Nut Allergy", "Diabetes");
  }

  @Test
  void BR_SAF_05_aSafePlanHasNoViolations() {
    SafetyConstraints c =
        of(List.of(NUT_ALLERGY, GLUTEN_FREE, HALAL), List.of(DIABETES, HYPERTENSION));
    PlanDraft day =
        new PlanDraft(
            DAY,
            List.of(
                meal(
                    Slot.LUNCH,
                    new Component(chickenBreastHalal(), 150),
                    new Component(whiteRice(), 150),
                    new Component(broccoli(), 150))),
            List.of());

    assertThat(PlanSafetyValidator.validate(day, c)).isEmpty();
  }

  @Test
  void strictestLimitWinsWhenConditionsOverlap() {
    ConditionRule strict = new ConditionRule("Strict Diet", 10.0, null, null, java.util.Set.of());
    SafetyConstraints c = of(List.of(), List.of(DIABETES, strict));
    assertThat(c.sugarPerMeal().value()).isEqualTo(10.0);
    assertThat(c.sugarPerMeal().source()).isEqualTo("Strict Diet");
  }
}
