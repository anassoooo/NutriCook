import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { Achievement } from '../types'

const TYPE_ICONS: Record<string, string> = {
  LOGIN_STREAK:    '🔥',
  PLANS_GENERATED: '🥗',
  CALORIE_GOAL:    '🎯',
  HYDRATION:       '💧',
}

interface AllAchievement {
  id: number; name: string; description: string; type: string; threshold: number
}

export default function AchievementsPage() {
  const { userId } = useAuth()
  const [all,    setAll]    = useState<AllAchievement[]>([])
  const [earned, setEarned] = useState<Achievement[]>([])

  useEffect(() => {
    api.get<AllAchievement[]>('/achievements').then(r => setAll(r.data)).catch(() => {})
    if (userId) {
      api.get<Achievement[]>(`/users/${userId}/achievements`).then(r => setEarned(r.data)).catch(() => {})
    }
  }, [userId])

  const earnedIds = new Set(earned.map(a => a.id))
  const earnedCount = earnedIds.size

  return (
    <div className="space-y-6 animate-fade-in">

      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <h1 className="page-title">Achievements</h1>
          <p className="page-subtitle">Track your milestones and stay motivated.</p>
        </div>
        <div className="card py-3 px-5 flex items-center gap-3">
          <span className="text-2xl">🏆</span>
          <div>
            <p className="text-xl font-bold text-slate-900">{earnedCount} / {all.length}</p>
            <p className="text-xs text-slate-400">Unlocked</p>
          </div>
        </div>
      </div>

      {/* Progress bar */}
      {all.length > 0 && (
        <div className="card py-4">
          <div className="flex justify-between text-sm mb-2">
            <span className="font-medium text-slate-700">Overall progress</span>
            <span className="text-slate-400">{Math.round((earnedCount / all.length) * 100)}%</span>
          </div>
          <div className="progress-bar">
            <motion.div
              className="progress-fill bg-brand-500"
              initial={{ width: 0 }}
              animate={{ width: `${(earnedCount / all.length) * 100}%` }}
              transition={{ duration: 1, ease: 'easeOut' }}
            />
          </div>
        </div>
      )}

      {/* Achievement grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {all.map((a, i) => {
          const isEarned    = earnedIds.has(a.id)
          const earnedEntry = earned.find(e => e.id === a.id)
          const icon        = TYPE_ICONS[a.type] ?? '⭐'

          return (
            <motion.div
              key={a.id}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.04 }}
              className={`card flex items-start gap-4 transition-all ${
                isEarned
                  ? 'border-brand-200 shadow-card-md'
                  : 'opacity-60 grayscale'
              }`}
            >
              {/* Icon */}
              <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-2xl shrink-0 ${
                isEarned ? 'bg-brand-50' : 'bg-slate-100'
              }`}>
                {isEarned ? icon : '🔒'}
              </div>

              {/* Content */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <p className="font-semibold text-slate-900">{a.name}</p>
                  {isEarned && <span className="badge-green">Earned</span>}
                </div>
                <p className="text-sm text-slate-500 mt-0.5">{a.description}</p>
                {earnedEntry?.earnedAt && (
                  <p className="text-xs text-brand-500 mt-1.5 font-medium">
                    ✓ {new Date(earnedEntry.earnedAt).toLocaleDateString('en-GB', {
                      day: 'numeric', month: 'short', year: 'numeric'
                    })}
                  </p>
                )}
                {!isEarned && (
                  <p className="text-xs text-slate-400 mt-1.5">
                    Requires {a.threshold} {a.type === 'PLANS_GENERATED' ? 'plans' : a.type === 'HYDRATION' ? 'hydration days' : a.type === 'CALORIE_GOAL' ? 'goal days' : 'streak days'}
                  </p>
                )}
              </div>
            </motion.div>
          )
        })}
      </div>

      {all.length === 0 && (
        <div className="card text-center py-16">
          <div className="text-5xl mb-3">🏆</div>
          <p className="font-semibold text-slate-700">No achievements yet</p>
          <p className="text-slate-400 text-sm mt-1">Start logging progress and generating meal plans to earn them.</p>
        </div>
      )}
    </div>
  )
}
