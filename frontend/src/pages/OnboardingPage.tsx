import { useEffect, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import Tooltip from '@mui/material/Tooltip'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { ActivityLevel, HealthGoal, DietaryRestriction, HealthCondition, UserProfile } from '../types'

const VIDEO_SRC =
  'https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260403_050628_c4e32401-fab4-4a27-b7a8-6e9291cd5959.mp4'

const ACTIVITY_OPTIONS: { value: ActivityLevel; label: string; sub: string; icon: string }[] = [
  { value: 'SEDENTARY',         label: 'Sedentary',         sub: 'Desk job, little exercise', icon: '🪑' },
  { value: 'LIGHTLY_ACTIVE',    label: 'Lightly active',    sub: '1–3 workouts / week',       icon: '🚶' },
  { value: 'MODERATELY_ACTIVE', label: 'Moderately active', sub: '3–5 workouts / week',       icon: '🏃' },
  { value: 'VERY_ACTIVE',       label: 'Very active',       sub: '6–7 workouts / week',       icon: '⚡' },
  { value: 'EXTRA_ACTIVE',      label: 'Extra active',      sub: 'Athlete / physical job',    icon: '🏋️' },
]

const GOAL_OPTIONS: { value: HealthGoal; label: string; icon: string; color: string }[] = [
  { value: 'LOSE_WEIGHT',     label: 'Lose weight',    icon: '⬇️', color: '#3b82f6' },
  { value: 'MAINTAIN_WEIGHT', label: 'Maintain',       icon: '⚖️', color: '#16a34a' },
  { value: 'GAIN_MUSCLE',     label: 'Gain muscle',    icon: '💪', color: '#f97316' },
  { value: 'IMPROVE_HEALTH',  label: 'Improve health', icon: '❤️', color: '#ef4444' },
]

const RESTRICTION_STYLE: Record<string, { bg: string; text: string; border: string }> = {
  PREFERENCE:  { bg: '#f0fdf4', text: '#16a34a', border: '#86efac' },
  INTOLERANCE: { bg: '#fffbeb', text: '#d97706', border: '#fcd34d' },
  ALLERGY:     { bg: '#fef2f2', text: '#dc2626', border: '#fca5a5' },
  RELIGIOUS:   { bg: '#eff6ff', text: '#2563eb', border: '#93c5fd' },
}

const STEPS = [
  { n: 1, label: 'Body metrics' },
  { n: 2, label: 'Your goal'    },
  { n: 3, label: 'Preferences'  },
]

export default function OnboardingPage() {
  const { userId } = useAuth()
  const navigate   = useNavigate()

  // Redirect if profile is already complete (returning user hitting this URL directly)
  useEffect(() => {
    if (!userId) return
    api.get<UserProfile>(`/users/${userId}/profile`)
      .then(r => { if (r.data.weightKg && r.data.heightCm) navigate('/dashboard', { replace: true }) })
      .catch(() => {})
  }, [userId, navigate])

  const [step,      setStep]      = useState(1)
  const [saving,    setSaving]    = useState(false)
  const [saveError, setSaveError] = useState('')

  /* ── Step 1 fields ─────────────────────────────────── */
  const [firstName,   setFirstName]   = useState('')
  const [weightKg,    setWeightKg]    = useState('')
  const [heightCm,    setHeightCm]    = useState('')
  const [dateOfBirth, setDateOfBirth] = useState('')
  const [gender,      setGender]      = useState<'MALE' | 'FEMALE'>('MALE')
  const [step1Error,  setStep1Error]  = useState('')

  /* ── Step 2 fields ─────────────────────────────────── */
  const [activityLevel, setActivityLevel] = useState<ActivityLevel>('MODERATELY_ACTIVE')
  const [healthGoal,    setHealthGoal]    = useState<HealthGoal>('IMPROVE_HEALTH')

  /* ── Step 3 fields ─────────────────────────────────── */
  const [allRestrictions,      setAllRestrictions]      = useState<DietaryRestriction[]>([])
  const [allConditions,        setAllConditions]        = useState<HealthCondition[]>([])
  const [selectedRestrictions, setSelectedRestrictions] = useState<Set<number>>(new Set())
  const [selectedConditions,   setSelectedConditions]   = useState<Set<number>>(new Set())
  const [restrictionsStatus,   setRestrictionsStatus]   = useState<'loading' | 'ok' | 'error'>('loading')
  const [conditionsStatus,     setConditionsStatus]     = useState<'loading' | 'ok' | 'error'>('loading')

  // Prefetch chips data immediately so step 3 is instant when the user arrives
  useEffect(() => {
    api.get<DietaryRestriction[]>('/dietary-restrictions')
      .then(r => { setAllRestrictions(r.data); setRestrictionsStatus('ok') })
      .catch(() => setRestrictionsStatus('error'))
    api.get<HealthCondition[]>('/health-conditions')
      .then(r => { setAllConditions(r.data); setConditionsStatus('ok') })
      .catch(() => setConditionsStatus('error'))
  }, [])

  /* ── Navigation ────────────────────────────────────── */
  const goNext = () => {
    if (step === 1) {
      const w = parseFloat(weightKg)
      const h = parseFloat(heightCm)
      if (!weightKg || !heightCm || isNaN(w) || isNaN(h)) {
        setStep1Error('Weight and height are required — they\'re used to calculate your daily calorie target.')
        return
      }
      if (w < 20 || w > 500) { setStep1Error('Enter a valid weight (20–500 kg).'); return }
      if (h < 50 || h > 300) { setStep1Error('Enter a valid height (50–300 cm).'); return }
      setStep1Error('')
    }
    setStep(s => s + 1)
  }

  const goBack = () => setStep(s => s - 1)

  const handleFinish = async () => {
    if (!userId) return
    setSaving(true); setSaveError('')
    try {
      await api.put(`/users/${userId}/profile`, {
        firstName:     firstName || null,
        weightKg:      parseFloat(weightKg),
        heightCm:      parseFloat(heightCm),
        dateOfBirth:   dateOfBirth || null,
        gender,
        activityLevel,
        healthGoal,
      })
      // Save chip selections in parallel
      await Promise.all([
        ...[...selectedRestrictions].map(id =>
          api.post(`/users/${userId}/profile/dietary-restrictions/${id}`)
        ),
        ...[...selectedConditions].map(id =>
          api.post(`/users/${userId}/profile/health-conditions/${id}`)
        ),
      ])
      navigate('/dashboard', { replace: true })
    } catch {
      setSaveError('Something went wrong saving your profile. Please try again.')
      setSaving(false)
    }
  }

  const toggleRestriction = (id: number) =>
    setSelectedRestrictions(prev => { const s = new Set(prev); s.has(id) ? s.delete(id) : s.add(id); return s })

  const toggleCondition = (id: number) =>
    setSelectedConditions(prev => { const s = new Set(prev); s.has(id) ? s.delete(id) : s.add(id); return s })

  /* ── Shared chip button ────────────────────────────── */
  function ChipBtn({ label, selected, style, onClick }: {
    label: string; selected: boolean
    style: { bg: string; text: string; border: string }
    onClick: () => void
  }) {
    return (
      <button type="button" onClick={onClick}
        className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-sm font-semibold border transition-all duration-150"
        style={selected
          ? { background: style.bg, color: style.text, borderColor: style.border }
          : { background: 'white', color: '#94a3b8', borderColor: '#e2e8f0' }}>
        {selected && (
          <svg className="w-3 h-3 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7"/>
          </svg>
        )}
        {label}
      </button>
    )
  }

  return (
    <div className="relative min-h-screen flex flex-col items-center justify-center px-4 py-12 overflow-hidden bg-black">

      {/* Video background */}
      <video
        autoPlay loop muted playsInline
        style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', zIndex: 0 }}
      >
        <source src={VIDEO_SRC} type="video/mp4" />
      </video>

      {/* Dark veil */}
      <div className="absolute inset-0 bg-black/55 z-[1]" />

      {/* Subtle dot grid */}
      <div className="fixed inset-0 pointer-events-none opacity-[0.04] z-[2]"
           style={{ backgroundImage: 'radial-gradient(circle, white 1px, transparent 1px)', backgroundSize: '28px 28px' }} />

      {/* Logo */}
      <motion.div
        initial={{ opacity: 0, y: -16 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-8 text-center relative z-10"
      >
        <div className="inline-flex items-center gap-2.5">
          <div className="w-10 h-10 rounded-xl flex items-center justify-center shadow-lg"
               style={{ background: 'rgba(255,255,255,.15)', border: '1px solid rgba(255,255,255,.3)' }}>
            <span className="text-white font-black text-lg">N</span>
          </div>
          <span className="text-white font-black text-xl tracking-tight">NutriCook</span>
        </div>
        <p className="text-emerald-300 text-sm mt-2">Set up your profile in 3 quick steps</p>
      </motion.div>

      {/* Card */}
      <motion.div
        initial={{ opacity: 0, y: 24, scale: 0.97 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ delay: 0.1, duration: 0.4, ease: [.4,0,.2,1] }}
        className="relative z-10 w-full max-w-lg rounded-3xl overflow-hidden"
        style={{
          background: 'rgba(255,255,255,0.93)',
          backdropFilter: 'blur(28px)',
          WebkitBackdropFilter: 'blur(28px)',
          border: '1px solid rgba(255,255,255,0.55)',
          boxShadow: '0 32px 80px rgba(0,0,0,.55)',
        }}
      >

        {/* Step indicator */}
        <div className="px-8 pt-8 pb-5 border-b border-slate-100">
          <div className="flex items-start">
            {STEPS.map((s, i) => (
              <div key={s.n} className="flex items-start flex-1">
                <div className="flex flex-col items-center">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-black transition-all duration-300 ${
                    step > s.n
                      ? 'bg-brand-600 text-white'
                      : step === s.n
                      ? 'bg-brand-600 text-white ring-4 ring-brand-100'
                      : 'bg-slate-100 text-slate-400'
                  }`}>
                    {step > s.n
                      ? <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7"/>
                        </svg>
                      : s.n}
                  </div>
                  <span className={`text-[10px] font-semibold mt-1.5 whitespace-nowrap transition-colors ${
                    step === s.n ? 'text-brand-600' : step > s.n ? 'text-slate-400' : 'text-slate-300'
                  }`}>
                    {s.label}
                  </span>
                </div>
                {i < STEPS.length - 1 && (
                  <div className={`flex-1 h-0.5 mt-4 mx-3 rounded-full transition-all duration-500 ${
                    step > s.n ? 'bg-brand-500' : 'bg-slate-200'
                  }`} />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Step content */}
        <div className="px-8 pt-6 pb-8">
          <AnimatePresence mode="wait">

            {/* ── STEP 1: Body metrics ─────────────────── */}
            {step === 1 && (
              <motion.div
                key="step1"
                initial={{ opacity: 0, x: 32 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -32 }}
                transition={{ duration: 0.2 }}
              >
                <h2 className="text-xl font-black text-slate-900 mb-1">Body metrics</h2>
                <p className="text-sm text-slate-400 mb-6">Used to compute your personal daily calorie target (TDEE).</p>

                <div className="space-y-4">
                  <div>
                    <label className="label">
                      First name
                      <span className="text-xs font-normal text-slate-400 ml-1">(optional)</span>
                    </label>
                    <input
                      className="input"
                      type="text"
                      placeholder="Anas"
                      value={firstName}
                      onChange={e => setFirstName(e.target.value)}
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="label">
                        Weight (kg) <span className="text-red-400 ml-0.5">*</span>
                      </label>
                      <input
                        className="input"
                        type="number"
                        step="0.1"
                        placeholder="70"
                        value={weightKg}
                        onChange={e => setWeightKg(e.target.value)}
                      />
                    </div>
                    <div>
                      <label className="label">
                        Height (cm) <span className="text-red-400 ml-0.5">*</span>
                      </label>
                      <input
                        className="input"
                        type="number"
                        placeholder="175"
                        value={heightCm}
                        onChange={e => setHeightCm(e.target.value)}
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="label">
                        Date of birth
                        <span className="text-xs font-normal text-slate-400 ml-1">(optional)</span>
                      </label>
                      <input
                        className="input"
                        type="date"
                        value={dateOfBirth}
                        onChange={e => setDateOfBirth(e.target.value)}
                      />
                    </div>
                    <div>
                      <label className="label">Gender</label>
                      <div className="flex gap-2 h-[42px]">
                        {(['MALE', 'FEMALE'] as const).map(g => (
                          <button
                            key={g}
                            type="button"
                            onClick={() => setGender(g)}
                            className={`flex-1 rounded-xl border text-sm font-semibold transition-all ${
                              gender === g
                                ? 'border-brand-500 bg-brand-50 text-brand-700'
                                : 'border-slate-200 text-slate-500 hover:border-slate-300'
                            }`}
                          >
                            {g === 'MALE' ? '♂ Male' : '♀ Female'}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>

                  {step1Error && (
                    <div className="flex items-start gap-2 bg-red-50 border border-red-200 rounded-xl px-4 py-3 text-sm text-red-600">
                      <svg className="w-4 h-4 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                      </svg>
                      {step1Error}
                    </div>
                  )}
                </div>
              </motion.div>
            )}

            {/* ── STEP 2: Goal & Activity ──────────────── */}
            {step === 2 && (
              <motion.div
                key="step2"
                initial={{ opacity: 0, x: 32 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -32 }}
                transition={{ duration: 0.2 }}
              >
                <h2 className="text-xl font-black text-slate-900 mb-1">Your goal</h2>
                <p className="text-sm text-slate-400 mb-5">This shapes how your AI meal plan is structured.</p>

                <div className="space-y-5">
                  {/* Health goal */}
                  <div>
                    <p className="label mb-2">What are you working towards?</p>
                    <div className="grid grid-cols-2 gap-2">
                      {GOAL_OPTIONS.map(({ value, label, icon, color }) => (
                        <button
                          key={value}
                          type="button"
                          onClick={() => setHealthGoal(value)}
                          className={`relative flex flex-col items-center gap-1.5 p-4 rounded-xl border transition-all text-center ${
                            healthGoal === value
                              ? 'border-brand-400 bg-brand-50/70 shadow-sm'
                              : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                          }`}
                        >
                          <span className="text-2xl">{icon}</span>
                          <span className={`text-xs font-semibold leading-tight ${healthGoal === value ? 'text-brand-700' : 'text-slate-700'}`}>
                            {label}
                          </span>
                          {healthGoal === value && (
                            <div className="absolute top-2 right-2 w-4 h-4 rounded-full flex items-center justify-center"
                                 style={{ background: color }}>
                              <svg className="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7"/>
                              </svg>
                            </div>
                          )}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Activity level */}
                  <div>
                    <p className="label mb-2">How active are you?</p>
                    <div className="space-y-1.5">
                      {ACTIVITY_OPTIONS.map(({ value, label, sub, icon }) => (
                        <button
                          key={value}
                          type="button"
                          onClick={() => setActivityLevel(value)}
                          className={`w-full flex items-center gap-3 p-3 rounded-xl border transition-all text-left ${
                            activityLevel === value
                              ? 'border-brand-400 bg-brand-50/70'
                              : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                          }`}
                        >
                          <span className="text-lg shrink-0">{icon}</span>
                          <div className="flex-1 min-w-0">
                            <p className={`text-sm font-semibold ${activityLevel === value ? 'text-brand-700' : 'text-slate-800'}`}>
                              {label}
                            </p>
                            <p className="text-xs text-slate-400">{sub}</p>
                          </div>
                          {activityLevel === value && (
                            <svg className="w-4 h-4 text-brand-600 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7"/>
                            </svg>
                          )}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              </motion.div>
            )}

            {/* ── STEP 3: Dietary preferences ─────────── */}
            {step === 3 && (
              <motion.div
                key="step3"
                initial={{ opacity: 0, x: 32 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -32 }}
                transition={{ duration: 0.2 }}
              >
                <h2 className="text-xl font-black text-slate-900 mb-1">Dietary preferences</h2>
                <p className="text-sm text-slate-400 mb-5">
                  The AI uses these when building your meal plans.
                  <span className="block mt-0.5">You can update them any time in your profile.</span>
                </p>

                <div className="space-y-5">
                  {/* Dietary restrictions */}
                  <div>
                    <p className="label mb-2">Diet & intolerances</p>
                    {restrictionsStatus === 'loading' && (
                      <p className="text-sm text-slate-400">Loading options…</p>
                    )}
                    {restrictionsStatus === 'error' && (
                      <p className="text-sm text-amber-600">
                        Could not load options — the backend may not be running.
                        You can set these later in your profile.
                      </p>
                    )}
                    {restrictionsStatus === 'ok' && (
                      <div className="flex flex-wrap gap-2">
                        {allRestrictions.map(r => (
                          <Tooltip key={r.id} title={r.description ?? ''} placement="top" arrow>
                            <span>
                              <ChipBtn
                                label={r.name}
                                selected={selectedRestrictions.has(r.id)}
                                style={RESTRICTION_STYLE[r.type] ?? RESTRICTION_STYLE.PREFERENCE}
                                onClick={() => toggleRestriction(r.id)}
                              />
                            </span>
                          </Tooltip>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Health conditions */}
                  <div>
                    <p className="label mb-2">Health conditions</p>
                    {conditionsStatus === 'loading' && (
                      <p className="text-sm text-slate-400">Loading options…</p>
                    )}
                    {conditionsStatus === 'error' && (
                      <p className="text-sm text-amber-600">
                        Could not load options — you can set these later in your profile.
                      </p>
                    )}
                    {conditionsStatus === 'ok' && (
                      <div className="flex flex-wrap gap-2">
                        {allConditions.map(c => (
                          <Tooltip key={c.id} title={c.description ?? ''} placement="top" arrow>
                            <span>
                              <ChipBtn
                                label={c.name}
                                selected={selectedConditions.has(c.id)}
                                style={{ bg: '#faf5ff', text: '#7c3aed', border: '#c4b5fd' }}
                                onClick={() => toggleCondition(c.id)}
                              />
                            </span>
                          </Tooltip>
                        ))}
                      </div>
                    )}
                  </div>

                  {saveError && (
                    <div className="flex items-start gap-2 bg-red-50 border border-red-200 rounded-xl px-4 py-3 text-sm text-red-600">
                      <svg className="w-4 h-4 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                      </svg>
                      {saveError}
                    </div>
                  )}
                </div>
              </motion.div>
            )}

          </AnimatePresence>

          {/* Navigation */}
          <div className="flex items-center justify-between mt-8 pt-5 border-t border-slate-100">
            <div>
              {step > 1 && (
                <button type="button" onClick={goBack}
                        className="text-sm font-semibold text-slate-400 hover:text-slate-600 transition-colors">
                  ← Back
                </button>
              )}
            </div>

            <div className="flex items-center gap-4">
              <Link to="/dashboard"
                    className="text-sm text-slate-400 hover:text-slate-500 transition-colors">
                Skip for now
              </Link>

              {step < 3 ? (
                <button type="button" onClick={goNext} className="btn-primary">
                  Continue →
                </button>
              ) : (
                <button type="button" onClick={handleFinish} disabled={saving} className="btn-primary">
                  {saving ? (
                    <>
                      <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                      </svg>
                      Saving…
                    </>
                  ) : '✓ Finish setup'}
                </button>
              )}
            </div>
          </div>
        </div>
      </motion.div>

      <p className="relative z-10 text-emerald-400/60 text-xs mt-6 text-center">
        Your information is private and used only to personalise your meal plans.
      </p>
    </div>
  )
}
