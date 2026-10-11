import { useCallback, useEffect, useState } from 'react'
import api from '../../lib/api'
import { toApiError, type FieldError } from '../../lib/errors'
import type { CatalogFood, FoodRole, Page } from '../../types'

/** The 14 allergens of EU Regulation 1169/2011 (BR-SAF-01). */
const ALLERGENS = [
  'GLUTEN', 'CRUSTACEANS', 'EGGS', 'FISH', 'PEANUTS', 'SOY', 'MILK',
  'TREE_NUTS', 'CELERY', 'MUSTARD', 'SESAME', 'SULPHITES', 'LUPIN', 'MOLLUSCS',
] as const
const DIET_FLAGS = ['VEGAN', 'VEGETARIAN', 'HALAL', 'KOSHER'] as const
const SLOTS = ['BREAKFAST', 'MORNING_SNACK', 'LUNCH', 'AFTERNOON_SNACK', 'DINNER'] as const
const ROLES: FoodRole[] = ['PROTEIN', 'CARB', 'PRODUCE', 'FAT']
const STATES = ['RAW', 'COOKED', 'DRY'] as const

const label = (code: string) => code.charAt(0) + code.slice(1).toLowerCase().replace(/_/g, ' ')

type FoodForm = Omit<CatalogFood, 'id' | 'origin' | 'active' | 'source' | 'category'> & {
  source: string
  category: string
}

const NUMBER_FIELDS: { key: keyof FoodForm; label: string }[] = [
  { key: 'caloriesPer100g', label: 'kcal' },
  { key: 'proteinPer100g', label: 'Protein g' },
  { key: 'carbsPer100g', label: 'Carbs g' },
  { key: 'fatPer100g', label: 'Fat g' },
  { key: 'sugarPer100g', label: 'Sugar g' },
  { key: 'fiberPer100g', label: 'Fiber g' },
  { key: 'sodiumMgPer100g', label: 'Sodium mg' },
  { key: 'portionMinG', label: 'Portion min g' },
  { key: 'portionMaxG', label: 'Portion max g' },
  { key: 'portionStepG', label: 'Portion step g' },
  { key: 'defaultPortionG', label: 'Default portion g' },
]

const EMPTY: FoodForm = {
  name: '', category: '', role: 'PROTEIN', state: 'COOKED',
  caloriesPer100g: 0, proteinPer100g: 0, carbsPer100g: 0, fatPer100g: 0,
  sugarPer100g: 0, fiberPer100g: 0, sodiumMgPer100g: 0,
  portionMinG: 50, portionMaxG: 250, portionStepG: 5, defaultPortionG: 100,
  allergens: [], dietFlags: [], suitableSlots: [], meat: false, source: '',
}

const toForm = (f: CatalogFood): FoodForm => {
  const { id: _id, origin: _origin, active: _active, ...rest } = f
  return { ...rest, category: f.category ?? '', source: f.source ?? '' }
}

function Checkboxes({ legend, options, value, onChange }: {
  legend: string
  options: readonly string[]
  value: string[]
  onChange: (next: string[]) => void
}) {
  return (
    <fieldset>
      <legend className="label">{legend}</legend>
      <div className="flex flex-wrap gap-x-4 gap-y-1">
        {options.map(o => (
          <label key={o} className="flex items-center gap-1.5 text-xs text-slate-300">
            <input
              type="checkbox"
              checked={value.includes(o)}
              onChange={e => onChange(e.target.checked ? [...value, o] : value.filter(v => v !== o))}
            />
            {label(o)}
          </label>
        ))}
      </div>
    </fieldset>
  )
}

