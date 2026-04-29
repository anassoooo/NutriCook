import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { motion } from 'framer-motion'
import {
  ResponsiveContainer, LineChart, Line, XAxis, YAxis,
  CartesianGrid, Tooltip as ChartTooltip, ReferenceLine,
} from 'recharts'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { ProgressEntry } from '../types'

interface FormData {
  date: string
  weightKg: string
  caloriesConsumed: string
  waterMl: string
  exerciseMinutes: string
  notes: string
}

const STATS = [
  { key: 'weightKg',         label: 'Weight',   unit: 'kg',   icon: '⚖️', color: '#3b82f6', bg: '#eff6ff', border: '#bfdbfe' },
  { key: 'caloriesConsumed', label: 'Calories', unit: 'kcal', icon: '🔥', color: '#f97316', bg: '#fff7ed', border: '#fed7aa' },
  { key: 'waterMl',          label: 'Water',    unit: 'ml',   icon: '💧', color: '#06b6d4', bg: '#ecfeff', border: '#a5f3fc' },
  { key: 'exerciseMinutes',  label: 'Exercise', unit: 'min',  icon: '🏃', color: '#16a34a', bg: '#f0fdf4', border: '#bbf7d0' },
]

const CHART_METRICS = [
  { key: 'weightKg',         label: 'Weight',   unit: 'kg',   color: '#3b82f6' },
  { key: 'caloriesConsumed', label: 'Calories', unit: 'kcal', color: '#f97316' },
  { key: 'waterMl',          label: 'Water',    unit: 'ml',   color: '#06b6d4' },
  { key: 'exerciseMinutes',  label: 'Exercise', unit: 'min',  color: '#16a34a' },
]

function TrendBadge({ diff, unit }: { diff: number; unit: string }) {
  const up    = diff > 0
  const color = up ? '#16a34a' : '#ef4444'
  return (
    <span className="text-xs font-semibold flex items-center gap-0.5 mt-1" style={{ color }}>
      {up ? '↑' : '↓'} {Math.abs(diff)}{unit} from last
    </span>
  )
}

function EntryRow({ entry }: { entry: ProgressEntry }) {
  return (
    <div className="flex items-start justify-between py-3.5 -mx-2 px-2 rounded-xl transition-colors hover:bg-white/5"
         style={{ borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
      <div>
        <p className="text-sm font-semibold" style={{ color: 'rgba(255,255,255,0.85)' }}>
          {new Date(entry.date).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })}
        </p>
        {entry.notes && <p className="text-xs italic mt-0.5" style={{ color: 'rgba(255,255,255,0.58)' }}>"{entry.notes}"</p>}
      </div>
      <div className="flex flex-wrap justify-end gap-1.5">
        {entry.weightKg         != null && <span className="badge" style={{ background: 'rgba(59,130,246,0.15)',  color: '#93c5fd' }}>{entry.weightKg} kg</span>}
        {entry.caloriesConsumed != null && <span className="badge" style={{ background: 'rgba(249,115,22,0.15)',  color: '#fdba74' }}>{entry.caloriesConsumed} kcal</span>}
        {entry.waterMl          != null && <span className="badge" style={{ background: 'rgba(6,182,212,0.15)',   color: '#67e8f9' }}>{entry.waterMl} ml</span>}
        {entry.exerciseMinutes  != null && <span className="badge" style={{ background: 'rgba(16,185,129,0.15)',  color: '#6ee7b7' }}>{entry.exerciseMinutes} min</span>}
      </div>
    </div>
  )
}

interface ChartPoint {
  date: string
  value: number | null
}

