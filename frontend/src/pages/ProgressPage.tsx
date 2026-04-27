import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { motion } from 'framer-motion'
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

const STAT_FIELDS = [
  { key: 'weightKg',        label: 'Weight',    unit: 'kg',  icon: '⚖️',  color: 'text-blue-600',   bg: 'bg-blue-50' },
  { key: 'caloriesConsumed',label: 'Calories',  unit: 'kcal',icon: '🔥',  color: 'text-orange-600', bg: 'bg-orange-50' },
  { key: 'waterMl',         label: 'Water',     unit: 'ml',  icon: '💧',  color: 'text-cyan-600',   bg: 'bg-cyan-50' },
  { key: 'exerciseMinutes', label: 'Exercise',  unit: 'min', icon: '🏃',  color: 'text-green-600',  bg: 'bg-green-50' },
]

function EntryRow({ entry }: { entry: ProgressEntry }) {
  return (
    <div className="flex items-start justify-between py-3.5 border-b border-slate-100 last:border-0">
      <div>
        <p className="text-sm font-semibold text-slate-900">
          {new Date(entry.date).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })}
        </p>
        {entry.notes && <p className="text-xs text-slate-400 mt-0.5">{entry.notes}</p>}
      </div>
      <div className="flex flex-wrap justify-end gap-2">
        {entry.weightKg        != null && <span className="badge-blue">{entry.weightKg} kg</span>}
        {entry.caloriesConsumed != null && <span className="badge-orange">{entry.caloriesConsumed} kcal</span>}
        {entry.waterMl         != null && <span className="badge bg-cyan-100 text-cyan-700">{entry.waterMl} ml</span>}
        {entry.exerciseMinutes != null && <span className="badge-green">{entry.exerciseMinutes} min</span>}
      </div>
    </div>
  )
}

export default function ProgressPage() {
  const { userId } = useAuth()
  const [entries, setEntries] = useState<ProgressEntry[]>([])
  const [saved, setSaved]     = useState(false)
  const [loading, setLoading] = useState(false)

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
      const payload = {
        date:             data.date,
        weightKg:         data.weightKg         ? parseFloat(data.weightKg)         : null,
        caloriesConsumed: data.caloriesConsumed  ? parseInt(data.caloriesConsumed)   : null,
        waterMl:          data.waterMl           ? parseInt(data.waterMl)            : null,
        exerciseMinutes:  data.exerciseMinutes   ? parseInt(data.exerciseMinutes)    : null,
        notes:            data.notes || null,
      }
      await api.post(`/users/${userId}/progress`, payload)
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
      reset({ date: new Date().toISOString().slice(0, 10) })
      fetchEntries()
    } catch { /* ignore */ }
    finally { setLoading(false) }
  }

  const latest = entries[entries.length - 1]

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">

      {/* Header */}
      <div>
        <h1 className="page-title">Progress</h1>
        <p className="page-subtitle">Log your daily stats and track your journey.</p>
      </div>

      {/* Latest snapshot */}
      {latest && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {STAT_FIELDS.map(({ key, label, unit, icon, color, bg }) => {
            const val = (latest as any)[key]
            return (
              <div key={key} className="card flex flex-col gap-2 p-4">
                <div className={`w-8 h-8 rounded-lg ${bg} flex items-center justify-center text-base`}>{icon}</div>
                <span className="section-label">{label}</span>
                <span className={`text-xl font-bold ${color}`}>
                  {val != null ? val : '—'}
                  {val != null && <span className="text-xs font-normal text-slate-400 ml-1">{unit}</span>}
                </span>
              </div>
            )
          })}
        </div>
      )}

      {/* Log form */}
      <div className="card space-y-4">
        <h2 className="font-semibold text-slate-900">Log today's entry</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
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

          <div className="flex items-center gap-4">
            <button type="submit" disabled={loading} className="btn-primary">
              {loading ? 'Saving…' : 'Log entry'}
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
                Entry saved
              </motion.div>
            )}
          </div>
        </form>
      </div>

      {/* History */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-slate-900">Last 30 days</h2>
          <span className="badge-gray">{entries.length} entries</span>
        </div>
        {entries.length === 0 ? (
          <div className="text-center py-10">
            <div className="text-4xl mb-3">📊</div>
            <p className="text-slate-500 text-sm">No entries yet. Start logging today!</p>
          </div>
        ) : (
          <div>
            {[...entries].reverse().map(e => <EntryRow key={e.id} entry={e} />)}
          </div>
        )}
      </div>
    </div>
  )
}