/** Create or edit a food. The server runs the quality gate (BR-CAT-02) and lists what fails. */
function FoodEditor({ food, onSaved, onCancel }: {
  food: CatalogFood | null
  onSaved: () => void
  onCancel: () => void
}) {
  const [form, setForm] = useState<FoodForm>(food ? toForm(food) : EMPTY)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldError[]>([])
  const set = <K extends keyof FoodForm>(key: K, value: FoodForm[K]) => setForm(f => ({ ...f, [key]: value }))

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    setBusy(true); setError(''); setFieldErrors([])
    try {
      if (food) await api.put(`/admin/foods/${food.id}`, form)
      else await api.post('/admin/foods', form)
      onSaved()
    } catch (err) {
      const apiError = toApiError(err)
      setError(apiError.detail ?? apiError.message)
      setFieldErrors(apiError.fieldErrors)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={save} className="card space-y-4" aria-label={food ? `Edit ${food.name}` : 'New food'}>
      <h2 className="section-title">{food ? `Edit ${food.name}` : 'New food'}</h2>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <label className="col-span-2">
          <span className="label">Name</span>
          <input className="input" required maxLength={100} value={form.name} onChange={e => set('name', e.target.value)} />
        </label>
        <label>
          <span className="label">Role</span>
          <select className="input" value={form.role} onChange={e => set('role', e.target.value as FoodRole)}>
            {ROLES.map(r => <option key={r} value={r}>{label(r)}</option>)}
          </select>
        </label>
        <label>
          <span className="label">State</span>
          <select className="input" value={form.state} onChange={e => set('state', e.target.value as FoodForm['state'])}>
            {STATES.map(s => <option key={s} value={s}>{label(s)}</option>)}
          </select>
        </label>
        <label className="col-span-2">
          <span className="label">Category</span>
          <input className="input" maxLength={50} value={form.category} onChange={e => set('category', e.target.value)} />
        </label>
        <label className="col-span-2">
          <span className="label">Source</span>
          <input className="input" required maxLength={100} placeholder="USDA FoodData Central" value={form.source} onChange={e => set('source', e.target.value)} />
        </label>
        {NUMBER_FIELDS.map(({ key, label: text }) => (
          <label key={key}>
            <span className="label">{text}</span>
            <input
              className="input"
              type="number"
              min={0}
              step="any"
              required
              value={form[key] as number}
              onChange={e => set(key, Number(e.target.value) as never)}
            />
          </label>
        ))}
      </div>
      <p className="text-xs text-slate-400">Values are per 100 g, in the state above. Ticking no allergen declares that the food contains none of the 14.</p>
      <Checkboxes legend="Allergens" options={ALLERGENS} value={form.allergens} onChange={v => set('allergens', v)} />
      <Checkboxes legend="Diet flags" options={DIET_FLAGS} value={form.dietFlags} onChange={v => set('dietFlags', v)} />
      <Checkboxes legend="Meal slots" options={SLOTS} value={form.suitableSlots} onChange={v => set('suitableSlots', v)} />
      <label className="flex items-center gap-1.5 text-xs text-slate-300">
        <input type="checkbox" checked={form.meat} onChange={e => set('meat', e.target.checked)} />
        Land-animal meat
      </label>

      {error && (
        <div role="alert" className="text-sm text-red-300">
          <p>{error}</p>
          {fieldErrors.length > 0 && (
            <ul className="list-disc pl-5 text-xs mt-1">
              {fieldErrors.map((f, i) => <li key={i}>{f.field}: {f.message}</li>)}
            </ul>
          )}
        </div>
      )}

      <div className="flex gap-3">
        <button type="submit" className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save food'}</button>
        <button type="button" className="text-sm text-slate-400" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}

/** Curated catalog administration (FR-005). ADMIN only (BR-CAT-05). */
export default function FoodsPage() {
  const [q, setQ] = useState('')
  const [role, setRole] = useState<FoodRole | ''>('')
  const [active, setActive] = useState<'' | 'true' | 'false'>('')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<Page<CatalogFood> | null>(null)
  const [editing, setEditing] = useState<CatalogFood | 'new' | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    const params = new URLSearchParams({ page: String(page), size: '20' })
    if (q.trim()) params.set('q', q.trim())
    if (role) params.set('role', role)
    if (active) params.set('active', active)
    api.get<Page<CatalogFood>>(`/admin/foods?${params}`)
      .then(r => { setResult(r.data); setError('') })
      .catch(err => setError(toApiError(err).message))
  }, [q, role, active, page])

  useEffect(load, [load])

  /** Soft delete or restore (BR-CAT-04): plans that used the food keep it either way. */
  const toggle = async (food: CatalogFood) => {
    try {
      if (food.active) await api.delete(`/admin/foods/${food.id}`)
      else await api.post(`/admin/foods/${food.id}/activate`)
      load()
    } catch (err) {
      const e = toApiError(err)
      setError([e.detail ?? e.message, ...e.fieldErrors.map(f => `${f.field}: ${f.message}`)].join(' · '))
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-end justify-between gap-4 flex-wrap">
        <div>
          <h1 className="page-title">Food catalog</h1>
          <p className="page-subtitle">Seeded foods come from foods.csv and can only be deactivated here.</p>
        </div>
        <button type="button" className="btn-primary" onClick={() => setEditing('new')}>New food</button>
      </div>

      {editing && (
        <FoodEditor
          key={editing === 'new' ? 'new' : editing.id}
          food={editing === 'new' ? null : editing}
          onCancel={() => setEditing(null)}
          onSaved={() => { setEditing(null); load() }}
        />
      )}

      <div className="flex gap-3 flex-wrap">
        <input className="input max-w-xs" placeholder="Search by name" aria-label="Search by name"
               value={q} onChange={e => { setQ(e.target.value); setPage(0) }} />
        <select className="input max-w-[10rem]" aria-label="Role" value={role}
                onChange={e => { setRole(e.target.value as FoodRole | ''); setPage(0) }}>
          <option value="">All roles</option>
          {ROLES.map(r => <option key={r} value={r}>{label(r)}</option>)}
        </select>
        <select className="input max-w-[10rem]" aria-label="Status" value={active}
                onChange={e => { setActive(e.target.value as '' | 'true' | 'false'); setPage(0) }}>
          <option value="">Active and inactive</option>
          <option value="true">Active</option>
          <option value="false">Inactive</option>
        </select>
      </div>

      {error && <p role="alert" className="text-sm text-red-300">{error}</p>}

      <div className="card !p-0 overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs text-slate-400">
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3 text-right">kcal</th>
              <th className="px-4 py-3 text-right">P / C / F</th>
              <th className="px-4 py-3">Origin</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {result?.items.map(f => (
              <tr key={f.id} className={f.active ? '' : 'opacity-50'} style={{ borderTop: '1px solid rgba(255,255,255,0.06)' }}>
                <td className="px-4 py-2.5 text-slate-200">{f.name}{!f.active && ' (inactive)'}</td>
                <td className="px-4 py-2.5 text-slate-400">{label(f.role)}</td>
                <td className="px-4 py-2.5 text-right tabular-nums">{f.caloriesPer100g}</td>
                <td className="px-4 py-2.5 text-right tabular-nums text-slate-400">
                  {f.proteinPer100g} / {f.carbsPer100g} / {f.fatPer100g}
                </td>
                <td className="px-4 py-2.5 text-xs text-slate-400">{f.origin === 'SEED' ? 'Seed' : 'Admin'}</td>
                <td className="px-4 py-2.5 text-right whitespace-nowrap space-x-3">
                  {f.origin === 'ADMIN' && (
                    <button type="button" className="text-xs underline" onClick={() => setEditing(f)}>Edit</button>
                  )}
                  <button type="button" className="text-xs underline" onClick={() => toggle(f)}>
                    {f.active ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {result && result.totalPages > 1 && (
        <div className="flex items-center gap-3 text-sm">
          <button type="button" disabled={page === 0} onClick={() => setPage(p => p - 1)} className="underline disabled:opacity-40">Previous</button>
          <span className="text-slate-400">Page {page + 1} of {result.totalPages} · {result.totalItems} foods</span>
          <button type="button" disabled={page + 1 >= result.totalPages} onClick={() => setPage(p => p + 1)} className="underline disabled:opacity-40">Next</button>
        </div>
      )}
    </div>
  )
}
