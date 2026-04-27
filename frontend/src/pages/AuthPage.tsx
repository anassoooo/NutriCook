import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { motion, AnimatePresence } from 'framer-motion'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { AuthResponse } from '../types'

interface FormData { email: string; password: string }

const FOOD_ITEMS = [
  { emoji: '🥑', x: '8%',  y: '12%', dur: '6s',  delay: '0s'    },
  { emoji: '🍎', x: '85%', y: '8%',  dur: '7s',  delay: '1s'    },
  { emoji: '🥦', x: '15%', y: '72%', dur: '5.5s',delay: '0.5s'  },
  { emoji: '🍋', x: '78%', y: '65%', dur: '8s',  delay: '2s'    },
  { emoji: '🥕', x: '50%', y: '5%',  dur: '6.5s',delay: '1.5s'  },
  { emoji: '🍇', x: '92%', y: '40%', dur: '7.5s',delay: '0.8s'  },
  { emoji: '🫐', x: '5%',  y: '45%', dur: '5s',  delay: '2.5s'  },
  { emoji: '🍓', x: '60%', y: '88%', dur: '6s',  delay: '0.3s'  },
  { emoji: '🥝', x: '35%', y: '78%', dur: '7s',  delay: '1.8s'  },
  { emoji: '🌿', x: '72%', y: '22%', dur: '8.5s',delay: '0.6s'  },
]

const FEATURES = [
  { icon: '🤖', text: 'AI-personalised meal plans' },
  { icon: '📊', text: 'Macro & calorie tracking'  },
  { icon: '🏆', text: 'Achievement system'         },
  { icon: '📍', text: 'Nearby healthy restaurants' },
]

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
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden gradient-bg">

      {/* Floating food items */}
      {FOOD_ITEMS.map((item, i) => (
        <span
          key={i}
          className="food-float absolute text-3xl select-none pointer-events-none"
          style={{
            left: item.x, top: item.y,
            '--dur': item.dur, '--delay': item.delay,
            filter: 'drop-shadow(0 2px 8px rgba(0,0,0,.15))',
          } as React.CSSProperties}
        >
          {item.emoji}
        </span>
      ))}

      {/* Glass card */}
      <motion.div
        initial={{ opacity: 0, y: 24, scale: 0.97 }}
        animate={{ opacity: 1, y: 0,  scale: 1 }}
        transition={{ duration: 0.5, ease: [.4,0,.2,1] }}
        className="glass rounded-3xl shadow-2xl w-full max-w-[420px] mx-4 overflow-hidden"
        style={{ boxShadow: '0 32px 80px rgba(0,0,0,.25)' }}
      >
        {/* Top branding strip */}
        <div className="px-8 pt-8 pb-6 text-center">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl mb-4"
               style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)' }}>
            <span className="text-white font-black text-2xl">N</span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">NutriCook</h1>
          <p className="text-slate-500 text-sm mt-1">Your AI-powered diet assistant</p>
        </div>

        {/* Features strip */}
        <div className="mx-8 mb-6 grid grid-cols-2 gap-2">
          {FEATURES.map(f => (
            <div key={f.text}
                 className="flex items-center gap-2 bg-white/60 rounded-xl px-3 py-2 border border-white/80">
              <span className="text-base">{f.icon}</span>
              <span className="text-xs font-medium text-slate-700 leading-tight">{f.text}</span>
            </div>
          ))}
        </div>

        {/* Form */}
        <div className="px-8 pb-8">
          {/* Tab toggle */}
          <div className="flex bg-slate-100/80 rounded-xl p-1 mb-5">
            {[{ label: 'Sign in', val: true }, { label: 'Sign up', val: false }].map(({ label, val }) => (
              <button
                key={label}
                type="button"
                onClick={() => switchMode(val)}
                className={`flex-1 py-2 rounded-lg text-sm font-bold transition-all ${
                  isLogin === val
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-400 hover:text-slate-600'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="label">Email address</label>
              <input
                className="input"
                type="email"
                placeholder="you@example.com"
                autoComplete="email"
                {...register('email', { required: 'Email is required' })}
              />
              {errors.email && <p className="text-red-500 text-xs mt-1.5">{errors.email.message}</p>}
            </div>

            <div>
              <label className="label">Password</label>
              <input
                className="input"
                type="password"
                placeholder="••••••••"
                autoComplete={isLogin ? 'current-password' : 'new-password'}
                {...register('password', {
                  required: 'Password is required',
                  minLength: { value: 8, message: 'At least 8 characters' },
                })}
              />
              {errors.password && <p className="text-red-500 text-xs mt-1.5">{errors.password.message}</p>}
            </div>

            <AnimatePresence>
              {serverError && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="flex items-start gap-2.5 bg-red-50 border border-red-200 rounded-xl px-4 py-3"
                >
                  <svg className="w-4 h-4 text-red-500 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                  <p className="text-red-600 text-sm">{serverError}</p>
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
        <div className="border-t border-slate-200/60 px-8 py-4 flex items-center justify-center gap-2">
          <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
          <span className="text-xs text-slate-400">Powered by Groq AI · Llama 3.3 70B</span>
        </div>
      </motion.div>
    </div>
  )
}
