package com.nutricook.domain.plan;

import static com.nutricook.domain.model.PlanStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.model.PlanStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** BR-LIF-01/03/04. */
class PlanLifecycleTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

  @ParameterizedTest(name = "{0} -> {1}: allowed={2}")
  @CsvSource({
    "ACTIVE,     SUPERSEDED, true",
    "ACTIVE,     ARCHIVED,   true",
    "ACTIVE,     ACTIVE,     false",
    "SUPERSEDED, ACTIVE,     false",
    "SUPERSEDED, ARCHIVED,   false",
    "SUPERSEDED, SUPERSEDED, false",
    "ARCHIVED,   ACTIVE,     false",
    "ARCHIVED,   SUPERSEDED, false",
    "ARCHIVED,   ARCHIVED,   false"
  })
  void BR_LIF_01_onlyActivePlansMove(PlanStatus from, PlanStatus to, boolean allowed) {
    if (allowed) {
      assertThat(PlanLifecycle.transition(from, to)).isEqualTo(to);
    } else {
      assertThatThrownBy(() -> PlanLifecycle.transition(from, to))
          .isInstanceOfSatisfying(
              DomainException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.PLAN_READ_ONLY));
    }
  }

  @Test
  void BR_LIF_03_plansFromTodayToSevenDaysAhead() {
    assertThatCode(() -> PlanLifecycle.assertGeneratable(TODAY, TODAY)).doesNotThrowAnyException();
    assertThatCode(() -> PlanLifecycle.assertGeneratable(TODAY.plusDays(7), TODAY))
        .doesNotThrowAnyException();
    for (LocalDate bad : new LocalDate[] {TODAY.minusDays(1), TODAY.plusDays(8)}) {
      assertThatThrownBy(() -> PlanLifecycle.assertGeneratable(bad, TODAY))
          .isInstanceOfSatisfying(
              DomainException.class,
              e -> {
                assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                assertThat(e.details()).containsEntry("field", "date");
              });
    }
  }

  @Test
  void BR_LIF_04_onlyActivePlansForTodayOrLaterCanChange() {
    assertThatCode(() -> PlanLifecycle.assertMutable(ACTIVE, TODAY, TODAY))
        .doesNotThrowAnyException();
    assertThatCode(() -> PlanLifecycle.assertMutable(ACTIVE, TODAY.plusDays(3), TODAY))
        .doesNotThrowAnyException();

    assertThatThrownBy(() -> PlanLifecycle.assertMutable(ACTIVE, TODAY.minusDays(1), TODAY))
        .isInstanceOfSatisfying(
            DomainException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.PLAN_READ_ONLY));
    assertThatThrownBy(() -> PlanLifecycle.assertMutable(ARCHIVED, TODAY, TODAY))
        .isInstanceOfSatisfying(
            DomainException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.PLAN_READ_ONLY));
    assertThatThrownBy(() -> PlanLifecycle.assertMutable(SUPERSEDED, TODAY, TODAY))
        .isInstanceOfSatisfying(
            DomainException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.PLAN_READ_ONLY));
  }
}
