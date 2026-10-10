import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { ApiError, ERROR_CODES, ERROR_MESSAGES, shouldRedirectToLogin, toApiError } from '../lib/errors'

function axiosError(status?: number, data?: unknown): AxiosError {
  const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig
  const response =
    status === undefined ? undefined : { status, data, statusText: '', headers: {}, config }
  return new AxiosError('Request failed', undefined, config, {}, response)
}

describe('error messages', () => {
  it('defines a non-empty message for every API error code', () => {
    for (const code of ERROR_CODES) {
      expect(ERROR_MESSAGES[code], code).toBeTruthy()
    }
  })
})

describe('toApiError', () => {
  it('reads code, detail, field errors and extra properties from a ProblemDetail body', () => {
    const err = toApiError(
      axiosError(422, {
        type: 'urn:nutricook:problem:profile_incomplete',
        title: 'Profile incomplete',
        status: 422,
        detail: 'Add your date of birth.',
        code: 'PROFILE_INCOMPLETE',
        missingFields: ['dateOfBirth'],
      }),
    )

    expect(err).toBeInstanceOf(ApiError)
    expect(err.code).toBe('PROFILE_INCOMPLETE')
    expect(err.status).toBe(422)
    expect(err.message).toBe(ERROR_MESSAGES.PROFILE_INCOMPLETE)
    expect(err.detail).toBe('Add your date of birth.')
    expect(err.properties.missingFields).toEqual(['dateOfBirth'])
  })

  it('exposes validation field errors', () => {
    const err = toApiError(
      axiosError(400, {
        code: 'VALIDATION_FAILED',
        errors: [{ field: 'password', message: 'Password must be at least 8 characters' }],
      }),
    )
    expect(err.fieldErrors).toEqual([{ field: 'password', message: 'Password must be at least 8 characters' }])
  })

  it('never shows raw server text as the message', () => {
    const err = toApiError(axiosError(500, { detail: 'NullPointerException at Foo.java:42' }))
    expect(err.code).toBe('INTERNAL')
    expect(err.message).not.toContain('NullPointerException')
  })

  it('maps an unknown 4xx code to UNKNOWN', () => {
    expect(toApiError(axiosError(418, { code: 'TEAPOT' })).code).toBe('UNKNOWN')
  })

  it('maps a missing response to NETWORK', () => {
    expect(toApiError(axiosError()).code).toBe('NETWORK')
  })

  it('maps non-HTTP failures to UNKNOWN and passes ApiError through', () => {
    expect(toApiError(new TypeError('boom')).code).toBe('UNKNOWN')
    const existing = new ApiError({ code: 'FORBIDDEN', status: 403 })
    expect(toApiError(existing)).toBe(existing)
  })
})

describe('shouldRedirectToLogin', () => {
  it('redirects when a signed-in request gets 401', () => {
    expect(shouldRedirectToLogin(401, '/users/1/profile', true)).toBe(true)
  })

  it('does not redirect on a failed login, so the error message stays visible', () => {
    expect(shouldRedirectToLogin(401, '/auth/login', false)).toBe(false)
    expect(shouldRedirectToLogin(401, '/auth/login', true)).toBe(false)
  })

  it('does not redirect for anonymous requests or other statuses', () => {
    expect(shouldRedirectToLogin(401, '/users/1/profile', false)).toBe(false)
    expect(shouldRedirectToLogin(403, '/users/1/profile', true)).toBe(false)
  })
})
