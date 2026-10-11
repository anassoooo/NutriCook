import { useState } from 'react'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { DietPlan, FoodRole } from '../types'
import { ROLE_COLORS } from '../lib/roles'
import { SwapIcon } from './Icons'

interface Option {
  foodId: number
  foodName: string
  grams: number
  kcal: number
  proteinG: number
}

interface Alternatives {
  role: FoodRole
  options: Option[]
}

const ROLE_LABEL: Record<FoodRole, string> = {
  PROTEIN: 'Protein',
  CARB: 'Carbs',
  PRODUCE: 'Fruit & veg',
  FAT: 'Healthy fat',
}

/**
 * Manual swap (FR-003): lists safe replacements for each food in a meal, at the portion they would
 * get, and swaps on click. Uses the same rules as AI swaps; the server re-validates everything.
 */
export default function SwapMenu({
  planId,
  mealId,
  onSwapped,
}: {
  planId: number
  mealId: number
  onSwapped: (plan: DietPlan) => void
}) {
  const { userId } = useAuth()
  const [groups, setGroups] = useState<Alternatives[] | null>(null)
  const [role, setRole] = useState<FoodRole | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const base = `/users/${userId}/diet-plans/${planId}/meals/${mealId}`

  const open = async () => {
    setError('')
    setBusy(true)
    try {
      const { data } = await api.get<Alternatives[]>(`${base}/alternatives`)
      setGroups(data)
      setRole(data[0]?.role ?? null)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setBusy(false)
    }
  }

  const swap = async (foodId: number) => {
    setError('')
    setBusy(true)
    try {
      const { data } = await api.post<DietPlan>(`${base}/swap`, { foodId })
      setGroups(null)
      onSwapped(data)
    } catch (err) {
      const e = toApiError(err)
      // SWAP_REJECTED / PLAN_READ_ONLY carry a server explanation worth showing as-is
      setError(e.detail ?? e.message)
    } finally {
      setBusy(false)
    }
  }

  if (!groups) {
    return (
      <div>
        <button type="button" onClick={open} disabled={busy} className="btn-ghost -ml-3">
          <SwapIcon size={17} />
          {busy ? 'Loading…' : 'Swap a food'}
        </button>
        {error && <p role="alert" className="mt-1 text-sm text-alert">{error}</p>}
      </div>
    )
  }

  const current = groups.find((g) => g.role === role)
  return (
    <div className="rounded-ctl border border-line bg-chalk p-3">
      <div className="flex flex-wrap items-center gap-1.5" role="tablist" aria-label="Food to replace">
        {groups.map((g) => (
          <button
            key={g.role}
            type="button"
            role="tab"
            aria-selected={g.role === role}
            onClick={() => setRole(g.role)}
            className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-semibold transition-colors ${
              g.role === role ? 'bg-ink text-chalk' : 'text-ink-soft hover:bg-wash'
            }`}
          >
            <span className="h-2 w-2 rounded-full" style={{ background: ROLE_COLORS[g.role] }} aria-hidden />
            {g.role === 'FAT' ? 'Fat' : ROLE_LABEL[g.role]}
          </button>
        ))}
        <button type="button" onClick={() => setGroups(null)} className="ml-auto text-sm font-semibold text-ink-soft hover:text-ink">
          Close
        </button>
      </div>
      {current && current.options.length === 0 && (
        <p className="mt-3 text-sm text-ink-soft">No safe alternative for this food.</p>
      )}
      <ul className="mt-2 divide-y divide-line" aria-label="Alternatives">
        {current?.options.map((o) => (
          <li key={o.foodId}>
            <button
              type="button"
              disabled={busy}
              onClick={() => swap(o.foodId)}
              className="flex w-full items-baseline justify-between gap-4 rounded-md px-2 py-2 text-left text-sm hover:bg-wash disabled:opacity-50"
            >
              <span className="text-ink">{o.foodName}</span>
              <span className="shrink-0 text-ink-soft num">
                {Math.round(o.grams)} g · {Math.round(o.kcal)} kcal
              </span>
            </button>
          </li>
        ))}
      </ul>
      {error && <p role="alert" className="mt-2 text-sm text-alert">{error}</p>}
    </div>
  )
}
