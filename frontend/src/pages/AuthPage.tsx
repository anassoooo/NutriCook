import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { AuthResponse } from '../types'
import { Wordmark } from '../components/Brand'
import DayPlate, { RoleKey } from '../components/DayPlate'
import { SAMPLE_EATEN, SAMPLE_MEALS, SAMPLE_TOTAL } from '../lib/samplePlan'

interface FormData {
  email: string
  password: string
}

export default function AuthPage() {
  const [isLogin, setIsLogin] = useState(true)
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormData>()

  const switchMode = (val: boolean) => { setIsLogin(val); setServerError(''); reset() }

  const onSubmit = async (data: FormData) => {
    setServerError(''); setLoading(true)
    try {
      const res = await api.post<AuthResponse>(isLogin ? '/auth/login' : '/auth/register', data)
      login(res.data)
      navigate(isLogin ? '/dashboard' : '/onboarding')
    } catch (err) {
      const e = toApiError(err)
      if (isLogin && e.code === 'UNAUTHENTICATED') setServerError('That email and password do not match an account.')
      else if (e.code === 'VALIDATION_FAILED' && e.fieldErrors.length > 0) setServerError(e.fieldErrors.map(f => f.message).join(' '))
      else setServerError(e.message)
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[1fr_minmax(420px,520px)]">
      {/* The product, not a stock photo */}
      <aside className="relative hidden flex-col justify-between bg-ink px-14 py-12 text-chalk lg:flex">
        <Wordmark light />
        <div className="mx-auto w-full max-w-[400px]">
          <DayPlate meals={SAMPLE_MEALS} eatenKcal={SAMPLE_EATEN} plannedKcal={SAMPLE_TOTAL} targetKcal={null} />
          <div className="mt-5 [&_li]:!text-chalk/70">
            <RoleKey />
          </div>
        </div>
        <p className="max-w-[40ch] font-heading text-2xl font-semibold leading-snug">
          Five meals a day, built around your body, your goal and what you can't eat.
        </p>
      </aside>

      <main className="flex min-h-screen flex-col px-6 py-8 sm:px-12 lg:py-12">
        <div className="lg:hidden"><Wordmark /></div>
        <div className="my-auto w-full max-w-sm py-10 lg:mx-auto">
          <h1 className="page-title">{isLogin ? 'Welcome back' : 'Create your account'}</h1>
          <p className="page-subtitle">
            {isLogin ? 'Sign in to see today’s plate.' : 'Three short questions after this, then your first day is planned.'}
          </p>

          <div className="mt-8 grid grid-cols-2 rounded-ctl border border-line p-1" role="tablist" aria-label="Account">
            {[true, false].map(mode => (
              <button
                key={String(mode)}
                type="button"
                role="tab"
                aria-selected={isLogin === mode}
                onClick={() => switchMode(mode)}
                className={`rounded-[7px] py-2 text-sm font-semibold transition-colors ${isLogin === mode ? 'bg-ink text-chalk' : 'text-ink-soft hover:text-ink'}`}
              >
                {mode ? 'Sign in' : 'Sign up'}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
            <label className="block">
              <span className="label">Email</span>
              <input
                type="email"
                autoComplete="email"
                className="input"
                aria-invalid={!!errors.email}
                {...register('email', { required: 'Enter your email.' })}
              />
              {errors.email && <span className="mt-1 block text-xs text-alert">{errors.email.message}</span>}
            </label>
            <label className="block">
              <span className="label">Password</span>
              <input
                type="password"
                autoComplete={isLogin ? 'current-password' : 'new-password'}
                className="input"
                aria-invalid={!!errors.password}
                {...register('password', {
                  required: 'Enter your password.',
                  minLength: { value: 8, message: 'Use at least 8 characters.' },
                })}
              />
              {errors.password && <span className="mt-1 block text-xs text-alert">{errors.password.message}</span>}
            </label>

            {serverError && <p role="alert" className="banner-error">{serverError}</p>}

            <button type="submit" disabled={loading} className="btn-primary w-full py-3">
              {loading ? (isLogin ? 'Signing in…' : 'Creating your account…') : isLogin ? 'Sign in' : 'Create account'}
            </button>
          </form>
        </div>
        <p className="text-xs text-ink-faint">Your data stays in your account. Plans are not medical advice.</p>
      </main>
    </div>
  )
}
