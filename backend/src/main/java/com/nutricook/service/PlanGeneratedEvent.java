package com.nutricook.service;

/** Published inside the generation transaction; handled only after it commits (BR-LIF-07). */
public record PlanGeneratedEvent(Long planId, Long userId) {}
