import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import api from '../lib/api'
import { planErrorMessage } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { DietPlan, Meal, UserProfile, ProgressEntry, SlotTarget, Targets } from '../types'
import ChatPanel from '../components/ChatPanel'
import MedicalDisclaimer from '../components/MedicalDisclaimer'
import PlanWarnings from '../components/PlanWarnings'
import MealComponents, { MealComponentsSummary } from '../components/MealComponents'
import SwapMenu from '../components/SwapMenu'
import MealCheck from '../components/MealCheck'
import DayPlate, { RoleKey } from '../components/DayPlate'
import { ROLE_COLORS } from '../lib/roles'
import { ChatIcon, ChevronIcon, RefreshIcon, SparkIcon } from '../components/Icons'
import { localIsoDate } from '../lib/progressRules'
import { useAiRefresh } from '../hooks/useAiRefresh'

const FIELD_LABELS: Record<string, string> = {
  dateOfBirth: 'date of birth',
  gender: 'gender',
  weightKg: 'weight',
  heightCm: 'height',
  activityLevel: 'activity level',
  healthGoal: 'goal',
}

function greeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 18) return 'Good afternoon'
  return 'Good evening'
}

/** One macro of the day against its target. Numbers come from the API (BR-PRF-08). */
function MacroLine({ label, value, max, color }: { label: string; value: number; max: number; color: string }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0
  return (
    <div data-tooltip={`${value} g / ${max} g (${pct}%)`}>
      <div className="flex items-baseline justify-between text-sm">
        <span className="font-semibold">{label}</span>
        <span className="text-ink-soft num">
          <span className="text-ink">{value}</span> / {max} g
        </span>
      </div>
      <div className="mt-1.5 h-1.5 rounded-full bg-wash" aria-hidden>
        <div className="h-full rounded-full" style={{ width: `${Math.min(pct, 100)}%`, background: color }} />
      </div>
    </div>
  )
}

function DayLedger({ plan, targets, entry }: { plan: DietPlan; targets: Targets | null; entry: ProgressEntry | null }) {
  return (
    <div className="space-y-5">
      <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-ctl border border-line bg-line text-sm">
        <div className="bg-paper px-4 py-3">
          <dt className="text-ink-soft">Planned</dt>
          <dd className="mt-0.5 font-heading text-xl font-semibold num">
            {plan.totalCalories.toLocaleString()}
            {targets && <span className="text-sm font-normal text-ink-soft"> / {targets.kcal.toLocaleString()} kcal</span>}
          </dd>
        </div>
        <div className="bg-paper px-4 py-3">
          <dt className="text-ink-soft">Eaten</dt>
          <dd className="mt-0.5 font-heading text-xl font-semibold num">
            {entry?.caloriesConsumed != null ? entry.caloriesConsumed.toLocaleString() : '0'}
            <span className="text-sm font-normal text-ink-soft">
              {' '}kcal{entry?.caloriesOverridden ? ', typed by you' : ''}
            </span>
          </dd>
        </div>
      </dl>

      {targets && (
        <div className="space-y-3.5">
          <MacroLine label="Protein" value={Math.round(plan.totalProteinG)} max={targets.proteinG} color={ROLE_COLORS.PROTEIN} />
          <MacroLine label="Carbs" value={Math.round(plan.totalCarbsG)} max={targets.carbsG} color={ROLE_COLORS.CARB} />
          <MacroLine label="Fat" value={Math.round(plan.totalFatG)} max={targets.fatG} color={ROLE_COLORS.FAT} />
          {plan.totalFiberG != null && (
            <MacroLine label="Fiber" value={Math.round(plan.totalFiberG)} max={targets.fiberG} color={ROLE_COLORS.PRODUCE} />
          )}
        </div>
      )}
      {/* Total sugars include natural sugars, so no generic limit is shown (BR-MAC-07) */}
      <p className="text-sm text-ink-soft">
        Total sugars (incl. natural): <span className="font-semibold text-ink num">{Math.round(plan.totalSugarG)} g</span>
      </p>
    </div>
  )
}

