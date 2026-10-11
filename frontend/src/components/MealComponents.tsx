import type { MealComponent } from '../types'
import { ROLE_COLORS } from '../lib/roles'

const ROLE_LABEL: Record<MealComponent['role'], string> = {
  PROTEIN: 'Protein',
  CARB: 'Carbs',
  PRODUCE: 'Fruit & veg',
  FAT: 'Healthy fat',
}

/** One line: "Chicken Breast 115 g · White Rice 300 g · Broccoli 150 g", with role dots. */
export function MealComponentsSummary({ components }: { components: MealComponent[] | undefined }) {
  if (!components?.length) return null
  return (
    <p className="mt-1.5 flex flex-wrap gap-x-4 gap-y-1 text-sm text-ink-soft" data-testid="meal-summary">
      {components.map((c, i) => (
        <span key={`${c.foodId}-${c.role}`} className="inline-flex items-baseline">
          <span className="mr-1.5 inline-block h-2 w-2 shrink-0 self-center rounded-full" style={{ background: ROLE_COLORS[c.role] }} aria-hidden />
          {`${c.foodName} ${Math.round(c.grams)} g`}
          {/* read as one line by assistive tech and copy/paste */}
          {i < components.length - 1 && <span className="sr-only"> · </span>}
        </span>
      ))}
    </p>
  )
}

/** The foods and portions in a meal, with the energy each brings (BR-CMP-02/03). */
export default function MealComponents({ components }: { components: MealComponent[] | undefined }) {
  if (!components?.length) return null
  return (
    <ul className="divide-y divide-line" aria-label="Foods in this meal">
      {components.map((c) => (
        <li key={`${c.foodId}-${c.role}`} className="flex items-baseline justify-between gap-4 py-2 text-sm">
          <span className="flex min-w-0 items-baseline gap-2.5">
            <span className="h-2.5 w-2.5 shrink-0 translate-y-[1px] rounded-full" style={{ background: ROLE_COLORS[c.role] }} aria-hidden />
            <span className="truncate text-ink">{c.foodName}</span>
            <span className="text-xs text-ink-faint">{ROLE_LABEL[c.role]}</span>
          </span>
          <span className="shrink-0 text-ink-soft num">
            {Math.round(c.grams)} g · {Math.round(c.kcal)} kcal
          </span>
        </li>
      ))}
    </ul>
  )
}
