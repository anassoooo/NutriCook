import { useEffect, useState } from 'react'
import api from '../lib/api'
import { toApiError, type ApiError } from '../lib/errors'
import type { Targets } from '../types'

/**
 * The signed-in user's daily targets from GET /users/{id}/targets (BR-PRF-08).
 * `error` is set (e.g. PROFILE_INCOMPLETE) when targets cannot be computed yet.
 */
export function useTargets(userId: number | null) {
  const [targets, setTargets] = useState<Targets | null>(null)
  const [error, setError] = useState<ApiError | null>(null)

  useEffect(() => {
    if (!userId) return
    let cancelled = false
    api
      .get<Targets>(`/users/${userId}/targets`)
      .then((r) => {
        if (!cancelled) setTargets(r.data)
      })
      .catch((e) => {
        if (!cancelled) setError(toApiError(e))
      })
    return () => {
      cancelled = true
    }
  }, [userId])

  return { targets, error }
}
