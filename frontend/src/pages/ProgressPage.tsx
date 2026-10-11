import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import {
  ResponsiveContainer, ComposedChart, Line, XAxis, YAxis,
  CartesianGrid, Tooltip as ChartTooltip,
} from 'recharts'
import api from '../lib/api'
import { toApiError, type FieldError } from '../lib/errors'
import { BACKFILL_DAYS, MAX_NOTES_LENGTH, PROGRESS_BOUNDS, daysAgo, localIsoDate } from '../lib/progressRules'
import { useAuth } from '../contexts/AuthContext'
import type { ProgressEntry } from '../types'
import { DropIcon, FlameIcon, ScaleIcon, StrideIcon } from '../components/Icons'

interface FormData {
  date: string
  weightKg: string
  caloriesConsumed: string
  waterMl: string
  exerciseMinutes: string
  notes: string
}

type Metric = 'weightKg' | 'caloriesConsumed' | 'waterMl' | 'exerciseMinutes'

const METRICS: { key: Metric; label: string; unit: string; Icon: typeof ScaleIcon; digits: number }[] = [
  { key: 'weightKg', label: 'Weight', unit: 'kg', Icon: ScaleIcon, digits: 1 },
  { key: 'caloriesConsumed', label: 'Calories', unit: 'kcal', Icon: FlameIcon, digits: 0 },
  { key: 'waterMl', label: 'Water', unit: 'ml', Icon: DropIcon, digits: 0 },
  { key: 'exerciseMinutes', label: 'Exercise', unit: 'min', Icon: StrideIcon, digits: 0 },
]

/** API field names in words people use. */
const FIELD_LABEL: Record<string, string> = {
  date: 'Date',
  weightKg: 'Weight (kg)',
  caloriesConsumed: 'Calories (kcal)',
  waterMl: 'Water (ml)',
  exerciseMinutes: 'Exercise (min)',
  notes: 'Notes',
  resetCalories: 'Calories',
}

const INK = '#2B1E33'
const FAINT = '#8E8795'
const LINE = '#D9DED3'

const shortDate = (iso: string) =>
  new Date(iso + 'T00:00:00').toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })
const longDate = (iso: string) =>
  new Date(iso + 'T00:00:00').toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })

