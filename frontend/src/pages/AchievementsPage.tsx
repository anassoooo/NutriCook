import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { Achievement, AchievementType } from '../types'

const TYPE_META: Record<AchievementType, { icon: string; color: string; unit: string }> = {
  LOGGING_STREAK: { icon: '🔥', color: '#f97316', unit: 'days in a row' },
  PLANS_GENERATED: { icon: '🥗', color: '#16a34a', unit: 'days planned' },
  CALORIE_GOAL: { icon: '🎯', color: '#3b82f6', unit: 'days on target' },
  HYDRATION: { icon: '💧', color: '#06b6d4', unit: 'hydrated days' },
  WEIGHT_MILESTONE: { icon: '⚖️', color: '#a855f7', unit: 'kg' },
}

const metaOf = (type: AchievementType) => TYPE_META[type] ?? { icon: '⭐', color: '#f59e0b', unit: '' }

function EarnedCard({ a, i }: { a: Achievement; i: number }) {
  const meta = metaOf(a.type)
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: i * 0.04 }}
      className="achievement-earned"
    >
      <div className="flex items-start justify-between mb-4">
        <div className="w-14 h-14 rounded-2xl flex items-center justify-center text-3xl"
             style={{ background: `${meta.color}20` }}>
          {meta.icon}
        </div>
        <span className="badge-gold flex items-center gap-1">
          <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
          </svg>
          Earned
        </span>
      </div>

      <p className="font-bold text-slate-900 text-base leading-tight">{a.name}</p>
      <p className="text-sm text-slate-600 mt-1 leading-relaxed">{a.description}</p>

      {a.earnedAt && (
        <p className="text-xs font-semibold mt-3 flex items-center gap-1" style={{ color: meta.color }}>
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7"/>
          </svg>
          {new Date(a.earnedAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })}
        </p>
      )}

      <div className="absolute inset-x-0 top-0 h-0.5 rounded-t-2xl"
           style={{ background: `linear-gradient(90deg, transparent, ${meta.color}60, transparent)` }} />
    </motion.div>
  )
}

/** Progress comes from the server (`currentValue / threshold`); nothing is computed here. */
function LockedCard({ a, i }: { a: Achievement; i: number }) {
  const meta = metaOf(a.type)
  const current = a.currentValue ?? 0
  const pct = a.threshold > 0 ? Math.min(current / a.threshold, 1) * 100 : 0

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: i * 0.04 }}
      className="achievement-locked"
    >
      <div className="flex items-start justify-between mb-4">
        <div className="w-14 h-14 rounded-2xl flex items-center justify-center text-3xl grayscale"
             style={{ background: 'rgba(255,255,255,0.07)' }}>
          {meta.icon}
        </div>
        <div className="w-7 h-7 rounded-lg flex items-center justify-center text-base"
             style={{ background: 'rgba(255,255,255,0.07)' }}>🔒</div>
      </div>

      <p className="font-bold text-slate-700 text-base leading-tight">{a.name}</p>
      <p className="text-sm text-slate-400 mt-1 leading-relaxed">{a.description}</p>

      <div className="mt-4 pt-3 border-t border-slate-100">
        <div className="flex justify-between text-xs font-medium text-slate-400 mb-1.5">
          <span>Progress</span>
          <span className="tabular-nums">{current} / {a.threshold} {meta.unit}</span>
        </div>
        <div
          className="h-1.5 rounded-full overflow-hidden"
          style={{ background: 'rgba(255,255,255,0.08)' }}
          role="progressbar"
          aria-label={`${a.name} progress`}
          aria-valuemin={0}
          aria-valuemax={a.threshold}
          aria-valuenow={current}
        >
          <div className="h-full rounded-full" style={{ width: `${pct}%`, background: meta.color }} />
        </div>
      </div>
    </motion.div>
  )
}

export default function AchievementsPage() {
  const { userId } = useAuth()
  const [all,   setAll]   = useState<Achievement[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    if (!userId) return
    api.get<Achievement[]>(`/users/${userId}/achievements`)
      .then(r => setAll(r.data))
      .catch(err => setError(toApiError(err).message))
  }, [userId])

  const earned      = all.filter(a => a.earned)
  const locked      = all.filter(a => !a.earned)
  const earnedCount = earned.length
  const pct         = all.length > 0 ? Math.round((earnedCount / all.length) * 100) : 0

  return (
    <div className="space-y-6 animate-fade-in">

      {/* ── Hero header ─────────────────────────── */}
      <div className="relative overflow-hidden rounded-3xl text-white p-7"
           style={{ background: 'linear-gradient(135deg,#78350f 0%,#92400e 40%,#b45309 100%)' }}>
        <div className="absolute inset-0 opacity-10"
             style={{ backgroundImage: 'radial-gradient(circle, white 1px, transparent 1px)', backgroundSize: '28px 28px' }} />

        <div className="relative z-10 flex items-center justify-between gap-4 flex-wrap">
          <div>
            <p className="text-amber-300 text-sm font-medium mb-1">Your achievements</p>
            <h1 className="text-3xl font-black tracking-tight">
              {earnedCount} <span className="text-amber-300 font-light text-2xl">/ {all.length} unlocked</span>
            </h1>
            <p className="text-amber-200 text-sm mt-1">
              {pct === 100 ? 'You\'ve unlocked everything — legend!' :
               pct >= 50   ? 'Great progress — keep it up!' :
               pct > 0     ? 'You\'re on your way. Keep going!' :
                             'Start logging to earn your first achievement.'}
            </p>
          </div>
          <div className="text-6xl filter drop-shadow-lg select-none">🏆</div>
        </div>

        {all.length > 0 && (
          <div className="relative z-10 mt-5">
            <div className="flex justify-between text-xs text-amber-300 mb-1.5">
              <span>Overall progress</span>
              <span className="text-white font-semibold">{pct}%</span>
            </div>
            <div className="h-2 bg-white/20 rounded-full overflow-hidden">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${pct}%` }}
                transition={{ duration: 1.2, ease: [.4,0,.2,1] }}
                className="h-full rounded-full bg-white"
              />
            </div>
          </div>
        )}
      </div>

      {error && <p role="alert" className="text-sm text-red-300">{error}</p>}

      {/* ── Earned section ─────────────────────── */}
      {earnedCount > 0 && (
        <div>
          <p className="section-label mb-3">Earned — {earnedCount}</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {earned.map((a, i) => <EarnedCard key={a.id} a={a} i={i} />)}
          </div>
        </div>
      )}

      {/* ── Locked section ─────────────────────── */}
      {locked.length > 0 && (
        <div>
          <p className="section-label mb-3">Locked — {locked.length}</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {locked.map((a, i) => <LockedCard key={a.id} a={a} i={i} />)}
          </div>
        </div>
      )}

      {all.length === 0 && !error && (
        <div className="card text-center py-16">
          <div className="text-5xl mb-3">🏆</div>
          <p className="font-bold text-slate-700">No achievements configured yet</p>
          <p className="text-slate-400 text-sm mt-1">Start logging progress and generating meal plans to earn them.</p>
        </div>
      )}
    </div>
  )
}
