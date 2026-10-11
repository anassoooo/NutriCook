import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { ActivityLevel, HealthGoal, DietaryRestriction, HealthCondition, UserProfile, Gender } from '../types'
import { GENDER_OPTIONS } from '../types'
import { ACTIVITY_OPTIONS, GOAL_OPTIONS, PROFILE_BOUNDS, RESTRICTION_GROUPS } from '../lib/profileOptions'
import { Wordmark } from '../components/Brand'
import { CheckIcon } from '../components/Icons'

const STEPS = [
  { n: 1, label: 'Your body', hint: 'For your energy needs' },
  { n: 2, label: 'Your goal', hint: 'Activity and aim' },
  { n: 3, label: 'Allergies and diets', hint: 'What never goes on your plate' },
]

function Option({ on, onClick, label, sub, kind = 'radio' }: { on: boolean; onClick: () => void; label: string; sub?: string | null; kind?: 'radio' | 'checkbox' }) {
  return (
    <button
      type="button"
      role={kind}
      aria-checked={on}
      onClick={onClick}
      className={`flex w-full items-start gap-3 rounded-ctl border px-4 py-3 text-left transition-colors ${
        on ? 'border-ink bg-paper' : 'border-line hover:border-ink-faint'
      }`}
    >
      <span
        className={`mt-0.5 grid h-4 w-4 shrink-0 place-items-center border-2 ${kind === 'radio' ? 'rounded-full' : 'rounded'} ${
          on ? 'border-ink' + (kind === 'checkbox' ? ' bg-ink text-chalk' : '') : 'border-ink-faint'
        }`}
        aria-hidden
      >
        {on && (kind === 'radio' ? <span className="h-1.5 w-1.5 rounded-full bg-ink" /> : <CheckIcon size={10} strokeWidth={3} />)}
      </span>
      <span>
        <span className="block text-sm font-semibold">{label}</span>
        {sub && <span className="block text-xs text-ink-soft">{sub}</span>}
      </span>
    </button>
  )
}

