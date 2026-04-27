import { useEffect, useState, Suspense, lazy } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { DietPlan, UserProfile, ProgressEntry } from '../types'

const NutritionOrb = lazy(() => import('../components/3d/NutritionOrb'))

function greeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 17) return 'Good afternoon'
  return 'Good evening'
}

const MEAL_ICONS: Record<string, string> = {
  BREAKFAST: '🌅', MORNING_SNACK: '🍎', LUNCH: '🍽️', AFTERNOON_SNACK: '🥜', DINNER: '🌙',
}

export default function DashboardPage() {
  const { userId } = useAuth()
  const [plan,       setPlan]       = useState<DietPlan | null>(null)
  const [profile,    setProfile]    = useState<UserProfile | null>(null)
  const [todayEntry, setTodayEntry] = useState<ProgressEntry | null>(null)
  const [generating, setGenerating] = useState(false)
  const [error,      setError]      = useState('')

  useEffect(() => {
    if (!userId) return
    api.get<UserProfile>(`/users/${userId}/profile`).then(r => setProfile(r.data)).catch(() => {})
    api.get<DietPlan[]>(`/users/${userId}/diet-plans`).then(r => {
      const active = r.data.find(p => p.status === 'ACTIVE')
      if (active) setPlan(active)
    }).catch(() => {})

    const today = new Date().toISOString().slice(0, 10)
    api.get<ProgressEntry[]>(`/users/${userId}/progress/weekly?start=${today}&end=${today}`)
       .then(r => { if (r.data.length > 0) setTodayEntry(r.data[0]) })
       .catch(() => {})
  }, [userId])

  const generate = async () => {
    if (!userId) return
    setGenerating(true); setError('')
    try {
      const today = new Date().toISOString().slice(0, 10)
      setPlan((await api.post<DietPlan>(`/users/${userId}/diet-plans/generate?date=${today}`)).data)
    } catch { setError('Could not generate plan. Complete your profile first.') }
    finally   { setGenerating(false) }
  }

  const cal            = plan?.totalCalories ?? 0
  const calPct         = Math.min(cal / 2200, 1)
  const profileComplete = !!(profile?.weightKg && profile?.heightCm)

  const planned = plan?.totalCalories ?? null
  const actual  = todayEntry?.caloriesConsumed ?? null
  const calDiff = planned && actual ? actual - planned : null

  return (
    <div className="space-y-6 animate-fade-in">

      {/* Profile incomplete banner */}
      {profile && !profileComplete && (
        <motion.div
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          className="banner-warning"
        >
          <div className="w-9 h-9 rounded-xl bg-amber-100 flex items-center justify-center shrink-0 text-lg">👤</div>
          <div className="flex-1">
            <p className="font-bold text-amber-800 text-sm">Complete your profile</p>
            <p className="text-amber-700 text-xs mt-0.5">Add your weight and height so AI can build a plan tailored to you.</p>
          </div>
          <Link to="/profile" className="btn-primary text-xs px-4 py-2 shrink-0"
                style={{ background: 'linear-gradient(135deg,#d97706,#b45309)' }}>
            Set up →
          </Link>
        </motion.div>
      )}

      {/* Hero banner */}
      <div className="relative rounded-3xl overflow-hidden text-white"
           style={{ background: 'linear-gradient(135deg,#064e3b 0%,#065f46 35%,#047857 65%,#0d9488 100%)' }}>
        <div className="absolute inset-0"
             style={{ backgroundImage: 'radial-gradient(circle at 15% 85%, rgba(255,255,255,.06) 0%, transparent 50%), radial-gradient(circle at 85% 15%, rgba(255,255,255,.06) 0%, transparent 50%)' }} />

        <div className="relative z-10 p-7 flex items-center justify-between gap-4 flex-wrap">
          <div>
            <p className="text-emerald-300 text-sm font-medium mb-1 flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 inline-block animate-pulse" />
              {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })}
            </p>
            <h1 className="text-3xl font-black tracking-tight">
              {greeting()}{profile?.firstName ? `, ${profile.firstName}` : ''} 👋
            </h1>
            {plan ? (
              <p className="text-emerald-200 mt-1 text-sm">
                Today's plan · <span className="text-white font-bold">{cal.toLocaleString()} kcal</span>
                <span className="text-emerald-300"> · {plan.meals.length} meals</span>
              </p>
            ) : (
              <p className="text-emerald-300 mt-1 text-sm">Generate your personalised plan to get started.</p>
            )}
          </div>

          <div className="w-32 h-32 rounded-2xl overflow-hidden shrink-0 shadow-lg"
               style={{ background: 'rgba(255,255,255,.08)' }}>
            <Suspense fallback={null}>
              <NutritionOrb pct={calPct} />
            </Suspense>
          </div>
        </div>

        {plan && (
          <div className="relative z-10 px-7 pb-5">
            <div className="flex justify-between text-xs text-emerald-300 mb-1.5">
              <span>Daily calories</span>
              <span className="text-white font-semibold">{cal.toLocaleString()} / 2,200 kcal</span>
            </div>
            <div className="h-2 bg-white/15 rounded-full overflow-hidden">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${calPct * 100}%` }}
                transition={{ duration: 1.3, ease: [.4,0,.2,1] }}
                className="h-full rounded-full bg-white"
              />
            </div>
          </div>
        )}

        {!plan && (
          <div className="relative z-10 px-7 pb-6">
            <button onClick={generate} disabled={generating}
                    className="btn-primary bg-white/20 hover:bg-white/30 border border-white/30 shadow-none">
              {generating
                ? <><svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>Generating…</>
                : '✨ Generate Today\'s Plan'}
            </button>
          </div>
        )}
      </div>

      {error && (
        <div className="banner-error">
          <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          {error}
        </div>
      )}

      {/* Planned vs Actual */}
      {plan && (
        <div className="card !p-0 overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-50"
               style={{ background: 'linear-gradient(135deg,#f8fafc 0%,#f1f5f9 100%)' }}>
            <p className="section-label">Today at a glance</p>
            <p className="text-xs text-slate-400 mt-0.5">Planned from your diet plan vs. what you've logged</p>
          </div>

          <div className="p-6 space-y-4">
            {/* Calorie comparison */}
            <div>
              <div className="flex justify-between items-center mb-1.5">
                <span className="text-sm font-semibold text-slate-700">Calories</span>
                <div className="flex items-center gap-3 text-xs font-semibold">
                  <span className="text-slate-500">Planned <span className="text-slate-800">{planned?.toLocaleString() ?? '—'} kcal</span></span>
                  <span className="text-slate-300">|</span>
                  <span className="text-slate-500">Logged <span className="text-slate-800">{actual?.toLocaleString() ?? '—'} kcal</span></span>
                  {calDiff !== null && (
                    <span className={`px-2 py-0.5 rounded-full text-[11px] ${
                      Math.abs(calDiff) < 150
                        ? 'bg-green-50 text-green-700'
                        : calDiff > 0
                        ? 'bg-red-50 text-red-600'
                        : 'bg-blue-50 text-blue-600'
                    }`}>
                      {calDiff > 0 ? '+' : ''}{calDiff} kcal
                    </span>
                  )}
                </div>
              </div>

              <div className="relative h-3 bg-slate-100 rounded-full overflow-hidden">
                {/* planned bar */}
                <div className="absolute inset-0 rounded-full bg-emerald-200"
                     style={{ width: `${Math.min((planned ?? 0) / 2500, 1) * 100}%` }} />
                {/* actual bar */}
                {actual && (
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${Math.min(actual / 2500, 1) * 100}%` }}
                    transition={{ duration: 1, ease: [.4,0,.2,1] }}
                    className="absolute inset-y-0 left-0 rounded-full"
                    style={{ background: actual > (planned ?? 0) * 1.1 ? '#ef4444' : '#16a34a' }}
                  />
                )}
              </div>
              {!actual && (
                <p className="text-xs text-slate-400 mt-1">
                  Log today's calories in{' '}
                  <Link to="/progress" className="text-brand-600 font-semibold hover:underline">Progress</Link>
                  {' '}to compare.
                </p>
              )}
            </div>

            {/* Quick macro pills */}
            <div className="grid grid-cols-3 gap-2 pt-1">
              {[
                { label: 'Protein',  value: `${Math.round(plan.totalProteinG)}g`, color: '#3b82f6', bg: '#eff6ff' },
                { label: 'Carbs',    value: `${Math.round(plan.totalCarbsG)}g`,   color: '#f59e0b', bg: '#fffbeb' },
                { label: 'Fat',      value: `${Math.round(plan.totalFatG)}g`,     color: '#ef4444', bg: '#fef2f2' },
              ].map(({ label, value, color, bg }) => (
                <div key={label} className="text-center py-3 rounded-xl" style={{ background: bg }}>
                  <p className="text-lg font-black" style={{ color }}>{value}</p>
                  <p className="text-xs text-slate-500 font-semibold mt-0.5">{label}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Meals preview */}
      {plan && plan.meals.length > 0 && (
        <div className="card !p-0 overflow-hidden">
          <div className="px-6 py-4 flex items-center justify-between border-b border-slate-50"
               style={{ background: 'linear-gradient(135deg,#f8fafc 0%,#f1f5f9 100%)' }}>
            <p className="section-label">Today's meals</p>
            <Link to="/plan" className="text-xs font-semibold text-brand-600 hover:text-brand-700 transition-colors">
              View full plan →
            </Link>
          </div>
          <div className="divide-y divide-slate-50">
            {[...plan.meals]
              .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
              .slice(0, 4)
              .map((meal, i) => (
                <motion.div
                  key={meal.id}
                  initial={{ opacity: 0, x: -8 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.05 }}
                  className="flex items-center justify-between px-6 py-3.5 hover:bg-slate-50/60 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-slate-100 flex items-center justify-center text-base shrink-0">
                      {MEAL_ICONS[meal.mealType] ?? '🍴'}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-slate-900 leading-none">{meal.name}</p>
                      <p className="text-xs text-slate-400 mt-0.5">{meal.mealType.replace('_', ' ')}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="text-sm font-bold text-slate-800">{meal.calories}</span>
                    <span className="text-xs text-slate-400 ml-1">kcal</span>
                  </div>
                </motion.div>
              ))}
          </div>
        </div>
      )}

      {/* AI explanation */}
      {plan?.aiExplanation && (
        <div className="card-brand">
          <div className="flex items-center gap-2.5 mb-3">
            <div className="w-7 h-7 rounded-lg flex items-center justify-center text-sm"
                 style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)' }}>
              <span className="text-white">✨</span>
            </div>
            <p className="section-label text-emerald-700">AI Dietitian</p>
          </div>
          <p className="text-slate-700 text-sm leading-relaxed">{plan.aiExplanation}</p>
        </div>
      )}
    </div>
  )
}