function format(value: number, digits: number) {
  return value.toLocaleString('en-GB', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

/** The latest value and its change since the previous logged value. Neutral: no good/bad colours. */
function Headline({ entries, metric }: { entries: ProgressEntry[]; metric: (typeof METRICS)[number] }) {
  const logged = entries.filter(e => e[metric.key] != null)
  const last = logged[logged.length - 1]
  const prev = logged.length > 1 ? logged[logged.length - 2] : undefined
  if (!last) return <p className="text-ink-soft">No {metric.label.toLowerCase()} logged in the last {BACKFILL_DAYS} days.</p>
  const value = last[metric.key] as number
  const diff = prev ? value - (prev[metric.key] as number) : null
  return (
    <div>
      <p className="font-heading text-5xl font-bold leading-none num">
        {format(value, metric.digits)}
        <span className="ml-1.5 text-xl font-medium text-ink-soft">{metric.unit}</span>
      </p>
      <p className="mt-2 text-sm text-ink-soft num">
        {longDate(last.date)}
        {diff !== null && diff !== 0 && (
          <>, {diff > 0 ? '+' : '−'}{format(Math.abs(diff), metric.digits)} {metric.unit} since {shortDate(prev!.date)}</>
        )}
        {diff === 0 && <>, the same as {shortDate(prev!.date)}</>}
      </p>
    </div>
  )
}

function TrendChart({ entries, metric }: { entries: ProgressEntry[]; metric: (typeof METRICS)[number] }) {
  const isCalories = metric.key === 'caloriesConsumed'
  const data = entries
    .filter(e => e[metric.key] != null)
    .map(e => ({
      date: shortDate(e.date),
      value: e[metric.key] as number,
      // each day is judged against its own target (BR-PRG-06)
      target: isCalories ? e.targetCalories : null,
    }))

  if (data.length < 2) {
    return (
      <p className="grid h-56 place-items-center rounded-ctl border border-dashed border-line text-sm text-ink-soft">
        Log two or more days to see a trend.
      </p>
    )
  }

  return (
    <div className="h-64" aria-label={`${metric.label} over the last ${BACKFILL_DAYS} days`} role="img">
      <ResponsiveContainer width="100%" height="100%">
        <ComposedChart data={data} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
          <CartesianGrid stroke={LINE} vertical={false} />
          <XAxis dataKey="date" tick={{ fontSize: 12, fill: FAINT }} tickLine={false} axisLine={{ stroke: LINE }} interval="preserveStartEnd" />
          <YAxis
            domain={['auto', 'auto']}
            tick={{ fontSize: 12, fill: FAINT }}
            tickLine={false}
            axisLine={false}
            width={52}
            tickFormatter={v => format(v, metric.digits)}
          />
          <ChartTooltip
            cursor={{ stroke: LINE }}
            contentStyle={{ borderRadius: 10, border: `1px solid ${LINE}`, boxShadow: 'none', fontSize: 13 }}
            formatter={(v, name) => [`${format(Number(v), metric.digits)} ${metric.unit}`, name === 'target' ? 'Target' : metric.label]}
          />
          {isCalories && (
            <Line type="stepAfter" dataKey="target" stroke={FAINT} strokeDasharray="5 5" strokeWidth={1.5} dot={false} name="target" />
          )}
          <Line
            type="monotone"
            dataKey="value"
            stroke={INK}
            strokeWidth={2.25}
            dot={{ r: 3.5, fill: INK, strokeWidth: 0 }}
            activeDot={{ r: 5.5, fill: INK, stroke: '#FFFFFF', strokeWidth: 2 }}
            name="value"
          />
        </ComposedChart>
      </ResponsiveContainer>
      {isCalories && <p className="mt-1 text-xs text-ink-faint">Dashed line: the day's calorie target.</p>}
    </div>
  )
}

const HISTORY_COLUMNS = 'sm:grid-cols-[104px_72px_minmax(0,1fr)_76px_64px]'

function Cell({ value, unit, label }: { value: number | null; unit: string; label: string }) {
  return (
    <span className="num" aria-label={value == null ? `${label}: not logged` : undefined}>
      {value == null ? <span className="text-ink-faint">–</span> : <><span className="text-ink">{value}</span> {unit}</>}
    </span>
  )
}

function EntryRow({ entry, onResetCalories }: { entry: ProgressEntry; onResetCalories: (date: string) => void }) {
  return (
    <li className={`grid grid-cols-2 gap-x-4 gap-y-1 py-3 text-sm text-ink-soft ${HISTORY_COLUMNS} sm:items-baseline`}>
      <p className="col-span-2 font-semibold text-ink sm:col-span-1">{longDate(entry.date)}</p>
      <Cell value={entry.weightKg} unit="kg" label="Weight" />
      <span className="col-span-2 inline-flex flex-wrap items-baseline gap-x-2 sm:col-span-1">
        {entry.caloriesConsumed == null ? (
          <span className="text-ink-faint">–</span>
        ) : (
          <>
            <span
              className="text-ink num"
              title={entry.caloriesOverridden ? 'Typed by you' : `From ${entry.mealsCompleted} meal${entry.mealsCompleted === 1 ? '' : 's'} checked off`}
            >
              {entry.caloriesConsumed} kcal · {entry.caloriesOverridden ? 'manual' : 'auto'}
            </span>
            {entry.caloriesOverridden && (
              <button
                type="button"
                onClick={() => onResetCalories(entry.date)}
                className="text-xs font-semibold text-ink underline underline-offset-2"
              >
                Use checked meals
              </button>
            )}
          </>
        )}
      </span>
      <Cell value={entry.waterMl} unit="ml" label="Water" />
      <Cell value={entry.exerciseMinutes} unit="min" label="Exercise" />
      {entry.notes && <p className="col-span-full italic">“{entry.notes}”</p>}
    </li>
  )
}

export default function ProgressPage() {
  const { userId } = useAuth()
  const [entries, setEntries] = useState<ProgressEntry[]>([])
  const [saved, setSaved] = useState(false)
  const [loading, setLoading] = useState(false)
  const [activeMetric, setActiveMetric] = useState<Metric>('weightKg')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldError[]>([])

  const { register, handleSubmit, reset } = useForm<FormData>({
    defaultValues: { date: localIsoDate() },
  })

  const fetchEntries = () => {
    if (!userId) return
    const end = localIsoDate()
    const start = daysAgo(BACKFILL_DAYS)
    api.get<ProgressEntry[]>(`/users/${userId}/progress/weekly?start=${start}&end=${end}`)
      .then(r => setEntries(r.data)).catch(() => {})
  }

  useEffect(fetchEntries, [userId])

  const fail = (err: unknown) => {
    const e = toApiError(err)
    setError(e.detail ?? e.message)
    setFieldErrors(e.fieldErrors)
  }

  /** Back to calories counted from checked meals (BR-PRG-05). */
  const resetCalories = async (date: string) => {
    if (!userId) return
    setError(''); setFieldErrors([])
    try {
      await api.post(`/users/${userId}/progress`, { date, resetCalories: true })
      fetchEntries()
    } catch (err) { fail(err) }
  }

  const onSubmit = async (data: FormData) => {
    if (!userId) return
    setLoading(true)
    setError(''); setFieldErrors([])
    try {
      await api.post(`/users/${userId}/progress`, {
        date: data.date,
        weightKg: data.weightKg ? parseFloat(data.weightKg) : null,
        caloriesConsumed: data.caloriesConsumed ? parseInt(data.caloriesConsumed) : null,
        waterMl: data.waterMl ? parseInt(data.waterMl) : null,
        exerciseMinutes: data.exerciseMinutes ? parseInt(data.exerciseMinutes) : null,
        notes: data.notes || null,
      })
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
      reset({ date: localIsoDate() })
      fetchEntries()
    } catch (err) { fail(err) }
    finally { setLoading(false) }
  }

  const metric = METRICS.find(m => m.key === activeMetric)!

  return (
    <div>
      <header>
        <h1 className="page-title">Progress</h1>
        <p className="page-subtitle">The last {BACKFILL_DAYS} days. Calories fill in by themselves as you check meals off on Today.</p>
      </header>

      {/* Trend */}
      <section className="mt-8 rounded-panel border border-line bg-paper p-6" aria-labelledby="trend-heading">
        <h2 id="trend-heading" className="sr-only">Trend</h2>
        <div className="flex flex-wrap gap-1.5" role="group" aria-label="Measure">
          {METRICS.map(({ key, label, Icon }) => (
            <button
              key={key}
              type="button"
              aria-pressed={activeMetric === key}
              onClick={() => setActiveMetric(key)}
              className={`inline-flex items-center gap-2 rounded-full px-3.5 py-1.5 text-sm font-semibold transition-colors ${
                activeMetric === key ? 'bg-ink text-chalk' : 'text-ink-soft hover:bg-wash hover:text-ink'
              }`}
            >
              <Icon size={16} />
              {label}
            </button>
          ))}
        </div>
        <div className="mt-6 grid gap-6 md:grid-cols-[220px_1fr] md:items-start">
          <Headline entries={entries} metric={metric} />
          <TrendChart entries={entries} metric={metric} />
        </div>
      </section>

      <div className="mt-10 grid gap-10 lg:grid-cols-[minmax(0,400px)_1fr]">
        {/* Log a day */}
        <section aria-labelledby="log-heading">
          <h2 id="log-heading" className="section-title">Log a day</h2>
          <p className="mt-1 text-sm text-ink-soft">Every field is optional. Leave calories empty to count the meals you checked off.</p>

          <form onSubmit={handleSubmit(onSubmit)} className="mt-5 space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <label className="col-span-2">
                <span className="label">Date</span>
                <input className="input" type="date" min={daysAgo(BACKFILL_DAYS)} max={localIsoDate()} {...register('date', { required: true })} />
              </label>
              <label>
                <span className="label">Weight (kg)</span>
                <input className="input num" type="number" step="0.1" placeholder="70.5" {...PROGRESS_BOUNDS.weightKg} {...register('weightKg')} />
              </label>
              <label>
                <span className="label">Calories (kcal)</span>
                <input className="input num" type="number" placeholder="From checked meals" {...PROGRESS_BOUNDS.caloriesConsumed} {...register('caloriesConsumed')} />
              </label>
              <label>
                <span className="label">Water (ml)</span>
                <input className="input num" type="number" placeholder="2000" {...PROGRESS_BOUNDS.waterMl} {...register('waterMl')} />
              </label>
              <label>
                <span className="label">Exercise (min)</span>
                <input className="input num" type="number" placeholder="30" {...PROGRESS_BOUNDS.exerciseMinutes} {...register('exerciseMinutes')} />
              </label>
              <label className="col-span-2">
                <span className="label">Notes</span>
                <input className="input" type="text" placeholder="Feeling great!" maxLength={MAX_NOTES_LENGTH} {...register('notes')} />
              </label>
            </div>

            {error && (
              <div role="alert" className="banner-error flex-col gap-1">
                <p>{error}</p>
                {fieldErrors.length > 0 && (
                  <ul className="list-disc pl-5 text-sm">
                    {fieldErrors.map(f => <li key={f.field}>{FIELD_LABEL[f.field] ?? f.field}: {f.message}</li>)}
                  </ul>
                )}
              </div>
            )}

            <div className="flex items-center gap-4">
              <button type="submit" disabled={loading} className="btn-primary">
                {loading ? 'Saving…' : 'Save entry'}
              </button>
              {saved && <p role="status" className="text-sm font-semibold text-cucumber">Saved.</p>}
            </div>
          </form>
        </section>

        {/* History */}
        <section aria-labelledby="history-heading">
          <div className="flex items-baseline justify-between">
            <h2 id="history-heading" className="section-title">History</h2>
            <p className="text-sm text-ink-soft num">{entries.length} {entries.length === 1 ? 'day' : 'days'} logged</p>
          </div>
          {entries.length === 0 ? (
            <p className="mt-4 rounded-ctl border border-dashed border-line px-4 py-8 text-center text-ink-soft">
              Nothing logged yet. Your first entry appears here.
            </p>
          ) : (
            <>
            <div className={`mt-4 hidden gap-x-4 pb-2 text-xs font-semibold text-ink-faint sm:grid ${HISTORY_COLUMNS}`} aria-hidden>
              <span>Day</span><span>Weight</span><span>Calories</span><span>Water</span><span>Exercise</span>
            </div>
            <ul className="divide-y divide-line border-y border-line max-sm:mt-3">
              {[...entries].reverse().map(e => <EntryRow key={e.id} entry={e} onResetCalories={resetCalories} />)}
            </ul>
            </>
          )}
        </section>
      </div>
    </div>
  )
}
