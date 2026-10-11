import { useState } from 'react'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { DietPlan, Meal, ProgressEntry } from '../types'

interface CompletionResponse {
  plan: DietPlan
  progress: ProgressEntry
}

/**
 * "I ate this" toggle (BR-PRG-05). The server counts checked meals and, unless the user typed
 * their own number, adds up their calories for the day.
 */
export default function MealCheck({
  planId,
  meal,
  onChanged,
}: {
  planId: number
  meal: Meal
  onChanged: (plan: DietPlan, progress: ProgressEntry) => void
}) {
  const { userId } = useAuth()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const toggle = async () => {
    setBusy(true)
    setError('')
    try {
      const { data } = await api.put<CompletionResponse>(
        `/users/${userId}/diet-plans/${planId}/meals/${meal.id}/completion`,
        { completed: !meal.completed },
      )
      onChanged(data.plan, data.progress)
    } catch (err) {
      const e = toApiError(err)
      setError(e.detail ?? e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex items-center gap-3 px-5 py-2.5" style={{ background: 'rgba(0,0,0,0.18)' }}>
      <button
        type="button"
        role="checkbox"
        aria-checked={meal.completed}
        aria-label={`I ate ${meal.name.toLowerCase()}`}
        disabled={busy}
        onClick={toggle}
        className="flex items-center gap-2 text-xs font-semibold disabled:opacity-50"
        style={{ color: meal.completed ? '#6ee7b7' : 'rgba(255,255,255,0.7)' }}
      >
        <span
          aria-hidden
          className="w-4 h-4 rounded border flex items-center justify-center"
          style={{
            borderColor: meal.completed ? '#10b981' : 'rgba(255,255,255,0.35)',
            background: meal.completed ? '#10b981' : 'transparent',
          }}
        >
          {meal.completed && (
            <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
            </svg>
          )}
        </span>
        {meal.completed ? 'Eaten' : 'Mark as eaten'}
      </button>
      {error && (
        <p role="alert" className="text-xs text-red-300">
          {error}
        </p>
      )}
    </div>
  )
}
