import { useEffect, useState } from 'react'
import api from '../lib/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../contexts/AuthContext'
import type { Achievement, AchievementType } from '../types'
import { CheckIcon, DropIcon, FlameIcon, PlateIcon, ScaleIcon, TrendIcon } from '../components/Icons'

const TYPE_META: Record<AchievementType, { Icon: typeof PlateIcon; unit: string }> = {
  LOGGING_STREAK: { Icon: FlameIcon, unit: 'days in a row' },
  PLANS_GENERATED: { Icon: PlateIcon, unit: 'days planned' },
  CALORIE_GOAL: { Icon: TrendIcon, unit: 'days on target' },
  HYDRATION: { Icon: DropIcon, unit: 'hydrated days' },
  WEIGHT_MILESTONE: { Icon: ScaleIcon, unit: 'kg' },
}

const metaOf = (type: AchievementType) => TYPE_META[type] ?? { Icon: PlateIcon, unit: '' }

const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })

/** One achievement. Progress comes from the server (`currentValue / threshold`); nothing is computed here. */
function AchievementRow({ a }: { a: Achievement }) {
  const { Icon, unit } = metaOf(a.type)
  const current = a.currentValue ?? 0
  const pct = a.threshold > 0 ? Math.min(current / a.threshold, 1) * 100 : 0
  return (
    <li className="grid grid-cols-[52px_1fr] gap-x-4 py-5">
      <span
        className={`grid h-[52px] w-[52px] place-items-center rounded-full ${
          a.earned ? 'bg-ink text-chalk' : 'border-2 border-dashed border-line text-ink-faint'
        }`}
        aria-hidden
      >
        <Icon size={22} />
      </span>
      <div className="min-w-0">
        <div className="flex flex-wrap items-baseline justify-between gap-x-4">
          <h3 className="font-heading text-lg font-semibold">{a.name}</h3>
          {a.earned && (
            <p className="inline-flex items-center gap-1.5 text-sm font-semibold text-ink">
              <CheckIcon size={15} strokeWidth={2.5} />
              <span>Earned</span>
              {a.earnedAt && <span className="font-normal text-ink-soft">{formatDate(a.earnedAt)}</span>}
            </p>
          )}
        </div>
        <p className="mt-0.5 max-w-[60ch] text-sm text-ink-soft">{a.description}</p>
        {!a.earned && (
          <div className="mt-3 max-w-md">
            <div
              className="h-1.5 overflow-hidden rounded-full bg-wash"
              role="progressbar"
              aria-label={`${a.name} progress`}
              aria-valuemin={0}
              aria-valuemax={a.threshold}
              aria-valuenow={current}
            >
              <div className="h-full rounded-full bg-ink" style={{ width: `${pct}%` }} />
            </div>
            <p className="mt-1.5 text-xs text-ink-soft num">{current} / {a.threshold} {unit}</p>
          </div>
        )}
      </div>
    </li>
  )
}

export default function AchievementsPage() {
  const { userId } = useAuth()
  const [all, setAll] = useState<Achievement[]>([])
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!userId) return
    api.get<Achievement[]>(`/users/${userId}/achievements`)
      .then(r => setAll(r.data))
      .catch(err => setError(toApiError(err).message))
      .finally(() => setLoaded(true))
  }, [userId])

  const earned = all.filter(a => a.earned)
  const locked = all.filter(a => !a.earned)

  return (
    <div>
      <header className="flex flex-wrap items-end justify-between gap-6">
        <div>
          <h1 className="page-title">Achievements</h1>
          <p className="page-subtitle">Each one states its exact rule. Once earned, it stays earned.</p>
        </div>
        {all.length > 0 && (
          <p className="font-heading text-2xl font-semibold num" aria-label={`${earned.length} of ${all.length} earned`}>
            {earned.length}
            <span className="text-base font-normal text-ink-soft"> of {all.length} earned</span>
          </p>
        )}
      </header>

      {error && <p role="alert" className="banner-error mt-6">{error}</p>}

      {locked.length > 0 && (
        <section className="mt-8" aria-labelledby="progress-heading">
          <h2 id="progress-heading" className="section-title">In progress ({locked.length})</h2>
          <ul className="mt-2 divide-y divide-line border-y border-line">
            {locked.map(a => <AchievementRow key={a.id} a={a} />)}
          </ul>
        </section>
      )}

      {earned.length > 0 && (
        <section className="mt-10" aria-labelledby="earned-heading">
          <h2 id="earned-heading" className="section-title">Earned ({earned.length})</h2>
          <ul className="mt-2 divide-y divide-line border-y border-line">
            {earned.map(a => <AchievementRow key={a.id} a={a} />)}
          </ul>
        </section>
      )}

      {loaded && all.length === 0 && !error && (
        <p className="mt-8 rounded-ctl border border-dashed border-line px-4 py-10 text-center text-ink-soft">
          No achievements are set up yet.
        </p>
      )}
    </div>
  )
}
