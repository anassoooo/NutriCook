package com.nutricook.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * "Today" in a user's own timezone (BR-TIM-01). The only place services turn the clock into dates.
 */
@Component
@RequiredArgsConstructor
public class UserClock {

  private final Clock clock;

  public LocalDate today(String timezone) {
    return LocalDate.now(clock.withZone(zoneOrUtc(timezone)));
  }

  /** Stored zones are validated on input; anything unreadable falls back to UTC. */
  static ZoneId zoneOrUtc(String timezone) {
    if (timezone == null || timezone.isBlank()) return ZoneOffset.UTC;
    try {
      return ZoneId.of(timezone);
    } catch (DateTimeException e) {
      return ZoneOffset.UTC;
    }
  }
}