function TrendChart({ entries, metricKey, color, unit }: {
  entries: ProgressEntry[]
  metricKey: string
  color: string
  unit: string
}) {
  const data: ChartPoint[] = [...entries]
    .sort((a, b) => a.date.localeCompare(b.date))
    .map(e => ({
      date:  new Date(e.date).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' }),
      value: (e as any)[metricKey] ?? null,
    }))
    .filter(d => d.value !== null)

  if (data.length < 2) {
    return (
      <div className="flex flex-col items-center justify-center h-40 text-sm gap-2" style={{ color: 'rgba(255,255,255,0.58)' }}>
        <span className="text-3xl">📈</span>
        Log at least 2 entries to see your trend.
      </div>
    )
  }

  const values   = data.map(d => d.value as number)
  const avg      = values.reduce((s, v) => s + v, 0) / values.length
  const minVal   = Math.min(...values)
  const maxVal   = Math.max(...values)
  const padding  = (maxVal - minVal) * 0.15 || 1
  const domain   = [Math.floor(minVal - padding), Math.ceil(maxVal + padding)]

  return (
    <ResponsiveContainer width="100%" height={200}>
      <LineChart data={data} margin={{ top: 8, right: 16, left: -8, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" vertical={false} />
        <XAxis
          dataKey="date"
          tick={{ fontSize: 11, fill: 'rgba(255,255,255,0.58)' }}
          tickLine={false}
          axisLine={false}
          interval="preserveStartEnd"
        />
        <YAxis
          domain={domain}
          tick={{ fontSize: 11, fill: 'rgba(255,255,255,0.58)' }}
          tickLine={false}
          axisLine={false}
          tickFormatter={v => `${v}${unit}`}
        />
        <ChartTooltip
          contentStyle={{ fontSize: 12, borderRadius: 10, background: 'rgba(0,0,0,0.75)', border: '1px solid rgba(255,255,255,0.12)', boxShadow: '0 8px 24px rgba(0,0,0,.4)', backdropFilter: 'blur(12px)' }}
          formatter={(v) => [`${v} ${unit}`, '']}
          labelStyle={{ fontWeight: 600, color: 'rgba(255,255,255,0.85)' }}
          itemStyle={{ color: 'rgba(255,255,255,0.65)' }}
        />
        <ReferenceLine y={avg} stroke={color} strokeDasharray="4 4" strokeOpacity={0.4} />
        <Line
          type="monotone"
          dataKey="value"
          stroke={color}
          strokeWidth={2.5}
          dot={{ r: 4, fill: color, strokeWidth: 0 }}
          activeDot={{ r: 6, fill: color, stroke: 'white', strokeWidth: 2 }}
          connectNulls={false}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}

export default function ProgressPage() {
  const { userId } = useAuth()
  const [entries,       setEntries]       = useState<ProgressEntry[]>([])
  const [saved,         setSaved]         = useState(false)
  const [loading,       setLoading]       = useState(false)
  const [activeMetric,  setActiveMetric]  = useState('weightKg')

  const { register, handleSubmit, reset } = useForm<FormData>({
    defaultValues: { date: new Date().toISOString().slice(0, 10) },
  })

  const fetchEntries = () => {
    if (!userId) return
    const end   = new Date().toISOString().slice(0, 10)
    const start = new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10)
    api.get<ProgressEntry[]>(`/users/${userId}/progress/weekly?start=${start}&end=${end}`)
       .then(r => setEntries(r.data)).catch(() => {})
  }

  useEffect(fetchEntries, [userId])

  const onSubmit = async (data: FormData) => {
    if (!userId) return
    setLoading(true)
    try {
      await api.post(`/users/${userId}/progress`, {
        date:             data.date,
        weightKg:         data.weightKg         ? parseFloat(data.weightKg)       : null,
        caloriesConsumed: data.caloriesConsumed  ? parseInt(data.caloriesConsumed) : null,
        waterMl:          data.waterMl           ? parseInt(data.waterMl)          : null,
        exerciseMinutes:  data.exerciseMinutes   ? parseInt(data.exerciseMinutes)  : null,
        notes:            data.notes || null,
      })
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
      reset({ date: new Date().toISOString().slice(0, 10) })
      fetchEntries()
    } catch { /* ignore */ }
    finally { setLoading(false) }
  }

  const latest   = entries.length > 0 ? entries[entries.length - 1] : null
  const previous = entries.length > 1 ? entries[entries.length - 2] : null
  const activeM  = CHART_METRICS.find(m => m.key === activeMetric)!

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">

      {/* Header */}
      <div>
        <h1 className="page-title">Progress</h1>
        <p className="page-subtitle">Track your daily stats and see how you're trending.</p>
      </div>

      {/* Latest snapshot */}
      {latest && (
        <div className="grid grid-cols-2 gap-3">
          {STATS.map(({ key, label, unit, icon, color, bg, border }) => {
            const val  = (latest   as any)[key]
            const prev = (previous as any)?.[key] ?? null
            const diff = val != null && prev != null ? +(val - prev).toFixed(1) : null
            return (
              <motion.div
                key={key}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="stat-tile cursor-pointer"
                style={{ '--tile-color': color, borderColor: `${color}35` } as React.CSSProperties}
                onClick={() => setActiveMetric(key)}
              >
                <div className="flex items-start justify-between mb-3">
                  <span className="text-2xl">{icon}</span>
                  <span className="section-label">{label}</span>
                </div>
                <p className="text-3xl font-black leading-none" style={{ color }}>
                  {val != null ? val : '—'}
                </p>
                <p className="text-xs text-slate-500 mt-1 font-medium">{unit}</p>
                {diff !== null && diff !== 0 && <TrendBadge diff={diff} unit={unit} />}
              </motion.div>
            )
          })}
        </div>
      )}

      {/* Trend chart */}
      <div className="card !p-0 overflow-hidden">
        <div className="px-6 pt-5 pb-3 border-b" style={{ background: 'rgba(0,0,0,0.25)', borderColor: 'rgba(255,255,255,0.08)' }}>
          <div className="flex items-center justify-between flex-wrap gap-3">
            <div>
              <h2 className="section-title">30-day trend</h2>
              <p className="text-xs mt-0.5" style={{ color: 'rgba(255,255,255,0.58)' }}>Dashed line = average</p>
            </div>
            <div className="flex gap-1.5 flex-wrap">
              {CHART_METRICS.map(m => (
                <button
                  key={m.key}
                  onClick={() => setActiveMetric(m.key)}
                  className="text-xs font-semibold px-3 py-1.5 rounded-full border transition-all"
                  style={activeMetric === m.key
                    ? { background: m.color, color: 'white', borderColor: m.color }
                    : { background: 'rgba(255,255,255,0.06)', color: 'rgba(255,255,255,0.65)', borderColor: 'rgba(255,255,255,0.1)' }}
                >
                  {m.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="px-4 py-4">
          <TrendChart
            entries={entries}
            metricKey={activeMetric}
            color={activeM.color}
            unit={activeM.unit}
          />
        </div>
      </div>

      {/* Log form */}
      <div className="card">
        <div className="flex items-center gap-2.5 mb-5">
          <div className="w-8 h-8 rounded-lg flex items-center justify-center text-base"
               style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)' }}>
            <span className="text-white text-sm font-bold">+</span>
          </div>
          <div>
            <h2 className="section-title">Log today's entry</h2>
            <p className="text-xs text-slate-400">All fields are optional — log what you have.</p>
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Date</label>
              <input className="input" type="date" {...register('date', { required: true })} />
            </div>
            <div>
              <label className="label">Weight (kg)</label>
              <input className="input" type="number" step="0.1" placeholder="70.5" {...register('weightKg')} />
            </div>
            <div>
              <label className="label">Calories (kcal)</label>
              <input className="input" type="number" placeholder="1800" {...register('caloriesConsumed')} />
            </div>
            <div>
              <label className="label">Water (ml)</label>
              <input className="input" type="number" placeholder="2000" {...register('waterMl')} />
            </div>
            <div>
              <label className="label">Exercise (min)</label>
              <input className="input" type="number" placeholder="30" {...register('exerciseMinutes')} />
            </div>
            <div>
              <label className="label">Notes</label>
              <input className="input" type="text" placeholder="Feeling great!" {...register('notes')} />
            </div>
          </div>

          <div className="flex items-center gap-4 pt-1">
            <button type="submit" disabled={loading} className="btn-primary">
              {loading ? 'Saving…' : 'Save entry'}
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
                Entry saved!
              </motion.div>
            )}
          </div>
        </form>
      </div>

      {/* History */}
      <div className="card !p-0 overflow-hidden">
        <div className="px-6 py-4 flex items-center justify-between border-b" style={{ background: 'rgba(0,0,0,0.25)', borderColor: 'rgba(255,255,255,0.08)' }}>
          <div>
            <h2 className="section-title">History</h2>
            <p className="text-xs mt-0.5" style={{ color: 'rgba(255,255,255,0.58)' }}>Last 30 days</p>
          </div>
          <span className="badge-gray">{entries.length} entries</span>
        </div>

        <div className="p-6">
          {entries.length === 0 ? (
            <div className="text-center py-10">
              <div className="text-4xl mb-3">📊</div>
              <p className="font-semibold" style={{ color: 'rgba(255,255,255,0.6)' }}>No entries yet</p>
              <p className="text-sm mt-1" style={{ color: 'rgba(255,255,255,0.58)' }}>Start logging above to track your journey.</p>
            </div>
          ) : (
            <div>{[...entries].reverse().map(e => <EntryRow key={e.id} entry={e} />)}</div>
          )}
        </div>
      </div>
    </div>
  )
}
