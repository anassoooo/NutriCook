import { useCallback, useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { UserProfile, ActivityLevel, HealthGoal, DietaryRestriction, HealthCondition, Gender } from '../types'
import { GENDER_OPTIONS } from '../types'
import { ACTIVITY_OPTIONS, GOAL_OPTIONS, PROFILE_BOUNDS, RESTRICTION_GROUPS } from '../lib/profileOptions'
import { ROLE_COLORS } from '../lib/roles'
import { CheckIcon } from '../components/Icons'

interface FormData {
  firstName: string
  lastName: string
  weightKg: number
  heightCm: number
  dateOfBirth: string
  gender: Gender
  activityLevel: ActivityLevel
  healthGoal: HealthGoal
}

const MISSING_LABEL: Record<string, string> = {
  dateOfBirth: 'date of birth', gender: 'gender', weightKg: 'weight', heightCm: 'height',
  activityLevel: 'activity level', healthGoal: 'goal',
}

/** A segmented choice made of real radio inputs. */
function Choice<T extends string>({
  name, value, options, register, columns = 1,
}: {
  name: keyof FormData
  value: T
  options: { value: T; label: string; sub?: string }[]
  register: ReturnType<typeof useForm<FormData>>['register']
  columns?: number
}) {
  return (
    <div className={`grid gap-2 ${columns === 2 ? 'sm:grid-cols-2' : columns === 3 ? 'grid-cols-3' : ''}`}>
      {options.map(o => {
        const on = value === o.value
        return (
          <label
            key={o.value}
            className={`flex cursor-pointer items-start gap-3 rounded-ctl border px-4 py-3 transition-colors has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-ink ${
              on ? 'border-ink bg-paper' : 'border-line hover:border-ink-faint'
            }`}
          >
            <input type="radio" value={o.value} {...register(name)} className="sr-only" />
            <span className={`mt-0.5 grid h-4 w-4 shrink-0 place-items-center rounded-full border-2 ${on ? 'border-ink' : 'border-ink-faint'}`} aria-hidden>
              {on && <span className="h-1.5 w-1.5 rounded-full bg-ink" />}
            </span>
            <span>
              <span className="block text-sm font-semibold">{o.label}</span>
              {o.sub && <span className="block text-xs text-ink-soft">{o.sub}</span>}
            </span>
          </label>
        )
      })}
    </div>
  )
}

/** A selectable item that saves at once (restrictions and conditions are separate API calls). */
function Toggle({ on, onClick, name, description }: { on: boolean; onClick: () => void; name: string; description: string | null }) {
  return (
    <li>
      <button
        type="button"
        role="checkbox"
        aria-checked={on}
        onClick={onClick}
        className={`flex w-full items-start gap-3 rounded-ctl border px-4 py-3 text-left transition-colors ${
          on ? 'border-ink bg-paper' : 'border-line hover:border-ink-faint'
        }`}
      >
        <span className={`mt-0.5 grid h-4 w-4 shrink-0 place-items-center rounded border ${on ? 'border-ink bg-ink text-chalk' : 'border-ink-faint'}`} aria-hidden>
          {on && <CheckIcon size={11} strokeWidth={3} />}
        </span>
        <span>
          <span className="block text-sm font-semibold">{name}</span>
          {description && <span className="block text-xs text-ink-soft">{description}</span>}
        </span>
      </button>
    </li>
  )
}

function Section({ title, hint, children }: { title: string; hint?: string; children: React.ReactNode }) {
  return (
    <section className="border-t border-line py-8 first:border-t-0 first:pt-0">
      <h2 className="section-title">{title}</h2>
      {hint && <p className="mt-1 max-w-[60ch] text-sm text-ink-soft">{hint}</p>}
      <div className="mt-5">{children}</div>
    </section>
  )
}

/** The daily target exactly as the server computed it from the saved profile (BR-NRG, BR-MAC). */
function TargetPanel({ profile }: { profile: UserProfile | null }) {
  if (!profile) return null
  const t = profile.targets
  if (!t) {
    const missing = profile.targetsError?.details.missingFields ?? []
    return (
      <div className="rounded-panel border border-dashed border-line p-6">
        <p className="font-heading text-lg font-semibold">Your daily target</p>
        <p className="mt-2 text-sm text-ink-soft">
          {missing.length > 0
            ? `Add your ${missing.map(f => MISSING_LABEL[f] ?? f).join(', ')} and save to see it.`
            : profile.targetsError?.message ?? 'Save your profile to see it.'}
        </p>
      </div>
    )
  }
  const macros: [string, number, string][] = [
    ['Protein', t.proteinG, ROLE_COLORS.PROTEIN],
    ['Carbs', t.carbsG, ROLE_COLORS.CARB],
    ['Fat', t.fatG, ROLE_COLORS.FAT],
    ['Fiber', t.fiberG, ROLE_COLORS.PRODUCE],
  ]
  return (
    <div className="rounded-panel bg-ink p-6 text-chalk">
      <p className="text-sm text-chalk/70">Your daily target</p>
      <p className="mt-1 font-heading text-5xl font-bold leading-none num">
        {t.kcal.toLocaleString()}<span className="ml-1.5 text-lg font-medium text-chalk/70">kcal</span>
      </p>
      <dl className="mt-5 space-y-1.5 border-t border-chalk/15 pt-4 text-sm num">
        <div className="flex justify-between"><dt className="text-chalk/70">Energy at rest (BMR)</dt><dd>{Math.round(t.bmr).toLocaleString()}</dd></div>
        <div className="flex justify-between"><dt className="text-chalk/70">With your activity (TDEE)</dt><dd>{Math.round(t.tdee).toLocaleString()}</dd></div>
        <div className="flex justify-between"><dt className="text-chalk/70">Adjusted for your goal</dt><dd>{t.kcal.toLocaleString()}</dd></div>
      </dl>
      {t.floorApplied && (
        <p className="mt-3 text-xs text-chalk/70">Raised to the safe minimum for you.</p>
      )}
      <ul className="mt-5 grid grid-cols-2 gap-x-4 gap-y-2 border-t border-chalk/15 pt-4 text-sm num">
        {macros.map(([label, grams, color]) => (
          <li key={label} className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full" style={{ background: color }} aria-hidden />
            <span className="text-chalk/70">{label}</span>
            <span className="ml-auto font-semibold">{grams} g</span>
          </li>
        ))}
      </ul>
      <p className="mt-4 text-xs text-chalk/60">Based on your saved profile. It updates when you save.</p>
    </div>
  )
}

export default function ProfilePage() {
  const { userId, email } = useAuth()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [saved, setSaved] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const [allRestrictions, setAllRestrictions] = useState<DietaryRestriction[]>([])
  const [allConditions, setAllConditions] = useState<HealthCondition[]>([])
  const [selectedRestrictions, setSelectedRestrictions] = useState<Set<number>>(new Set())
  const [selectedConditions, setSelectedConditions] = useState<Set<number>>(new Set())
  const [optionsError, setOptionsError] = useState(false)

  const { control, register, handleSubmit, reset, formState: { errors } } =
    useForm<FormData>({ defaultValues: { gender: 'MALE', activityLevel: 'SEDENTARY', healthGoal: 'IMPROVE_HEALTH' } })
  const all = useWatch({ control })

  const loadProfile = useCallback(() => {
    if (!userId) return
    api.get<UserProfile>(`/users/${userId}/profile`).then(r => {
      const p = r.data
      setProfile(p)
      setSelectedRestrictions(new Set((p.dietaryRestrictions ?? []).map(x => x.id)))
      setSelectedConditions(new Set((p.healthConditions ?? []).map(c => c.id)))
      reset({
        firstName: p.firstName ?? '',
        lastName: p.lastName ?? '',
        weightKg: p.weightKg ?? undefined,
        heightCm: p.heightCm ?? undefined,
        dateOfBirth: p.dateOfBirth ?? '',
        gender: p.gender ?? 'MALE',
        activityLevel: p.activityLevel ?? 'SEDENTARY',
        healthGoal: p.healthGoal ?? 'IMPROVE_HEALTH',
      })
    }).catch(() => {})
  }, [userId, reset])

  useEffect(() => {
    Promise.all([
      api.get<DietaryRestriction[]>('/dietary-restrictions'),
      api.get<HealthCondition[]>('/health-conditions'),
    ])
      .then(([r, c]) => { setAllRestrictions(r.data); setAllConditions(c.data) })
      .catch(() => setOptionsError(true))
  }, [])

  useEffect(loadProfile, [loadProfile])

  /** Optimistic toggle; rolled back if the server refuses. Restrictions change targets, so reload. */
  const toggle = async (kind: 'dietary-restrictions' | 'health-conditions', id: number) => {
    const set = kind === 'dietary-restrictions' ? setSelectedRestrictions : setSelectedConditions
    const current = kind === 'dietary-restrictions' ? selectedRestrictions : selectedConditions
    const on = current.has(id)
    const flip = (s: Set<number>) => { const n = new Set(s); if (n.has(id)) n.delete(id); else n.add(id); return n }
    set(flip)
    try {
      if (on) await api.delete(`/users/${userId}/profile/${kind}/${id}`)
      else await api.post(`/users/${userId}/profile/${kind}/${id}`)
    } catch (err) {
      set(flip)
      setError(toApiError(err).message)
    }
  }

  const onSubmit = async (data: FormData) => {
    if (!userId) return
    setLoading(true); setError(''); setSaved(false)
    try {
      await api.put(`/users/${userId}/profile`, {
        ...data,
        firstName: data.firstName || null,
        lastName: data.lastName || null,
        dateOfBirth: data.dateOfBirth || null,
        weightKg: isNaN(data.weightKg) ? null : data.weightKg,
        heightCm: isNaN(data.heightCm) ? null : data.heightCm,
      })
      setSaved(true)
      loadProfile()
    } catch (err) {
      const e = toApiError(err)
      setError(e.detail ?? e.message)
    } finally { setLoading(false) }
  }

  const name = [all.firstName, all.lastName].filter(Boolean).join(' ')

  return (
    <div>
      <header>
        <h1 className="page-title">{name || 'Your profile'}</h1>
        <p className="page-subtitle">{email}</p>
      </header>

      <div className="mt-8 grid gap-10 lg:grid-cols-[minmax(0,1fr)_320px]">
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <Section title="About you" hint="Age and sex change how much energy your body uses at rest.">
            <div className="grid gap-4 sm:grid-cols-2">
              <label>
                <span className="label">First name <span className="font-normal text-ink-faint">(optional)</span></span>
                <input className="input" type="text" autoComplete="given-name" {...register('firstName')} />
              </label>
              <label>
                <span className="label">Last name <span className="font-normal text-ink-faint">(optional)</span></span>
                <input className="input" type="text" autoComplete="family-name" {...register('lastName')} />
              </label>
              <label>
                <span className="label">Date of birth</span>
                <input className="input" type="date" max={new Date().toISOString().slice(0, 10)} {...register('dateOfBirth')} />
              </label>
              <fieldset>
                <legend className="label">Sex</legend>
                <Choice name="gender" value={all.gender as Gender} options={GENDER_OPTIONS} register={register} columns={3} />
              </fieldset>
            </div>
          </Section>

          <Section title="Body" hint="Used for your energy and protein targets. Logging a new weight on Progress updates it too.">
            <div className="grid max-w-md grid-cols-2 gap-4">
              <label>
                <span className="label">Weight (kg)</span>
                <input className="input num" type="number" step="0.1" {...PROFILE_BOUNDS.weightKg}
                  aria-invalid={!!errors.weightKg}
                  {...register('weightKg', { required: true, ...PROFILE_BOUNDS.weightKg, valueAsNumber: true })} />
                {errors.weightKg && <span className="mt-1 block text-xs text-alert">Between {PROFILE_BOUNDS.weightKg.min} and {PROFILE_BOUNDS.weightKg.max} kg</span>}
              </label>
              <label>
                <span className="label">Height (cm)</span>
                <input className="input num" type="number" {...PROFILE_BOUNDS.heightCm}
                  aria-invalid={!!errors.heightCm}
                  {...register('heightCm', { required: true, ...PROFILE_BOUNDS.heightCm, valueAsNumber: true })} />
                {errors.heightCm && <span className="mt-1 block text-xs text-alert">Between {PROFILE_BOUNDS.heightCm.min} and {PROFILE_BOUNDS.heightCm.max} cm</span>}
              </label>
            </div>
          </Section>

          <Section title="Activity" hint="An average week, including work.">
            <Choice name="activityLevel" value={all.activityLevel as ActivityLevel} options={ACTIVITY_OPTIONS} register={register} columns={2} />
          </Section>

          <Section title="Goal">
            <Choice name="healthGoal" value={all.healthGoal as HealthGoal} options={GOAL_OPTIONS} register={register} columns={2} />
          </Section>

          {error && <div role="alert" className="banner-error mb-4">{error}</div>}
          <div className="flex items-center gap-4 border-t border-line pt-6">
            <button type="submit" disabled={loading} className="btn-primary">
              {loading ? 'Saving…' : 'Save profile'}
            </button>
            {saved && <p role="status" className="text-sm font-semibold text-cucumber">Saved. Your target is up to date.</p>}
          </div>

          <Section title="Allergies and diets" hint="These apply as soon as you tick them. Every plan and swap respects them.">
            {optionsError ? (
              <p className="text-sm text-alert">The options could not be loaded. Refresh the page to try again.</p>
            ) : (
              <div className="space-y-6">
                {RESTRICTION_GROUPS.map(group => {
                  const items = allRestrictions.filter(r => r.type === group.type)
                  if (!items.length) return null
                  return (
                    <div key={group.type}>
                      <h3 className="text-sm font-semibold">{group.label}</h3>
                      <p className="text-xs text-ink-soft">{group.hint}</p>
                      <ul className="mt-2 grid gap-2 sm:grid-cols-2">
                        {items.map(r => (
                          <Toggle key={r.id} on={selectedRestrictions.has(r.id)} name={r.name} description={r.description}
                                  onClick={() => toggle('dietary-restrictions', r.id)} />
                        ))}
                      </ul>
                    </div>
                  )
                })}
              </div>
            )}
          </Section>

          <Section title="Health conditions" hint="Plans then stay within the sugar, fat and salt limits for each condition. This is not medical advice.">
            {!optionsError && (
              <ul className="grid gap-2 sm:grid-cols-2">
                {allConditions.map(c => (
                  <Toggle key={c.id} on={selectedConditions.has(c.id)} name={c.name} description={c.description}
                          onClick={() => toggle('health-conditions', c.id)} />
                ))}
              </ul>
            )}
          </Section>
        </form>

        <aside className="lg:sticky lg:top-10 lg:self-start">
          <TargetPanel profile={profile} />
        </aside>
      </div>
    </div>
  )
}
