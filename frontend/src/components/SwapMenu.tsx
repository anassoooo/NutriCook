import { useState } from 'react'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { DietPlan, FoodRole } from '../types'

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
        <button type="button" onClick={open} disabled={busy} className="text-xs font-semibold underline underline-offset-2 disabled:opacity-50" style={{ color: 'rgba(134,239,172,0.9)' }}>
          {busy ? 'Loading…' : 'Swap a food'}
        </button>
        {error && <p role="alert" className="text-xs mt-1 text-red-300">{error}</p>}
      </div>
    )
  }

  const current = groups.find((g) => g.role === role)
  return (
    <div className="rounded-xl p-3 space-y-2" style={{ background: 'rgba(255,255,255,0.05)' }}>
      <div className="flex gap-1.5 flex-wrap" role="tablist" aria-label="Food to replace">
        {groups.map((g) => (
          <button
            key={g.role}
            type="button"
            role="tab"
            aria-selected={g.role === role}
            onClick={() => setRole(g.role)}
            className="text-xs px-2 py-1 rounded-lg"
            style={g.role === role ? { background: 'rgba(16,185,129,0.25)', color: '#6ee7b7' } : { color: 'rgba(255,255,255,0.6)' }}
          >
            {ROLE_LABEL[g.role]}
          </button>
        ))}
        <button type="button" onClick={() => setGroups(null)} className="text-xs ml-auto" style={{ color: 'rgba(255,255,255,0.5)' }}>
          Close
        </button>
      </div>
      {current && current.options.length === 0 && (
        <p className="text-xs" style={{ color: 'rgba(255,255,255,0.6)' }}>No safe alternative for this food.</p>
      )}
      <ul className="space-y-1" aria-label="Alternatives">
        {current?.options.map((o) => (
          <li key={o.foodId}>
            <button
              type="button"
              disabled={busy}
              onClick={() => swap(o.foodId)}
              className="w-full flex justify-between text-sm px-2 py-1 rounded-lg hover:bg-white/10 disabled:opacity-50"
            >
              <span style={{ color: 'rgba(255,255,255,0.85)' }}>{o.foodName}</span>
              <span className="tabular-nums" style={{ color: 'rgba(255,255,255,0.55)' }}>
                {Math.round(o.grams)} g · {Math.round(o.kcal)} kcal
              </span>
            </button>
          </li>
        ))}
      </ul>
      {error && <p role="alert" className="text-xs text-red-300">{error}</p>}
    </div>
  )
}
