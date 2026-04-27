import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { motion } from 'framer-motion'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { UserProfile, ActivityLevel, HealthGoal } from '../types'

interface FormData {
  firstName: string
  lastName: string
  weightKg: number
  heightCm: number
  dateOfBirth: string
  gender: 'MALE' | 'FEMALE'
  activityLevel: ActivityLevel
  healthGoal: HealthGoal
}

const ACTIVITY_OPTIONS: { value: ActivityLevel; label: string; sub: string }[] = [
  { value: 'SEDENTARY',        label: 'Sedentary',          sub: 'Desk job, little exercise' },
  { value: 'LIGHTLY_ACTIVE',   label: 'Lightly active',     sub: '1–3 workouts / week' },
  { value: 'MODERATELY_ACTIVE',label: 'Moderately active',  sub: '3–5 workouts / week' },
  { value: 'VERY_ACTIVE',      label: 'Very active',        sub: '6–7 workouts / week' },
  { value: 'EXTRA_ACTIVE',     label: 'Extra active',       sub: 'Athlete / physical job' },
]

const GOAL_OPTIONS: { value: HealthGoal; label: string; icon: string }[] = [
  { value: 'LOSE_WEIGHT',    label: 'Lose weight',         icon: '⬇️' },
  { value: 'MAINTAIN_WEIGHT',label: 'Maintain weight',     icon: '⚖️' },
  { value: 'GAIN_MUSCLE',    label: 'Gain muscle',         icon: '💪' },
  { value: 'IMPROVE_HEALTH', label: 'Improve overall health', icon: '❤️' },
]

export default function ProfilePage() {
  const { userId } = useAuth()
  const [saved, setSaved]   = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError]   = useState('')

  const { register, handleSubmit, reset, watch, formState: { errors } } =
    useForm<FormData>({ defaultValues: { gender: 'MALE', activityLevel: 'SEDENTARY', healthGoal: 'IMPROVE_HEALTH' } })

  const selectedGoal    = watch('healthGoal')
  const selectedActivity = watch('activityLevel')

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
      setError('Failed to save profile. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">

      {/* Header */}
      <div>
        <h1 className="page-title">Your Profile</h1>
        <p className="page-subtitle">Used to calculate your personalised daily calorie target (TDEE).</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">

        {/* Personal info */}
        <div className="card space-y-4">
          <h2 className="font-semibold text-slate-900">Personal information</h2>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">First name</label>
              <input className="input" type="text" placeholder="Anas" {...register('firstName')} />
            </div>
            <div>
              <label className="label">Last name</label>
              <input className="input" type="text" placeholder="Bougrine" {...register('lastName')} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">Date of birth</label>
              <input className="input" type="date" {...register('dateOfBirth')} />
            </div>
            <div>
              <label className="label">Gender</label>
              <div className="flex gap-2">
                {(['MALE', 'FEMALE'] as const).map(g => (
                  <label
                    key={g}
                    className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl border text-sm font-medium cursor-pointer transition-all ${
                      watch('gender') === g
                        ? 'border-brand-500 bg-brand-50 text-brand-700'
                        : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                    }`}
                  >
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
          <h2 className="font-semibold text-slate-900">Body metrics</h2>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">Weight (kg)</label>
              <input
                className="input"
                type="number"
                step="0.1"
                placeholder="70"
                {...register('weightKg', { required: 'Required', min: 20, max: 300, valueAsNumber: true })}
              />
              {errors.weightKg && <p className="text-red-500 text-xs mt-1">Valid weight required (20–300 kg)</p>}
            </div>
            <div>
              <label className="label">Height (cm)</label>
              <input
                className="input"
                type="number"
                placeholder="175"
                {...register('heightCm', { required: 'Required', min: 50, max: 300, valueAsNumber: true })}
              />
              {errors.heightCm && <p className="text-red-500 text-xs mt-1">Valid height required (50–300 cm)</p>}
            </div>
          </div>
        </div>

        {/* Activity level */}
        <div className="card space-y-4">
          <h2 className="font-semibold text-slate-900">Activity level</h2>
          <div className="space-y-2">
            {ACTIVITY_OPTIONS.map(({ value, label, sub }) => (
              <label
                key={value}
                className={`flex items-center gap-4 p-4 rounded-xl border cursor-pointer transition-all ${
                  selectedActivity === value
                    ? 'border-brand-500 bg-brand-50'
                    : 'border-slate-200 bg-white hover:border-slate-300'
                }`}
              >
                <input type="radio" value={value} {...register('activityLevel')} className="accent-brand-600" />
                <div>
                  <p className={`text-sm font-semibold ${selectedActivity === value ? 'text-brand-700' : 'text-slate-900'}`}>{label}</p>
                  <p className="text-xs text-slate-400">{sub}</p>
                </div>
              </label>
            ))}
          </div>
        </div>

        {/* Health goal */}
        <div className="card space-y-4">
          <h2 className="font-semibold text-slate-900">Health goal</h2>
          <div className="grid grid-cols-2 gap-3">
            {GOAL_OPTIONS.map(({ value, label, icon }) => (
              <label
                key={value}
                className={`flex items-center gap-3 p-4 rounded-xl border cursor-pointer transition-all ${
                  selectedGoal === value
                    ? 'border-brand-500 bg-brand-50'
                    : 'border-slate-200 bg-white hover:border-slate-300'
                }`}
              >
                <input type="radio" value={value} {...register('healthGoal')} className="accent-brand-600 sr-only" />
                <span className="text-2xl">{icon}</span>
                <span className={`text-sm font-medium ${selectedGoal === value ? 'text-brand-700' : 'text-slate-700'}`}>{label}</span>
              </label>
            ))}
          </div>
        </div>

        {error && (
          <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl px-4 py-3">
            <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            {error}
          </div>
        )}

        <div className="flex items-center gap-4">
          <button type="submit" disabled={loading} className="btn-primary px-8">
            {loading ? 'Saving…' : 'Save profile'}
          </button>
          {saved && (
            <motion.div
              initial={{ opacity: 0, x: -8 }}
              animate={{ opacity: 1, x: 0 }}
              className="flex items-center gap-1.5 text-brand-600 text-sm font-medium"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7"/>
              </svg>
              Profile saved
            </motion.div>
          )}
        </div>
      </form>
    </div>
  )
}