function MealRow({
  meal,
  last,
  target,
  planId,
  onSwapped,
  onCompleted,
  onAsk,
}: {
  meal: Meal
  last: boolean
  target?: SlotTarget
  planId: number
  onSwapped: (plan: DietPlan) => void
  onCompleted: (plan: DietPlan, progress: ProgressEntry) => void
  onAsk: () => void
}) {
  const [open, setOpen] = useState(false)
  const detailsId = `meal-details-${meal.id}`
  return (
    <li id={`meal-${meal.id}`} className="relative grid scroll-mt-24 grid-cols-[28px_1fr] gap-x-4">
      {/* timeline: the node is filled once the meal is eaten */}
      <div className="relative flex justify-center" aria-hidden>
        {!last && <span className="absolute top-7 bottom-0 w-px bg-line" />}
        <span
          className={`relative mt-1.5 h-[18px] w-[18px] rounded-full border-2 transition-colors ${
            meal.completed ? 'border-ink bg-ink' : 'border-ink-faint bg-chalk'
          }`}
        />
      </div>

      <div className="min-w-0 pb-9">
        <div className="flex flex-wrap items-start justify-between gap-x-6 gap-y-2">
          <div className="min-w-0">
            <h3 className="font-heading text-xl font-semibold">{meal.name}</h3>
            <p className="mt-0.5 flex flex-wrap gap-x-3 text-sm num">
              <span className="font-semibold text-ink">{meal.calories} kcal</span>
              <span className="text-ink-faint">
                {Math.round(meal.proteinG ?? 0)} g protein, {Math.round(meal.carbsG ?? 0)} g carbs, {Math.round(meal.fatG ?? 0)} g fat
              </span>
            </p>
          </div>
          <MealCheck planId={planId} meal={meal} onChanged={onCompleted} />
        </div>

        <MealComponentsSummary components={meal.components} />

        <button
          type="button"
          onClick={() => setOpen(o => !o)}
          aria-expanded={open}
          aria-controls={detailsId}
          className="btn-ghost -ml-3 mt-1"
        >
          {open ? 'Hide details' : 'Details and swaps'}
          <ChevronIcon size={16} className={`transition-transform ${open ? 'rotate-180' : ''}`} />
        </button>

        <AnimatePresence initial={false}>
          {open && (
            <motion.div
              id={detailsId}
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.2 }}
              className="overflow-hidden"
            >
              <div className="mt-2 rounded-ctl border border-line bg-paper px-4 py-3">
                <MealComponents components={meal.components} />
                {target && (
                  <p className="mt-2 border-t border-line pt-2 text-xs text-ink-soft num">
                    This meal aims for about {target.kcal} kcal and {target.proteinG} g protein, its share of your day.
                  </p>
                )}
              </div>
              {meal.aiSuggestion && (
                <p className="mt-3 border-l-2 border-ink pl-3 text-sm leading-relaxed text-ink-soft">{meal.aiSuggestion}</p>
              )}
              <div className="mt-2 flex flex-wrap items-start gap-x-1">
                <SwapMenu planId={planId} mealId={meal.id} onSwapped={onSwapped} />
                <button type="button" onClick={onAsk} className="btn-ghost">
                  <SparkIcon size={17} />
                  Ask for a different meal
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </li>
  )
}

function EmptyPlate() {
  return (
    <svg viewBox="0 0 200 200" className="h-40 w-40" aria-hidden>
      <circle cx="100" cy="100" r="92" fill="#FFFFFF" stroke="#D9DED3" />
      <circle cx="100" cy="100" r="64" fill="none" stroke="#D9DED3" strokeDasharray="4 6" />
    </svg>
  )
}

