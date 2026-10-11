import axios from 'axios'

/**
 * Business error codes returned by the API (docs/domain/business-rules.md §12).
 * Must stay in sync with backend `com.nutricook.domain.ErrorCode` (checked by ErrorCodeSyncTest).
 */
export const ERROR_CODES = [
  'VALIDATION_FAILED',
  'UNAUTHENTICATED',
  'FORBIDDEN',
  'NOT_FOUND',
  'EMAIL_TAKEN',
  'PLAN_READ_ONLY',
  'FOOD_READ_ONLY',
  'PROFILE_INCOMPLETE',
  'PROFILE_INELIGIBLE',
  'PLAN_INFEASIBLE',
  'SWAP_REJECTED',
  'GENERATION_LIMIT',
  'RATE_LIMITED',
  'AI_UNAVAILABLE',
  'MAP_UNAVAILABLE',
  'INTERNAL',
] as const

export type ErrorCode = (typeof ERROR_CODES)[number]

/** Client-side codes for failures that never reached the API. */
export type ClientErrorCode = 'NETWORK' | 'UNKNOWN'

/** The single place where error codes become words (Constitution VIII). */
export const ERROR_MESSAGES: Record<ErrorCode | ClientErrorCode, string> = {
  VALIDATION_FAILED: 'Some of the information is invalid. Please check the highlighted fields.',
  UNAUTHENTICATED: 'Your session has expired. Please sign in again.',
  FORBIDDEN: "You don't have access to this.",
  NOT_FOUND: "We couldn't find what you were looking for.",
  EMAIL_TAKEN: 'An account with this email already exists. Try signing in instead.',
  PLAN_READ_ONLY: 'This plan can no longer be changed.',
  FOOD_READ_ONLY: 'This food comes from the curated catalog file and cannot be edited here.',
  PROFILE_INCOMPLETE: 'Please complete your profile first. We need it to calculate your targets.',
  PROFILE_INELIGIBLE: 'Personalized meal plans are only available for adults (18–100).',
  PLAN_INFEASIBLE: "We couldn't build a safe plan with your current restrictions.",
  SWAP_REJECTED: "That swap isn't possible for your plan.",
  GENERATION_LIMIT: "You've reached today's plan generation limit. Try again tomorrow.",
  RATE_LIMITED: "You're sending messages too quickly. Please wait a moment.",
  AI_UNAVAILABLE: 'The AI assistant is unavailable right now. Your plan still works without it.',
  MAP_UNAVAILABLE: 'Nearby places are unavailable right now. Please try again in a minute.',
  INTERNAL: 'Something went wrong on our side. Please try again.',
  NETWORK: "Can't reach the server. Check your connection and try again.",
  UNKNOWN: 'Something went wrong. Please try again.',
}

export interface FieldError {
  field: string
  message: string
}

/** A normalized API failure. `message` is always safe to show to users. */
export class ApiError extends Error {
  readonly code: ErrorCode | ClientErrorCode
  readonly status: number | null
  /** Server-written explanation for the user, when the API provided one. */
  readonly detail: string | null
  readonly fieldErrors: FieldError[]
  /** Extra ProblemDetail properties, e.g. `missingFields`, `constraint`, `allowedValues`. */
  readonly properties: Record<string, unknown>

  constructor(init: {
    code: ErrorCode | ClientErrorCode
    status: number | null
    detail?: string | null
    fieldErrors?: FieldError[]
    properties?: Record<string, unknown>
  }) {
    super(ERROR_MESSAGES[init.code])
    this.name = 'ApiError'
    this.code = init.code
    this.status = init.status
    this.detail = init.detail ?? null
    this.fieldErrors = init.fieldErrors ?? []
    this.properties = init.properties ?? {}
  }
}

function isErrorCode(value: unknown): value is ErrorCode {
  return typeof value === 'string' && (ERROR_CODES as readonly string[]).includes(value)
}

/** Converts anything thrown by an API call into an {@link ApiError}. */
export function toApiError(err: unknown): ApiError {
  if (err instanceof ApiError) return err
  if (!axios.isAxiosError(err)) return new ApiError({ code: 'UNKNOWN', status: null })
  if (!err.response) return new ApiError({ code: 'NETWORK', status: null })

  const { status, data } = err.response
  const body: Record<string, unknown> = data && typeof data === 'object' ? (data as Record<string, unknown>) : {}
  const code = isErrorCode(body.code) ? body.code : status >= 500 ? 'INTERNAL' : 'UNKNOWN'
  const { errors, detail, ...properties } = body
  return new ApiError({
    code,
    status,
    detail: typeof detail === 'string' ? detail : null,
    fieldErrors: Array.isArray(errors) ? (errors as FieldError[]) : [],
    properties,
  })
}

/**
 * A 401 means the stored session is no longer valid, so send the user to sign in. Exceptions: the
 * auth endpoints themselves (a failed login must show its error) and requests made without a token.
 */
export function shouldRedirectToLogin(status: number | undefined, url: string | undefined, hadToken: boolean): boolean {
  return status === 401 && hadToken && !(url ?? '').startsWith('/auth/')
}

/**
 * Message for a failed plan generation. For PLAN_INFEASIBLE the server explains which restriction
 * or limit could not be met, which is more useful than the generic text.
 */
export function planErrorMessage(err: unknown): string {
  const e = toApiError(err)
  if (e.code === 'PLAN_INFEASIBLE' && e.detail) {
    const reason = typeof e.properties.reason === 'string' ? ` ${e.properties.reason}` : ''
    return `${e.detail}${reason}`
  }
  return e.message
}
