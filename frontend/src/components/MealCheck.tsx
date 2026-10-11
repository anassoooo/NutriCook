import { useState } from 'react'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { DietPlan, Meal, ProgressEntry } from '../types'
import { CheckIcon } from './Icons'

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
    <div className="flex flex-col items-end gap-1">
      <button
        type="button"
        role="checkbox"
        aria-checked={meal.completed}
        aria-label={`I ate ${meal.name.toLowerCase()}`}
        disabled={busy}
        onClick={toggle}
        className={`inline-flex items-center gap-2 rounded-full border px-3.5 py-1.5 text-sm font-semibold transition-colors disabled:opacity-50 ${
          meal.completed
            ? 'border-ink bg-ink text-chalk'
            : 'border-line bg-paper text-ink hover:border-ink'
        }`}
      >
        <span
          aria-hidden
          className={`grid h-4 w-4 place-items-center rounded-full border ${meal.completed ? 'border-chalk' : 'border-ink-faint'}`}
        >
          {meal.completed && <CheckIcon size={11} strokeWidth={3} />}
        </span>
        {meal.completed ? 'Eaten' : 'Mark as eaten'}
      </button>
      {error && <p role="alert" className="max-w-[16rem] text-right text-xs text-alert">{error}</p>}
    </div>
  )
}
