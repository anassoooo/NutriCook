import { useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { motion } from 'framer-motion'
import Tooltip from '@mui/material/Tooltip'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { UserProfile, ActivityLevel, HealthGoal } from '../types'

interface FormData {
  firstName:     string
  lastName:      string
  weightKg:      number
  heightCm:      number
  dateOfBirth:   string
  gender:        'MALE' | 'FEMALE'
  activityLevel: ActivityLevel
  healthGoal:    HealthGoal
}

const ACTIVITY_OPTIONS: { value: ActivityLevel; label: string; sub: string; icon: string }[] = [
  { value: 'SEDENTARY',         label: 'Sedentary',         sub: 'Desk job, little exercise',  icon: '🪑' },
  { value: 'LIGHTLY_ACTIVE',    label: 'Lightly active',    sub: '1–3 workouts / week',        icon: '🚶' },
  { value: 'MODERATELY_ACTIVE', label: 'Moderately active', sub: '3–5 workouts / week',        icon: '🏃' },
  { value: 'VERY_ACTIVE',       label: 'Very active',       sub: '6–7 workouts / week',        icon: '⚡' },
  { value: 'EXTRA_ACTIVE',      label: 'Extra active',      sub: 'Athlete / physical job',     icon: '🏋️' },
]

const GOAL_OPTIONS: { value: HealthGoal; label: string; icon: string; color: string }[] = [
  { value: 'LOSE_WEIGHT',     label: 'Lose weight',    icon: '⬇️', color: '#3b82f6' },
  { value: 'MAINTAIN_WEIGHT', label: 'Maintain',       icon: '⚖️', color: '#16a34a' },
  { value: 'GAIN_MUSCLE',     label: 'Gain muscle',    icon: '💪', color: '#f97316' },
  { value: 'IMPROVE_HEALTH',  label: 'Improve health', icon: '❤️', color: '#ef4444' },
]

/* Fields the user must actively fill — radio defaults don't count */
const COMPLETION_STEPS = [
  { key: 'firstName',   label: 'Name'     },
  { key: 'weightKg',    label: 'Weight'   },
  { key: 'heightCm',    label: 'Height'   },
  { key: 'dateOfBirth', label: 'Birthday' },
]

function isDone(v: any): boolean {
  if (v === undefined || v === null || v === '') return false
  if (typeof v === 'number') return !isNaN(v) && v > 0
  return true
}

function bmiInfo(weight: number, height: number) {
  const bmi   = weight / Math.pow(height / 100, 2)
  const value = bmi.toFixed(1)
  if (bmi < 18.5) return { value, label: 'Underweight', color: '#3b82f6', bg: '#eff6ff', bar: 20 }
  if (bmi < 25)   return { value, label: 'Normal',      color: '#16a34a', bg: '#f0fdf4', bar: 45 }
  if (bmi < 30)   return { value, label: 'Overweight',  color: '#f59e0b', bg: '#fffbeb', bar: 68 }
                  return { value, label: 'Obese',        color: '#ef4444', bg: '#fef2f2', bar: 88 }
}

function RequiredDot() {
  return (
    <Tooltip title="Required field" placement="top" arrow>
      <span className="text-red-400 ml-0.5 cursor-default">*</span>
    </Tooltip>
  )
}

export default function ProfilePage() {
  const { userId, email } = useAuth()
  const [saved,   setSaved]   = useState(false)
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState('')

  const { control, register, handleSubmit, reset, formState: { errors } } =
    useForm<FormData>({ defaultValues: { gender: 'MALE', activityLevel: 'SEDENTARY', healthGoal: 'IMPROVE_HEALTH' } })

  /* useWatch with control — reliably re-renders after reset() from API */
  const all = useWatch({ control })
  const watchFirst       = (all.firstName      ?? '') as string
  const watchLast        = (all.lastName       ?? '') as string
  const watchGender      = (all.gender         ?? 'MALE') as 'MALE' | 'FEMALE'
  const selectedActivity = (all.activityLevel  ?? 'SEDENTARY') as ActivityLevel
  const selectedGoal     = (all.healthGoal     ?? 'IMPROVE_HEALTH') as HealthGoal
  const watchWeight      = all.weightKg  as number | undefined
  const watchHeight      = all.heightCm  as number | undefined

  const bmi = watchWeight && watchHeight && !isNaN(Number(watchWeight)) && !isNaN(Number(watchHeight))
    ? bmiInfo(Number(watchWeight), Number(watchHeight))
    : null

  const displayName = [watchFirst, watchLast].filter(Boolean).join(' ') || 'Your'
  const initial     = (watchFirst || email || 'U').charAt(0).toUpperCase()

  const completedCount = COMPLETION_STEPS.filter(s => isDone((all as any)[s.key])).length
  const completionPct  = Math.round((completedCount / COMPLETION_STEPS.length) * 100)

  useEffect(() => {
    if (!userId) return
    api.get<UserProfile>(`/users/${userId}/profile`).then(r => {
      const p = r.data
      reset({
        firstName:     p.firstName     ?? '',
        lastName:      p.lastName      ?? '',
        weightKg:      p.weightKg      ?? undefined,
        heightCm:      p.heightCm      ?? undefined,
        dateOfBirth:   p.dateOfBirth   ?? '',
        gender:        p.gender        ?? 'MALE',
        activityLevel: p.activityLevel ?? 'SEDENTARY',
        healthGoal:    p.healthGoal    ?? 'IMPROVE_HEALTH',
      })
    }).catch(() => {})
  }, [userId, reset])

  const onSubmit = async (data: FormData) => {
    if (!userId) return
    setLoading(true); setError('')
    try {
      await api.put(`/users/${userId}/profile`, data)
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    } catch {
      setError('Failed to save. Please check your inputs and try again.')
    } finally { setLoading(false) }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">

      {/* ── Profile header ──────────────────────── */}
      <div className="relative overflow-hidden rounded-3xl p-7 text-white"
           style={{ background: 'linear-gradient(135deg,#1e3a5f 0%,#1e40af 50%,#1d4ed8 100%)' }}>
        <div className="absolute inset-0 opacity-10"
             style={{ backgroundImage: 'radial-gradient(circle, white 1px, transparent 1px)', backgroundSize: '24px 24px' }} />

        <div className="relative z-10 flex items-center gap-5 flex-wrap">
          {/* Avatar */}
          <div className="w-20 h-20 rounded-2xl flex items-center justify-center text-3xl font-black shrink-0 shadow-lg select-none"
               style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)' }}>
            {initial}
          </div>

          <div className="flex-1 min-w-0">
            {/* Name updates live as user types */}
            <h1 className="text-2xl font-black tracking-tight">{displayName} Profile</h1>
            <p className="text-blue-200 text-sm mt-0.5 truncate">{email}</p>

            {/* Completion pills — only track explicitly-filled fields */}
            <div className="flex items-center gap-1.5 mt-3 flex-wrap">
              {COMPLETION_STEPS.map(s => {
                const done = isDone((all as any)[s.key])
                return (
                  <span key={s.key}
                        className="text-[10px] font-semibold px-2 py-0.5 rounded-full transition-all"
                        style={{
                          background: done ? 'rgba(255,255,255,.28)' : 'rgba(255,255,255,.08)',
                          color:      done ? 'white'                  : 'rgba(255,255,255,.38)',
                        }}>
                    {done ? '✓ ' : ''}{s.label}
                  </span>
                )
              })}
            </div>
          </div>

          {/* Completion % */}
          <Tooltip title={`${completedCount} of ${COMPLETION_STEPS.length} key fields filled`} placement="left" arrow>
            <div className="text-center shrink-0 cursor-default">
              <p className="text-3xl font-black">{completionPct}%</p>
              <p className="text-blue-200 text-xs mt-0.5">complete</p>
            </div>
          </Tooltip>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">

        {/* Personal info */}
        <div className="card space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
            <span className="text-lg">👤</span>
            <div>
              <h2 className="section-title">Personal information</h2>
              <p className="text-xs text-slate-400 mt-0.5">Fields marked <span className="text-red-400">*</span> are required</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">
                First name
                <span className="text-xs font-normal text-slate-400 ml-1">(optional)</span>
              </label>
              <input className="input" type="text" placeholder="Anas" {...register('firstName')} />
            </div>
            <div>
              <label className="label">
                Last name
                <span className="text-xs font-normal text-slate-400 ml-1">(optional)</span>
              </label>
              <input className="input" type="text" placeholder="Bougrine" {...register('lastName')} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">
                Date of birth
                <span className="text-xs font-normal text-slate-400 ml-1">(optional)</span>
              </label>
              <input className="input" type="date" {...register('dateOfBirth')} />
            </div>
            <div>
              <label className="label">Gender</label>
              <div className="flex gap-2">
                {(['MALE', 'FEMALE'] as const).map(g => (
                  <label key={g}
                    className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl border text-sm font-semibold cursor-pointer transition-all ${
                      watchGender === g
                        ? 'border-brand-500 bg-brand-50 text-brand-700'
                        : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300'
                    }`}>
                    <input type="radio" value={g} {...register('gender')} className="sr-only" />
                    {g === 'MALE' ? '♂ Male' : '♀ Female'}
                  </label>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Body metrics */}
        <div className="card space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
            <span className="text-lg">📏</span>
            <div>
              <h2 className="section-title">Body metrics</h2>
              <p className="text-xs text-slate-400 mt-0.5">Used to calculate your daily calorie target (TDEE)</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">Weight (kg) <RequiredDot /></label>
              <input className="input" type="number" step="0.1" placeholder="70"
                {...register('weightKg', { required: true, min: 20, max: 500, valueAsNumber: true })} />
              {errors.weightKg && <p className="text-red-500 text-xs mt-1">Enter a valid weight (20–500 kg)</p>}
            </div>
            <div>
              <label className="label">Height (cm) <RequiredDot /></label>
              <input className="input" type="number" placeholder="175"
                {...register('heightCm', { required: true, min: 50, max: 300, valueAsNumber: true })} />
              {errors.heightCm && <p className="text-red-500 text-xs mt-1">Enter a valid height (50–300 cm)</p>}
            </div>
          </div>

          {/* Live BMI */}
          {bmi && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              className="rounded-xl p-4"
              style={{ background: bmi.bg, border: `1px solid ${bmi.color}30` }}
            >
              <div className="flex items-center justify-between mb-2">
                <div>
                  <p className="section-label">Body Mass Index (BMI)</p>
                  <p className="text-2xl font-black mt-1" style={{ color: bmi.color }}>{bmi.value}</p>
                </div>
                <span className="text-sm font-bold px-3 py-1 rounded-full"
                      style={{ background: `${bmi.color}20`, color: bmi.color }}>
                  {bmi.label}
                </span>
              </div>
              <div className="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                <div className="h-full rounded-full transition-all duration-500"
                     style={{ width: `${bmi.bar}%`, background: bmi.color }} />
              </div>
              <div className="flex justify-between text-[10px] text-slate-400 mt-1.5 font-medium">
                <span>Underweight</span><span>Normal</span><span>Overweight</span><span>Obese</span>
              </div>
            </motion.div>
          )}
        </div>

        {/* Activity level */}
        <div className="card space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
            <span className="text-lg">⚡</span>
            <div>
              <h2 className="section-title">Activity level</h2>
              <p className="text-xs text-slate-400 mt-0.5">How active are you on an average week?</p>
            </div>
          </div>
          <div className="space-y-2">
            {ACTIVITY_OPTIONS.map(({ value, label, sub, icon }) => (
              <label key={value}
                className={`flex items-center gap-4 p-3.5 rounded-xl border cursor-pointer transition-all ${
                  selectedActivity === value
                    ? 'border-brand-400 bg-brand-50/70 shadow-sm'
                    : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/60'
                }`}>
                <span className="text-xl shrink-0">{icon}</span>
                <div className="flex-1">
                  <p className={`text-sm font-semibold ${selectedActivity === value ? 'text-brand-700' : 'text-slate-900'}`}>
                    {label}
                  </p>
                  <p className="text-xs text-slate-400">{sub}</p>
                </div>
                <input type="radio" value={value} {...register('activityLevel')} className="accent-brand-600" />
              </label>
            ))}
          </div>
        </div>

        {/* Health goal */}
        <div className="card space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
            <span className="text-lg">🎯</span>
            <div>
              <h2 className="section-title">Health goal</h2>
              <p className="text-xs text-slate-400 mt-0.5">What are you working towards?</p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {GOAL_OPTIONS.map(({ value, label, icon, color }) => (
              <label key={value}
                className={`relative flex flex-col items-center gap-2 p-5 rounded-xl border cursor-pointer transition-all text-center ${
                  selectedGoal === value
                    ? 'border-brand-400 bg-brand-50/70 shadow-sm'
                    : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/60'
                }`}>
                <input type="radio" value={value} {...register('healthGoal')} className="sr-only" />
                <span className="text-3xl">{icon}</span>
                <span className={`text-sm font-semibold leading-tight ${selectedGoal === value ? 'text-brand-700' : 'text-slate-700'}`}>
                  {label}
                </span>
                {selectedGoal === value && (
                  <div className="absolute top-2 right-2 w-5 h-5 rounded-full flex items-center justify-center"
                       style={{ background: color }}>
                    <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7"/>
                    </svg>
                  </div>
                )}
              </label>
            ))}
          </div>
        </div>

        {error && (
          <div className="banner-error">
            <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            {error}
          </div>
        )}

        <div className="flex items-center gap-4 pb-4">
          <button type="submit" disabled={loading} className="btn-primary px-8">
            {loading ? 'Saving…' : 'Save profile'}
          </button>
          {saved && (
            <motion.div
              initial={{ opacity: 0, x: -8 }}
              animate={{ opacity: 1, x: 0 }}
              className="flex items-center gap-1.5 text-brand-600 text-sm font-semibold"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7"/>
              </svg>
              Profile saved!
            </motion.div>
          )}
        </div>
      </form>
    </div>
  )
}
