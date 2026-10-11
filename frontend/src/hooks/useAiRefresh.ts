import { useEffect } from 'react'
import api from '../lib/api'
import type { DietPlan } from '../types'

/** Delays (ms) at which a plan whose AI text is still being written is fetched again. */
export const AI_REFRESH_DELAYS = [3000, 8000]

/**
 * The AI explanation and meal tips are written after the plan is saved (BR-LIF-07). While a plan's
 * aiStatus is PENDING, fetch it again a couple of times so the text appears without a reload.
 */
export function useAiRefresh(
  userId: number | null,
  plan: DietPlan | null,
  setPlan: (plan: DietPlan) => void,
) {
  const planId = plan?.id
  const pending = plan?.aiStatus === 'PENDING'

  useEffect(() => {
    if (!userId || !planId || !pending) return
    const timers = AI_REFRESH_DELAYS.map((delay) =>
      setTimeout(() => {
        api
          .get<DietPlan>(`/users/${userId}/diet-plans/${planId}`)
          .then((r) => {
            if (r.data.aiStatus !== 'PENDING') setPlan(r.data)
          })
          .catch(() => {})
      }, delay),
    )
    return () => timers.forEach(clearTimeout)
  }, [userId, planId, pending, setPlan])
}
