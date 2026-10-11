package com.nutricook.domain.achievement;

import java.time.LocalDate;

/** One logged day, as achievements see it. {@code null} means "not logged". */
public record DayRecord(
    LocalDate date,
    Integer caloriesConsumed,
    Integer targetCalories,
    Integer waterMl,
    Double weightKg) {}
