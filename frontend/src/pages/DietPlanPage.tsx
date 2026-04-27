import { useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { DietPlan, Meal } from '../types'
import MacroRing from '../components/MacroRing'

const MEAL_META: Record<string, { icon: string; gradient: string; badge: string }> = {
  BREAKFAST:       { icon: '🌅', gradient: 'from-amber-400 to-orange-500',    badge: 'badge-amber'  },
  MORNING_SNACK:   { icon: '🍎', gradient: 'from-emerald-400 to-teal-500',    badge: 'badge-green'  },
  LUNCH:           { icon: '🍽️', gradient: 'from-blue-400 to-cyan-500',       badge: 'badge-blue'   },
  AFTERNOON_SNACK: { icon: '🥜', gradient: 'from-violet-400 to-purple-500',   badge: 'badge-purple' },
  DINNER:          { icon: '🌙', gradient: 'from-slate-500 to-indigo-700',    badge: 'badge-gray'   },
}

function MealCard({ meal, index }: { meal: Meal; index: number }) {
  const [open, setOpen] = useState(false)
  const meta = MEAL_META[meal.mealType] ?? { icon: '🍴', gradient: 'from-slate-400 to-slate-600', badge: 'badge-gray' }

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.06 }}
      className="card overflow-hidden !p-0"
    >
      {/* Gradient header */}
      <button
        onClick={() => setOpen(o => !o)}
        className={`w-full text-left bg-gradient-to-r ${meta.gradient} px-5 py-4 flex items-center justify-between`}
      >
        <div className="flex items-center gap-3">
          <span className="text-2xl filter drop-shadow">{meta.icon}</span>
          <div>
            <p className="font-bold text-white text-base leading-tight">{meal.name}</p>
            <p className="text-white/70 text-xs mt-0.5">
              {meal.mealType.replace('_', ' ')} · {meal.calories} kcal
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="text-right hidden sm:block">
            <p className="text-white/90 text-sm font-semibold">{Math.round(meal.proteinG ?? 0)}g protein</p>
            <p className="text-white/60 text-xs">{Math.round(meal.carbsG ?? 0)}g carbs · {Math.round(meal.fatG ?? 0)}g fat</p>
          </div>
          <svg
            className={`w-5 h-5 text-white/80 transition-transform duration-300 ${open ? 'rotate-180' : ''}`}
            fill="none" stroke="currentColor" viewBox="0 0 24 24"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7"/>
          </svg>
        </div>
      </button>

      {/* Expanded content */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.25 }}
            className="overflow-hidden"
          >
            <div className="px-5 py-5 space-y-5">
              {/* Macro rings */}
              <div className="flex justify-around">
                <MacroRing label="Protein" value={Math.round(meal.proteinG ?? 0)} max={60}  color="#3b82f6" size={76} strokeWidth={8} />
                <MacroRing label="Carbs"   value={Math.round(meal.carbsG   ?? 0)} max={120} color="#f59e0b" size={76} strokeWidth={8} />
                <MacroRing label="Fat"     value={Math.round(meal.fatG     ?? 0)} max={40}  color="#ef4444" size={76} strokeWidth={8} />
                <MacroRing label="Fiber"   value={Math.round(meal.fiberG   ?? 0)} max={15}  color="#10b981" size={76} strokeWidth={8} />
              </div>

              {/* AI tip */}
              {meal.aiSuggestion && (
                <div className="bg-slate-50 border border-slate-200 rounded-xl p-4">
                  <p className="text-[11px] font-bold text-slate-400 uppercase tracking-widest mb-1.5">AI Tip</p>
                  <p className="text-sm text-slate-700 leading-relaxed">{meal.aiSuggestion}</p>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  )
}

