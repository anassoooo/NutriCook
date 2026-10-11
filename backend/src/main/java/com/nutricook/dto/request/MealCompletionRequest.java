package com.nutricook.dto.request;

import jakarta.validation.constraints.NotNull;

/** Checks a meal off ({@code true}) or unchecks it (BR-PRG-05). */
public record MealCompletionRequest(@NotNull Boolean completed) {}
