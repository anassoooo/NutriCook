package com.nutricook.domain.plan;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.model.PlanStatus;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/** The plan state machine and date rules (BR-LIF-01/03/04). */
public final class PlanLifecycle {

  /** Plans can be generated for today and up to this many days ahead (BR-LIF-03). */
  public static final int MAX_DAYS_AHEAD = 7;

  private static final Map<PlanStatus, Set<PlanStatus>> ALLOWED =
      Map.of(
          PlanStatus.ACTIVE, Set.of(PlanStatus.SUPERSEDED, PlanStatus.ARCHIVED),
          PlanStatus.SUPERSEDED, Set.of(),
          PlanStatus.ARCHIVED, Set.of());

  private PlanLifecycle() {}

  /**
   * @return {@code to}, when the transition is allowed
   * @throws DomainException PLAN_READ_ONLY otherwise
   */
  public static PlanStatus transition(PlanStatus from, PlanStatus to) {
    if (!ALLOWED.get(from).contains(to)) {
      throw new DomainException(
          ErrorCode.PLAN_READ_ONLY,
          "A " + label(from) + " plan cannot become " + label(to) + ".",
          Map.of("status", from.name()));
    }
    return to;
  }

  /** Only an ACTIVE plan for today or later may change (swaps, check-offs) (BR-LIF-04). */
  public static void assertMutable(PlanStatus status, LocalDate planDate, LocalDate today) {
    if (status != PlanStatus.ACTIVE) {
      throw new DomainException(
          ErrorCode.PLAN_READ_ONLY,
          "This plan is " + label(status) + " and can no longer be changed.",
          Map.of("status", status.name()));
    }
    if (planDate.isBefore(today)) {
      throw new DomainException(
          ErrorCode.PLAN_READ_ONLY,
          "Plans for past days can no longer be changed.",
          Map.of("status", status.name()));
    }
  }

  /** BR-LIF-03: the plan date must be within [today, today + 7] in the user's timezone. */
  public static void assertGeneratable(LocalDate date, LocalDate today) {
    if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          "Plans can be generated for today and the next " + MAX_DAYS_AHEAD + " days.",
          Map.of(
              "field", "date",
              "minDate", today.toString(),
              "maxDate", today.plusDays(MAX_DAYS_AHEAD).toString()));
    }
  }

  private static String label(PlanStatus s) {
    return s.name().toLowerCase();
  }
}