export default function DietPlanPage() {
  const { userId } = useAuth()
  const [plan,       setPlan]       = useState<DietPlan | null>(null)
  const [generating, setGenerating] = useState(false)
  const [error,      setError]      = useState('')

  useEffect(() => {
    if (!userId) return
    api.get<DietPlan[]>(`/users/${userId}/diet-plans`).then(r => {
      const active = r.data.find(p => p.status === 'ACTIVE')
      if (active) setPlan(active)
    }).catch(() => {})
  }, [userId])

  const generate = async () => {
    if (!userId) return
    setGenerating(true); setError('')
    try {
      const today = new Date().toISOString().slice(0, 10)
      setPlan((await api.post<DietPlan>(`/users/${userId}/diet-plans/generate?date=${today}`)).data)
    } catch { setError('Generation failed. Make sure your profile is complete.') }
    finally   { setGenerating(false) }
  }

  return (
    <div className="space-y-6 animate-fade-in">

      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <h1 className="page-title">Diet Plan</h1>
          <p className="page-subtitle">
            {plan
              ? `${plan.validForDate} · ${plan.totalCalories.toLocaleString()} kcal · ${plan.meals.length} meals`
              : 'Generate your AI-personalised meal plan'}
          </p>
        </div>
        <button onClick={generate} disabled={generating} className="btn-primary">
          {generating
            ? <><svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>Generating…</>
            : plan ? '↻ Regenerate' : '✨ Generate Plan'}
        </button>
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-2xl px-4 py-3">
          <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          {error}
        </div>
      )}

      {plan ? (
        <>
          {/* Daily macro overview */}
          <div className="card">
            <p className="section-label mb-5">Daily macros</p>
            <div className="flex justify-around items-center">
              <MacroRing label="Protein"  value={Math.round(plan.totalProteinG)} max={180} color="#3b82f6" />
              <MacroRing label="Carbs"    value={Math.round(plan.totalCarbsG)}   max={320} color="#f59e0b" />
              <MacroRing label="Fat"      value={Math.round(plan.totalFatG)}     max={90}  color="#ef4444" />
              <MacroRing label="Sugar"    value={Math.round(plan.totalSugarG)}   max={50}  color="#a855f7" />
            </div>
          </div>

          {/* Meals */}
          <div className="space-y-3">
            <p className="section-label">Your meals</p>
            {[...plan.meals]
              .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
              .map((meal, i) => <MealCard key={meal.id} meal={meal} index={i} />)}
          </div>

          {/* AI explanation */}
          {plan.aiExplanation && (
            <div className="rounded-2xl p-6 border border-emerald-200"
                 style={{ background: 'linear-gradient(135deg,#f0fdf4 0%,#ecfdf5 100%)' }}>
              <div className="flex items-center gap-2.5 mb-3">
                <div className="w-7 h-7 rounded-lg flex items-center justify-center"
                     style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)' }}>
                  <span className="text-white text-sm">✨</span>
                </div>
                <p className="section-label text-emerald-700">AI Dietitian Notes</p>
              </div>
              <p className="text-slate-700 text-sm leading-relaxed">{plan.aiExplanation}</p>
            </div>
          )}
        </>
      ) : generating ? (
        <div className="card text-center py-20">
          <div className="inline-flex items-center gap-3 text-brand-600 mb-3">
            <svg className="animate-spin w-8 h-8" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
          </div>
          <p className="font-bold text-slate-900">AI is crafting your plan…</p>
          <p className="text-slate-400 text-sm mt-1">Analysing your profile and generating personalised meals.</p>
        </div>
      ) : (
        <div className="rounded-2xl border-2 border-dashed border-slate-200 text-center py-24 bg-white">
          <div className="text-6xl mb-4">🥗</div>
          <p className="font-bold text-slate-900 text-lg">No plan yet</p>
          <p className="text-slate-400 text-sm mt-2 mb-6">Let AI build your personalised meal plan for today.</p>
          <button onClick={generate} className="btn-primary mx-auto">✨ Generate Plan</button>
        </div>
      )}
    </div>
  )
}
