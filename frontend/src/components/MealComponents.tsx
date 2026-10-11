import type { MealComponent } from '../types'

const ROLE_LABEL: Record<MealComponent['role'], string> = {
  PROTEIN: 'Protein',
  CARB: 'Carbs',
  PRODUCE: 'Fruit & veg',
  FAT: 'Healthy fat',
}

/** One line: "Chicken Breast 115 g · White Rice 300 g · Broccoli 150 g". */
export function MealComponentsSummary({ components }: { components: MealComponent[] | undefined }) {
  if (!components?.length) return null
  return (
    <p className="text-white/80 text-xs mt-1 leading-snug" data-testid="meal-summary">
      {components.map((c) => `${c.foodName} ${Math.round(c.grams)} g`).join(' · ')}
    </p>
  )
}

/** The foods and portions in a meal, with the energy each brings (BR-CMP-02/03). */
export default function MealComponents({ components }: { components: MealComponent[] | undefined }) {
  if (!components?.length) return null
  return (
    <ul className="space-y-1.5" aria-label="Foods in this meal">
      {components.map((c) => (
        <li key={`${c.foodId}-${c.role}`} className="flex items-center justify-between text-sm">
          <span className="flex items-center gap-2">
            <span
              className="text-[10px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded"
              style={{ background: 'rgba(255,255,255,0.08)', color: 'rgba(255,255,255,0.6)' }}
            >
              {ROLE_LABEL[c.role]}
            </span>
            <span style={{ color: 'rgba(255,255,255,0.85)' }}>{c.foodName}</span>
          </span>
          <span className="tabular-nums" style={{ color: 'rgba(255,255,255,0.6)' }}>
            {Math.round(c.grams)} g · {Math.round(c.kcal)} kcal
          </span>
        </li>
      ))}
    </ul>
  )
}
