import { useCallback, useState } from 'react'
import { toApiError, type ApiError } from '../lib/errors'

/**
 * Holds the last API error of a component as a user-safe message.
 *
 * const { error, report, clear } = useApiError()
 * try { await api.post(...) } catch (e) { report(e) }
 */
export function useApiError() {
  const [apiError, setApiError] = useState<ApiError | null>(null)

  const report = useCallback((err: unknown) => {
    const normalized = toApiError(err)
    setApiError(normalized)
    return normalized
  }, [])

  const clear = useCallback(() => setApiError(null), [])

  return { error: apiError?.message ?? '', apiError, report, clear }
}
