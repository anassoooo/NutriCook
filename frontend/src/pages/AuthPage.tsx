import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { motion, AnimatePresence } from 'framer-motion'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { AuthResponse } from '../types'

const VIDEO_SRC =
  'https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260403_050628_c4e32401-fab4-4a27-b7a8-6e9291cd5959.mp4'

interface FormData { email: string; password: string }

const FEATURES = [
  { icon: '🤖', text: 'AI-personalised meal plans' },
  { icon: '📊', text: 'Macro & calorie tracking'   },
  { icon: '🏆', text: 'Achievement system'          },
  { icon: '📍', text: 'Nearby healthy restaurants'  },
]

const fadeUp = (delay = 0) => ({
  hidden: { opacity: 0, y: 16 },
  show:   { opacity: 1, y: 0, transition: { duration: 0.7, ease: 'easeOut', delay } },
})

export default function AuthPage() {
  const [isLogin, setIsLogin]         = useState(true)
  const [serverError, setServerError] = useState('')
  const [loading, setLoading]         = useState(false)
  const { login } = useAuth()
  const navigate   = useNavigate()

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormData>()

  const switchMode = (val: boolean) => { setIsLogin(val); setServerError(''); reset() }

  const onSubmit = async (data: FormData) => {
    setServerError(''); setLoading(true)
    try {
      const res = await api.post<AuthResponse>(
        isLogin ? '/auth/login' : '/auth/register', data
      )
      login(res.data)
      navigate(isLogin ? '/dashboard' : '/onboarding')
    } catch (err: any) {
      const msg = err.response?.data?.message ?? err.response?.data ?? 'Something went wrong.'
      setServerError(typeof msg === 'string' ? msg : 'Request failed.')
    } finally { setLoading(false) }
  }

  return (
    <div className="relative w-full min-h-screen flex items-center justify-center overflow-hidden bg-black">

      {/* Video background */}
      <video
        autoPlay loop muted playsInline
        style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', zIndex: 0 }}
      >
        <source src={VIDEO_SRC} type="video/mp4" />
      </video>

      {/* Dark veil */}
      <div className="absolute inset-0 bg-black/55 z-[1]" />

      {/* Content */}
      <div className="relative z-10 w-full flex flex-col items-center px-4 py-12">

        {/* Logo */}
        <motion.div
          variants={fadeUp(0)}
          initial="hidden"
          animate="show"
          className="flex items-center gap-2.5 mb-8 select-none"
        >
          <div style={{
            width: 36, height: 36, borderRadius: 10,
            background: 'linear-gradient(135deg,#16a34a,#0d9488)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 2px 12px rgba(22,163,74,.5)',
          }}>
            <span style={{ color: 'white', fontWeight: 900, fontSize: 15 }}>N</span>
          </div>
          <span className="text-2xl font-black tracking-tight text-white" style={{ letterSpacing: '-0.02em' }}>
            NutriCook
          </span>
        </motion.div>

        {/* Main card */}
        <motion.div
          initial={{ opacity: 0, y: 24, scale: 0.97 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ duration: 0.5, delay: 0.15, ease: [.4, 0, .2, 1] }}
          className="liquid-glass rounded-3xl w-full max-w-[420px] overflow-hidden"
          style={{ boxShadow: '0 32px 80px rgba(0,0,0,.5)' }}
        >
          {/* Header */}
          <div className="px-8 pt-8 pb-5 text-center">
            <h1 className="text-2xl font-black text-white tracking-tight">
              {isLogin ? 'Welcome back' : 'Create account'}
            </h1>
            <p className="text-white/70 text-sm mt-1">Your AI-powered diet assistant</p>
          </div>

          {/* Features strip */}
          <div className="mx-8 mb-6 grid grid-cols-2 gap-2">
            {FEATURES.map(f => (
              <div
                key={f.text}
                className="flex items-center gap-2 rounded-xl px-3 py-2"
                style={{ background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.1)' }}
              >
                <span className="text-base">{f.icon}</span>
                <span className="text-xs font-medium text-white/70 leading-tight">{f.text}</span>
              </div>
            ))}
          </div>

          {/* Form */}
          <div className="px-8 pb-8">

            {/* Tab toggle */}
            <div
              className="flex rounded-xl p-1 mb-5"
              style={{ background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.08)' }}
            >
              {[{ label: 'Sign in', val: true }, { label: 'Sign up', val: false }].map(({ label, val }) => (
                <button
                  key={label}
                  type="button"
                  onClick={() => switchMode(val)}
                  className={`flex-1 py-2 rounded-lg text-sm font-bold transition-all ${
                    isLogin === val ? 'text-white' : 'text-white/55 hover:text-white/80'
                  }`}
                  style={isLogin === val
                    ? { background: 'rgba(255,255,255,0.15)', boxShadow: '0 1px 3px rgba(0,0,0,.2)' }
                    : {}}
                >
                  {label}
                </button>
              ))}
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">

              <div>
                <label className="block text-sm font-semibold text-white/80 mb-2">Email address</label>
                <input
                  type="email"
                  placeholder="you@example.com"
                  autoComplete="email"
                  className="w-full rounded-xl px-4 py-3 text-sm text-white placeholder-white/30
                             focus:outline-none focus:ring-2 focus:ring-emerald-500/50 transition-all duration-300"
                  style={{ background: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.15)' }}
                  {...register('email', { required: 'Email is required' })}
                />
                {errors.email && <p className="text-red-400 text-xs mt-1.5">{errors.email.message}</p>}
              </div>

              <div>
                <label className="block text-sm font-semibold text-white/80 mb-2">Password</label>
                <input
                  type="password"
                  placeholder="••••••••"
                  autoComplete={isLogin ? 'current-password' : 'new-password'}
                  className="w-full rounded-xl px-4 py-3 text-sm text-white placeholder-white/30
                             focus:outline-none focus:ring-2 focus:ring-emerald-500/50 transition-all duration-300"
                  style={{ background: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.15)' }}
                  {...register('password', {
                    required: 'Password is required',
                    minLength: { value: 8, message: 'At least 8 characters' },
                  })}
                />
                {errors.password && <p className="text-red-400 text-xs mt-1.5">{errors.password.message}</p>}
              </div>

              <AnimatePresence>
                {serverError && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    className="flex items-start gap-2.5 rounded-xl px-4 py-3"
                    style={{ background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.3)' }}
                  >
                    <svg className="w-4 h-4 text-red-400 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                    <p className="text-red-300 text-sm">{serverError}</p>
                  </motion.div>
                )}
              </AnimatePresence>

              <button type="submit" disabled={loading} className="btn-primary w-full py-3 text-[15px]">
                {loading ? (
                  <>
                    <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                    </svg>
                    {isLogin ? 'Signing in…' : 'Creating account…'}
                  </>
                ) : isLogin ? 'Sign in →' : 'Create account →'}
              </button>
            </form>
          </div>

          {/* Footer */}
          <div
            className="px-8 py-4 flex items-center justify-center gap-2"
            style={{ borderTop: '1px solid rgba(255,255,255,0.08)' }}
          >
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-xs text-white/55">Powered by Groq AI · Llama 3.3 70B</span>
          </div>
        </motion.div>

        {/* Back to home */}
        <motion.div variants={fadeUp(0.5)} initial="hidden" animate="show">
          <Link to="/" className="mt-6 inline-block text-sm text-white/60 hover:text-white/85 transition-colors">
            ← Back to home
          </Link>
        </motion.div>

      </div>
    </div>
  )
}
