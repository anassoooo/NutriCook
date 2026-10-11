package com.nutricook.service;

import java.time.LocalDate;

/**
 * A plan for a user-day was saved or archived. Handled synchronously, inside the same transaction,
 * so the day's progress entry never disagrees with its plan (BR-PRG-05/06).
 */
public record DayPlanChangedEvent(Long userId, LocalDate date) {}