export default function DashboardPage() {
  const { userId } = useAuth()
  const [plan, setPlan] = useState<DietPlan | null>(null)
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [todayEntry, setTodayEntry] = useState<ProgressEntry | null>(null)
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState('')
  const [chatTrigger, setChatTrigger] = useState('')
  const [chatOpenRequest, setChatOpenRequest] = useState(0)
  useAiRefresh(userId, plan, setPlan)

  useEffect(() => {
    if (!userId) return
    api.get<UserProfile>(`/users/${userId}/profile`).then(r => setProfile(r.data)).catch(() => {})
    api.get<DietPlan[]>(`/users/${userId}/diet-plans`).then(r => {
      const active = r.data.find(p => p.status === 'ACTIVE')
      if (active) setPlan(active)
    }).catch(() => {})

    const today = localIsoDate()
    api.get<ProgressEntry[]>(`/users/${userId}/progress/weekly?start=${today}&end=${today}`)
      .then(r => { if (r.data.length > 0) setTodayEntry(r.data[0]) })
      .catch(() => {})
  }, [userId])

  const generate = async () => {
    if (!userId) return
    setGenerating(true); setError('')
    try {
      // No date: the server plans for the user's own today (BR-TIM-01, BR-LIF-03)
      setPlan((await api.post<DietPlan>(`/users/${userId}/diet-plans/generate`)).data)
    } catch (err) { setError(planErrorMessage(err)) }
    finally { setGenerating(false) }
  }

  // Targets come from the API (BR-PRF-08); nothing here is hard-coded
  const targets = profile?.targets ?? null
  const targetsError = profile?.targetsError ?? null
  const missingFields = targetsError?.details.missingFields ?? []
  const slotTarget = (meal: Meal) => targets?.slots.find(s => s.slot === meal.mealType)
  const meals = plan ? [...plan.meals].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)) : []
  const eatenCount = meals.filter(m => m.completed).length

  return (
    <>
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm text-ink-soft">
            {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })}
          </p>
          <h1 className="page-title mt-1">
            {greeting()}{profile?.firstName ? `, ${profile.firstName}` : ''}
          </h1>
          {plan && (
            <p className="mt-2 text-ink-soft">
              {eatenCount === 0
                ? 'Your plate for today is ready.'
                : eatenCount === meals.length
                ? 'Every meal of the day is checked off.'
                : `${eatenCount} of ${meals.length} meals eaten so far.`}
            </p>
          )}
        </div>
        {plan && (
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={generate} disabled={generating} className="btn-secondary">
              <RefreshIcon size={17} className={generating ? 'animate-spin' : ''} />
              {generating ? 'Planning…' : 'Plan the day again'}
            </button>
            {/* phones use the floating button instead */}
            <button type="button" onClick={() => setChatOpenRequest(n => n + 1)} className="btn-primary hidden lg:inline-flex">
              <ChatIcon size={17} />
              Ask the assistant
            </button>
          </div>
        )}
      </header>

      <div className="mt-6 space-y-3">
        {profile && targetsError && (
          <div className="banner-warning items-center">
            <div className="flex-1">
              <p className="font-semibold">
                {targetsError.code === 'PROFILE_INELIGIBLE' ? 'Plans are for adults' : 'Complete your profile'}
              </p>
              <p className="mt-0.5">
                {targetsError.code === 'PROFILE_INCOMPLETE' && missingFields.length > 0
                  ? `Add your ${missingFields.map(f => FIELD_LABELS[f] ?? f).join(', ')} so we can calculate your targets.`
                  : targetsError.message}
              </p>
            </div>
            <Link to="/profile" className="btn-primary shrink-0">Open profile</Link>
          </div>
        )}
        <MedicalDisclaimer disclaimer={plan?.disclaimer ?? profile?.disclaimer} />
        <PlanWarnings warnings={plan?.warnings} />
        {error && <div className="banner-error" role="alert">{error}</div>}
      </div>

      {!plan && (
        <section className="mt-10 flex flex-col items-start gap-6 rounded-panel border border-line bg-paper p-8 sm:flex-row sm:items-center">
          <EmptyPlate />
          <div>
            <h2 className="section-title text-2xl">Nothing on the plate yet</h2>
            <p className="mt-2 max-w-[52ch] text-ink-soft">
              Plan today's five meals from your targets, your allergies and your diet. It takes a few seconds,
              and you can swap any food afterwards.
            </p>
            <button type="button" onClick={generate} disabled={generating || !!targetsError} className="btn-primary mt-5">
              {generating ? 'Planning your day…' : "Plan today's meals"}
            </button>
          </div>
        </section>
      )}

      {plan && (
        <div className="mt-8 grid gap-x-14 gap-y-10 lg:grid-cols-[340px_minmax(0,1fr)] lg:grid-rows-[auto_1fr]">
          {/* phones: plate, meals, then the ledger; wide screens: plate and ledger on the left */}
          <div className="space-y-6 lg:col-start-1 lg:row-start-1">
            <DayPlate
              meals={meals}
              eatenKcal={todayEntry?.caloriesConsumed ?? null}
              plannedKcal={plan.totalCalories}
              targetKcal={targets?.kcal ?? null}
            />
            <RoleKey />
          </div>

          <div className="lg:col-start-1 lg:row-start-2">
            <DayLedger plan={plan} targets={targets} entry={todayEntry} />
          </div>

          <section aria-labelledby="meals-heading" className="lg:col-start-2 lg:row-span-2 lg:row-start-1">
            <h2 id="meals-heading" className="section-title mb-6">Today's meals</h2>
            <ol>
              {meals.map((meal, i) => (
                <MealRow
                  key={meal.id}
                  meal={meal}
                  last={i === meals.length - 1}
                  target={slotTarget(meal)}
                  planId={plan.id}
                  onSwapped={setPlan}
                  onCompleted={(p, progress) => { setPlan(p); setTodayEntry(progress) }}
                  onAsk={() =>
                    setChatTrigger(`Replace my ${meal.mealType.replace(/_/g, ' ').toLowerCase()} with something different`)
                  }
                />
              ))}
            </ol>

            {(plan.aiExplanation || plan.aiStatus === 'PENDING') && (
              <div className="card-brand mt-2">
                <p className="flex items-center gap-2 font-heading text-lg font-semibold">
                  <SparkIcon size={18} />
                  Why this plan
                </p>
                <p className="mt-2 max-w-[65ch] text-sm leading-relaxed text-chalk/80">
                  {plan.aiExplanation ?? 'The explanation is being written. It appears here in a few seconds.'}
                </p>
              </div>
            )}
          </section>
        </div>
      )}

      {plan && (
        <ChatPanel
          planId={plan.id}
          onPlanUpdate={setPlan}
          triggerMessage={chatTrigger}
          onTriggerConsumed={() => setChatTrigger('')}
          openRequest={chatOpenRequest}
        />
      )}
    </>
  )
}
