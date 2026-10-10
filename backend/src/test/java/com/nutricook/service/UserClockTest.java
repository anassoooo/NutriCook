package com.nutricook.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class UserClockTest {

  // 23:30 UTC on 10 October 2026
  private final UserClock userClock =
      new UserClock(Clock.fixed(Instant.parse("2026-10-10T23:30:00Z"), ZoneOffset.UTC));

  @Test
  void BR_TIM_01_todayUsesTheUsersZone() {
    assertThat(userClock.today("UTC")).isEqualTo(LocalDate.of(2026, 10, 10));
    // Tunis is UTC+1: it is already 00:30 on the 11th there
    assertThat(userClock.today("Africa/Tunis")).isEqualTo(LocalDate.of(2026, 10, 11));
    // New York is UTC-4 in October: still the 10th
    assertThat(userClock.today("America/New_York")).isEqualTo(LocalDate.of(2026, 10, 10));
  }

  @Test
  void BR_TIM_01_missingOrUnknownZoneFallsBackToUtc() {
    assertThat(userClock.today(null)).isEqualTo(LocalDate.of(2026, 10, 10));
    assertThat(userClock.today("Mars/Olympus_Mons")).isEqualTo(LocalDate.of(2026, 10, 10));
  }
}
