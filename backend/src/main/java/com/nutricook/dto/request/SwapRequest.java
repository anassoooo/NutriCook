package com.nutricook.dto.request;

import jakarta.validation.constraints.NotNull;

/** Replace one food in a meal with {@code foodId} (FR-003). */
public record SwapRequest(@NotNull Long foodId) {}
