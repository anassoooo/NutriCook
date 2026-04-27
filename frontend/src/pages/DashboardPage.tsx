import { useEffect, useState, Suspense, lazy } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { DietPlan, UserProfile } from '../types'
import MacroRing from '../components/MacroRing'

const NutritionOrb = lazy(() => import('../components/3d/NutritionOrb'))

function greeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 17) return 'Good afternoon'
  return 'Good evening'
}

const QUICK = [
  { to: '/plan',         emoji: '🥗', label: 'Diet Plan',         color: 'from-emerald-400 to-teal-500'  },
  { to: '/progress',     emoji: '📊', label: 'Log Progress',      color: 'from-blue-400 to-indigo-500'   },
  { to: '/achievements', emoji: '🏆', label: 'Achievements',      color: 'from-amber-400 to-orange-500'  },
  { to: '/restaurants',  emoji: '📍', label: 'Find Restaurants',  color: 'from-rose-400 to-pink-500'     },
]

export default function DashboardPage() {
  const { userId } = useAuth()
  const [plan,      setPlan]      = useState<DietPlan | null>(null)
  const [profile,   setProfile]   = useState<UserProfile | null>(null)
  const [generating,setGenerating]= useState(false)
  const [error,     setError]     = useState('')

  useEffect(() => {
    if (!userId) return
    api.get<UserProfile>(`/users/${userId}/profile`).then(r => setProfile(r.data)).catch(() => {})
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
    } catch { setError('Could not generate plan. Complete your profile first.') }
    finally   { setGenerating(false) }
  }

  const prot  = Math.round(plan?.totalProteinG ?? 0)
  const carbs = Math.round(plan?.totalCarbsG   ?? 0)
  const fat   = Math.round(plan?.totalFatG     ?? 0)
  const cal   = plan?.totalCalories ?? 0
  const calPct = Math.min(cal / 2200, 1)

  return (
    <div className="space-y-6 animate-fade-in">

      {/* ── Hero banner ───────────────────────── */}
      <div className="relative rounded-3xl overflow-hidden text-white"
           style={{ background: 'linear-gradient(135deg,#065f46 0%,#047857 40%,#0d9488 100%)' }}>

        {/* subtle pattern */}
        <div className="absolute inset-0 opacity-10"
             style={{ backgroundImage: 'radial-gradient(circle at 20% 80%, white 1px, transparent 1px), radial-gradient(circle at 80% 20%, white 1px, transparent 1px)', backgroundSize: '40px 40px' }} />

        <div className="relative z-10 p-7 flex items-center justify-between gap-4 flex-wrap">
          <div>
            <p className="text-emerald-200 text-sm font-medium mb-1">
              {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })}
            </p>
            <h1 className="text-3xl font-black tracking-tight">
              {greeting()}{profile?.firstName ? `, ${profile.firstName}` : ''} 👋
            </h1>
            {plan ? (
              <p className="text-emerald-200 mt-1 text-sm">
                Today's plan is ready · <span className="text-white font-bold">{cal.toLocaleString()} kcal</span>
              </p>
            ) : (
              <p className="text-emerald-200 mt-1 text-sm">Generate your personalised plan to get started.</p>
            )}
          </div>

          {/* Orb */}
          <div className="w-32 h-32 rounded-2xl overflow-hidden shrink-0 shadow-lg"
               style={{ background: 'rgba(255,255,255,.08)' }}>
            <Suspense fallback={null}>
              <NutritionOrb pct={calPct} />
            </Suspense>
          </div>
        </div>

        {/* Calorie bar */}
        {plan && (
          <div className="relative z-10 px-7 pb-5">
            <div className="flex justify-between text-xs text-emerald-200 mb-1.5">
              <span>Calories consumed</span>
              <span className="text-white font-semibold">{cal} / 2,200 kcal</span>
            </div>
            <div className="h-2 bg-white/20 rounded-full overflow-hidden">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${calPct * 100}%` }}
                transition={{ duration: 1.2, ease: [.4,0,.2,1] }}
                className="h-full rounded-full bg-white"
              />
            </div>
          </div>
        )}

        {!plan && (
          <div className="relative z-10 px-7 pb-6">
            <button onClick={generate} disabled={generating} className="btn-primary bg-white/20 hover:bg-white/30 border border-white/30 shadow-none">
              {generating
                ? <><svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>Generating…</>
                : '✨ Generate Today\'s Plan'}
            </button>
          </div>
        )}
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-2xl px-4 py-3">
          <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          {error}
        </div>
      )}

      {/* ── Macro rings ───────────────────────── */}
      {plan && (
        <div className="card">
          <p className="section-label mb-5">Today's macros</p>
          <div className="flex justify-around items-center">
            <MacroRing label="Protein" value={prot}  max={180} color="#3b82f6" />
            <MacroRing label="Carbs"   value={carbs} max={320} color="#f59e0b" />
            <MacroRing label="Fat"     value={fat}   max={90}  color="#ef4444" />
            <MacroRing label="Calories" value={cal}  max={2200} color="#10b981" unit="kcal" size={96} strokeWidth={10} />
          </div>
        </div>
      )}

      {/* ── Quick actions ─────────────────────── */}
      <div>
        <p className="section-label mb-3">Quick access</p>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {QUICK.map(({ to, emoji, label, color }, i) => (
            <motion.div
              key={to}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.07 }}
            >
              <Link to={to} className="group block">
                <div className={`relative rounded-2xl p-5 bg-gradient-to-br ${color} text-white overflow-hidden transition-all duration-200 group-hover:shadow-lg group-hover:-translate-y-1`}>
                  <div className="absolute -right-3 -bottom-3 text-5xl opacity-20 select-none">{emoji}</div>
                  <span className="text-2xl mb-3 block">{emoji}</span>
                  <p className="text-sm font-bold leading-tight">{label}</p>
                </div>
              </Link>
            </motion.div>
          ))}
        </div>
      </div>

      {/* ── Meals preview ─────────────────────── */}
      {plan && plan.meals.length > 0 && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <p className="section-label">Today's meals</p>
            <Link to="/plan" className="text-xs font-semibold text-brand-600 hover:text-brand-700">
              View all →
            </Link>
          </div>
          <div className="space-y-2">
            {[...plan.meals]
              .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
              .slice(0, 4)
              .map(meal => (
                <div key={meal.id} className="flex items-center justify-between py-2 border-b border-slate-50 last:border-0">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-xl bg-slate-100 flex items-center justify-center text-base">
                      {{ BREAKFAST:'🌅', MORNING_SNACK:'🍎', LUNCH:'🍽️', AFTERNOON_SNACK:'🥜', DINNER:'🌙' }[meal.mealType] ?? '🍴'}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-slate-900 leading-none">{meal.name}</p>
                      <p className="text-xs text-slate-400 mt-0.5">{meal.mealType.replace('_', ' ')}</p>
                    </div>
                  </div>
                  <span className="text-sm font-bold text-slate-700">{meal.calories} kcal</span>
                </div>
              ))}
          </div>
        </div>
      )}

      {/* ── AI explanation ────────────────────── */}
      {plan?.aiExplanation && (
        <div className="rounded-2xl p-6 border border-emerald-200"
             style={{ background: 'linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%)' }}>
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
