package com.nutricook.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Operational limits that protect the free AI tier (business-rules.md §13).
 *
 * @param generationsPerDay plan generations per user per user-day (BR-LIF-06)
 * @param chatPerHour chat messages per user per rolling hour (BR-AI-07)
 * @param aiTimeout timeout of a single AI call (BR-AI-05)
 */
@Validated
@ConfigurationProperties(prefix = "app.limits")
public record LimitsProperties(
    @Min(1) @Max(1000) int generationsPerDay,
    @Min(1) @Max(1000) int chatPerHour,
    @NotNull Duration aiTimeout) {}