export default function OnboardingPage() {
  const { userId } = useAuth()
  const navigate = useNavigate()

  // A returning user with a complete body profile goes straight to Today
  useEffect(() => {
    if (!userId) return
    api.get<UserProfile>(`/users/${userId}/profile`)
      .then(r => { if (r.data.weightKg && r.data.heightCm && r.data.dateOfBirth) navigate('/dashboard', { replace: true }) })
      .catch(() => {})
  }, [userId, navigate])

  const [step, setStep] = useState(1)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState('')

  const [firstName, setFirstName] = useState('')
  const [weightKg, setWeightKg] = useState('')
  const [heightCm, setHeightCm] = useState('')
  const [dateOfBirth, setDateOfBirth] = useState('')
  // no default: sex changes the energy formula, so the user chooses (BR-PRF-01)
  const [gender, setGender] = useState<Gender | null>(null)
  const [step1Error, setStep1Error] = useState('')

  const [activityLevel, setActivityLevel] = useState<ActivityLevel>('MODERATELY_ACTIVE')
  const [healthGoal, setHealthGoal] = useState<HealthGoal>('IMPROVE_HEALTH')

  const [allRestrictions, setAllRestrictions] = useState<DietaryRestriction[]>([])
  const [allConditions, setAllConditions] = useState<HealthCondition[]>([])
  const [selectedRestrictions, setSelectedRestrictions] = useState<Set<number>>(new Set())
  const [selectedConditions, setSelectedConditions] = useState<Set<number>>(new Set())
  const [optionsError, setOptionsError] = useState(false)

  // Fetched now so step 3 is instant
  useEffect(() => {
    Promise.all([api.get<DietaryRestriction[]>('/dietary-restrictions'), api.get<HealthCondition[]>('/health-conditions')])
      .then(([r, c]) => { setAllRestrictions(r.data); setAllConditions(c.data) })
      .catch(() => setOptionsError(true))
  }, [])

  const goNext = () => {
    if (step === 1) {
      const w = parseFloat(weightKg)
      const h = parseFloat(heightCm)
      // BR-PRF-01: targets need all of these; BR-PRF-03 ranges. The server checks again.
      if (!dateOfBirth || !gender || !weightKg || !heightCm || isNaN(w) || isNaN(h)) {
        setStep1Error('Date of birth, sex, weight and height are all needed to work out your daily target.')
        return
      }
      if (w < PROFILE_BOUNDS.weightKg.min || w > PROFILE_BOUNDS.weightKg.max) {
        setStep1Error(`Weight must be between ${PROFILE_BOUNDS.weightKg.min} and ${PROFILE_BOUNDS.weightKg.max} kg.`); return
      }
      if (h < PROFILE_BOUNDS.heightCm.min || h > PROFILE_BOUNDS.heightCm.max) {
        setStep1Error(`Height must be between ${PROFILE_BOUNDS.heightCm.min} and ${PROFILE_BOUNDS.heightCm.max} cm.`); return
      }
      setStep1Error('')
    }
    setStep(s => s + 1)
  }

  const toggle = (set: typeof setSelectedRestrictions, id: number) =>
    set(prev => { const s = new Set(prev); if (s.has(id)) s.delete(id); else s.add(id); return s })

  /** Saves everything, then plans the first day so Today opens on a full plate. */
  const handleFinish = async () => {
    if (!userId) return
    setSaving(true); setSaveError('')
    try {
      await api.put(`/users/${userId}/profile`, {
        firstName: firstName || null,
        weightKg: parseFloat(weightKg),
        heightCm: parseFloat(heightCm),
        dateOfBirth,
        gender,
        activityLevel,
        healthGoal,
        // "Today" is the user's own calendar day (BR-TIM-01)
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      })
      await Promise.all([
        ...[...selectedRestrictions].map(id => api.post(`/users/${userId}/profile/dietary-restrictions/${id}`)),
        ...[...selectedConditions].map(id => api.post(`/users/${userId}/profile/health-conditions/${id}`)),
      ])
      // Best effort: if it cannot be planned (e.g. an ineligible profile), Today explains why
      await api.post(`/users/${userId}/diet-plans/generate`).catch(() => {})
      navigate('/dashboard', { replace: true })
    } catch (err) {
      const e = toApiError(err)
      setSaveError(e.detail ?? e.message)
      setSaving(false)
    }
  }

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[360px_1fr]">
      {/* Steps */}
      <aside className="border-b border-line px-6 py-6 lg:border-b-0 lg:border-r lg:px-10 lg:py-10">
        <Wordmark to="/" />
        <p className="mt-8 hidden max-w-[28ch] text-ink-soft lg:block">
          Three short steps, and NutriCook plans your first day of meals.
        </p>
        <ol className="mt-6 flex gap-6 lg:mt-10 lg:flex-col lg:gap-5" aria-label="Steps">
          {STEPS.map(s => {
            const done = step > s.n
            const current = step === s.n
            return (
              <li key={s.n} className="flex items-center gap-3" aria-current={current ? 'step' : undefined}>
                <span
                  className={`grid h-8 w-8 shrink-0 place-items-center rounded-full text-sm font-semibold ${
                    done ? 'bg-ink text-chalk' : current ? 'border-2 border-ink text-ink' : 'border border-line text-ink-faint'
                  }`}
                >
                  {done ? <CheckIcon size={15} strokeWidth={2.5} /> : s.n}
                </span>
                <span className="hidden sm:block">
                  <span className={`block text-sm font-semibold ${current || done ? 'text-ink' : 'text-ink-faint'}`}>{s.label}</span>
                  <span className="hidden text-xs text-ink-faint lg:block">{s.hint}</span>
                </span>
              </li>
            )
          })}
        </ol>
      </aside>

      {/* Step content */}
      <main className="px-6 py-10 lg:px-16 lg:py-16">
        <div className="max-w-xl">
          {step === 1 && (
            <section aria-labelledby="s1">
              <h1 id="s1" className="page-title">About your body</h1>
              <p className="page-subtitle">Your daily energy target is worked out from these, with the Mifflin-St Jeor formula.</p>
              <div className="mt-8 grid gap-4 sm:grid-cols-2">
                <label className="sm:col-span-2">
                  <span className="label">First name <span className="font-normal text-ink-faint">(optional)</span></span>
                  <input className="input" value={firstName} onChange={e => setFirstName(e.target.value)} autoComplete="given-name" />
                </label>
                <label>
                  <span className="label">Date of birth</span>
                  <input className="input" type="date" value={dateOfBirth} max={new Date().toISOString().slice(0, 10)} onChange={e => setDateOfBirth(e.target.value)} />
                </label>
                <fieldset>
                  <legend className="label">Sex</legend>
                  <div className="grid grid-cols-3 gap-2">
                    {GENDER_OPTIONS.map(g => (
                      <button
                        key={g.value}
                        type="button"
                        role="radio"
                        aria-checked={gender === g.value}
                        onClick={() => setGender(g.value)}
                        className={`rounded-ctl border py-2.5 text-sm font-semibold transition-colors ${
                          gender === g.value ? 'border-ink bg-ink text-chalk' : 'border-line hover:border-ink-faint'
                        }`}
                      >
                        {g.label}
                      </button>
                    ))}
                  </div>
                </fieldset>
                <label>
                  <span className="label">Weight (kg)</span>
                  <input className="input num" type="number" step="0.1" inputMode="decimal" {...PROFILE_BOUNDS.weightKg} value={weightKg} onChange={e => setWeightKg(e.target.value)} />
                </label>
                <label>
                  <span className="label">Height (cm)</span>
                  <input className="input num" type="number" inputMode="numeric" {...PROFILE_BOUNDS.heightCm} value={heightCm} onChange={e => setHeightCm(e.target.value)} />
                </label>
              </div>
              {step1Error && <p role="alert" className="banner-error mt-5">{step1Error}</p>}
            </section>
          )}

          {step === 2 && (
            <section aria-labelledby="s2">
              <h1 id="s2" className="page-title">What are you aiming for?</h1>
              <p className="page-subtitle">You can change both later in your profile.</p>
              <h2 className="mt-8 text-sm font-semibold">How active is an average week?</h2>
              <div className="mt-2 grid gap-2 sm:grid-cols-2" role="radiogroup" aria-label="Activity">
                {ACTIVITY_OPTIONS.map(o => (
                  <Option key={o.value} on={activityLevel === o.value} onClick={() => setActivityLevel(o.value)} label={o.label} sub={o.sub} />
                ))}
              </div>
              <h2 className="mt-8 text-sm font-semibold">Your goal</h2>
              <div className="mt-2 grid gap-2 sm:grid-cols-2" role="radiogroup" aria-label="Goal">
                {GOAL_OPTIONS.map(o => (
                  <Option key={o.value} on={healthGoal === o.value} onClick={() => setHealthGoal(o.value)} label={o.label} sub={o.sub} />
                ))}
              </div>
            </section>
          )}

          {step === 3 && (
            <section aria-labelledby="s3">
              <h1 id="s3" className="page-title">What should never be on your plate?</h1>
              <p className="page-subtitle">Every plan and every swap respects these. Skip this step if none apply.</p>
              {optionsError ? (
                <p className="banner-error mt-8">The options could not be loaded. You can add them later in your profile.</p>
              ) : (
                <div className="mt-8 space-y-6">
                  {RESTRICTION_GROUPS.map(group => {
                    const items = allRestrictions.filter(r => r.type === group.type)
                    if (!items.length) return null
                    return (
                      <div key={group.type}>
                        <h2 className="text-sm font-semibold">{group.label}</h2>
                        <div className="mt-2 grid gap-2 sm:grid-cols-2">
                          {items.map(r => (
                            <Option key={r.id} kind="checkbox" on={selectedRestrictions.has(r.id)} onClick={() => toggle(setSelectedRestrictions, r.id)} label={r.name} sub={r.description} />
                          ))}
                        </div>
                      </div>
                    )
                  })}
                  {allConditions.length > 0 && (
                    <div>
                      <h2 className="text-sm font-semibold">Health conditions</h2>
                      <p className="text-xs text-ink-soft">Plans then stay within the limits for each condition. This is not medical advice.</p>
                      <div className="mt-2 grid gap-2 sm:grid-cols-2">
                        {allConditions.map(c => (
                          <Option key={c.id} kind="checkbox" on={selectedConditions.has(c.id)} onClick={() => toggle(setSelectedConditions, c.id)} label={c.name} sub={c.description} />
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
              {saveError && <p role="alert" className="banner-error mt-6">{saveError}</p>}
            </section>
          )}

          <div className="mt-10 flex items-center gap-3 border-t border-line pt-6">
            {step > 1 && (
              <button type="button" onClick={() => setStep(s => s - 1)} className="btn-secondary" disabled={saving}>
                Back
              </button>
            )}
            {step < 3 ? (
              <button type="button" onClick={goNext} className="btn-primary">Continue</button>
            ) : (
              <button type="button" onClick={handleFinish} disabled={saving} className="btn-primary">
                {saving ? 'Planning your first day…' : 'Plan my first day'}
              </button>
            )}
            <span className="ml-auto text-sm text-ink-faint num">Step {step} of 3</span>
          </div>
        </div>
      </main>
    </div>
  )
}
