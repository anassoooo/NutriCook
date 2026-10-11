package com.nutricook.domain;

/**
 * Stable, client-facing business error codes (business-rules.md §12).
 *
 * <p>Codes are part of the public API contract: the frontend maps each one to a message. Never
 * rename a code; add a new one instead. HTTP status mapping lives in the web layer ({@code
 * ProblemDetailHandler}) so the domain stays free of HTTP concerns (Constitution IV).
 */
public enum ErrorCode {
  VALIDATION_FAILED,
  UNAUTHENTICATED,
  FORBIDDEN,
  NOT_FOUND,
  EMAIL_TAKEN,
  PLAN_READ_ONLY,
  FOOD_READ_ONLY,
  PROFILE_INCOMPLETE,
  PROFILE_INELIGIBLE,
  PLAN_INFEASIBLE,
  SWAP_REJECTED,
  GENERATION_LIMIT,
  RATE_LIMITED,
  AI_UNAVAILABLE,
  INTERNAL
}
