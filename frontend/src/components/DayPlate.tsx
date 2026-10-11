import { motion, useReducedMotion } from 'framer-motion'
import type { Meal } from '../types'
import { ROLE_COLORS } from '../lib/roles'

const SIZE = 320
const C = SIZE / 2
const R_OUT = 148
const R_IN = 104
const GAP_DEG = 3

function point(r: number, deg: number) {
  const a = ((deg - 90) * Math.PI) / 180
  return [C + r * Math.cos(a), C + r * Math.sin(a)]
}

/** An annular sector between two angles (degrees, clockwise from 12 o'clock). */
function sector(a0: number, a1: number, rOut = R_OUT, rIn = R_IN) {
  const large = a1 - a0 > 180 ? 1 : 0
  const [x0, y0] = point(rOut, a0)
  const [x1, y1] = point(rOut, a1)
  const [x2, y2] = point(rIn, a1)
  const [x3, y3] = point(rIn, a0)
  return `M${x0} ${y0} A${rOut} ${rOut} 0 ${large} 1 ${x1} ${y1} L${x2} ${y2} A${rIn} ${rIn} 0 ${large} 0 ${x3} ${y3}Z`
}

/**
 * Today as a plate: one slice per meal, sized by its calories, divided into its foods and coloured
 * by role (protein, carbs, fruit and veg, fat). Eaten meals are solid, the rest faded. Every slice
 * links to its meal in the list.
 */
export default function DayPlate({
  meals,
  eatenKcal,
  plannedKcal,
  targetKcal,
}: {
  meals: Meal[]
  eatenKcal: number | null
  plannedKcal: number
  targetKcal: number | null
}) {
  const reduce = useReducedMotion()
  const total = meals.reduce((s, m) => s + (m.calories ?? 0), 0) || 1
  const usable = 360 - GAP_DEG * meals.length

  let cursor = GAP_DEG / 2
  const slices = meals.map((meal) => {
    const span = ((meal.calories ?? 0) / total) * usable
    const start = cursor
    cursor += span + GAP_DEG
    const parts = meal.components.length ? meal.components : []
    const partTotal = parts.reduce((s, p) => s + p.kcal, 0) || 1
    let inner = start
    const pieces = parts.map((p) => {
      const a0 = inner
      inner += (p.kcal / partTotal) * span
      return { key: `${p.foodId}-${p.role}`, a0, a1: inner, color: ROLE_COLORS[p.role] ?? '#8E8795' }
    })
    return { meal, start, end: start + span, pieces }
  })

  const eaten = eatenKcal ?? 0
  const describe = `${meals.filter((m) => m.completed).length} of ${meals.length} meals eaten, ${eaten} of ${plannedKcal} planned calories`

  return (
    <figure className="relative mx-auto w-full max-w-[340px]" aria-label={`Today's plate: ${describe}`}>
      <svg viewBox={`0 0 ${SIZE} ${SIZE}`} className="w-full h-auto overflow-visible">
        {/* the plate itself */}
        <circle cx={C} cy={C} r={R_OUT + 9} fill="#FFFFFF" stroke="#D9DED3" />
        <circle cx={C} cy={C} r={R_IN - 8} fill="#F3F5F1" />

        {slices.map(({ meal, start, end, pieces }, i) => {
          const label = `${meal.name}: ${meal.calories} kcal, ${meal.completed ? 'eaten' : 'not eaten yet'}`
          return (
            <a key={meal.id} href={`#meal-${meal.id}`} aria-label={label}>
              <title>{label}</title>
              <motion.g
                initial={reduce ? false : { opacity: 0, scale: 0.92 }}
                animate={{ opacity: meal.completed ? 1 : 0.32, scale: 1 }}
                transition={{ delay: reduce ? 0 : 0.08 * i, duration: 0.45, ease: [0.2, 0.7, 0.2, 1] }}
                style={{ transformOrigin: `${C}px ${C}px` }}
              >
                {pieces.map((p) => (
                  <path key={p.key} d={sector(p.a0, p.a1)} fill={p.color} stroke="#FFFFFF" strokeWidth={1.5} />
                ))}
              </motion.g>
              {/* eaten marker on the inner edge */}
              {meal.completed && <path d={sector(start, end, R_IN - 2, R_IN - 6)} fill="#2B1E33" />}
            </a>
          )
        })}
      </svg>

      <figcaption className="pointer-events-none absolute inset-0 grid place-items-center text-center">
        <div>
          <p className="font-heading text-[2.6rem] font-bold leading-none num">{eaten.toLocaleString()}</p>
          <p className="mt-1.5 text-sm text-ink-soft num">of {plannedKcal.toLocaleString()} kcal</p>
          {targetKcal !== null && <p className="text-xs text-ink-faint num">target {targetKcal.toLocaleString()}</p>}
        </div>
      </figcaption>
    </figure>
  )
}

/** The four roles as a key, in the plate's colours. */
export function RoleKey() {
  const items: [keyof typeof ROLE_COLORS, string][] = [
    ['PROTEIN', 'Protein'],
    ['CARB', 'Carbs'],
    ['PRODUCE', 'Fruit & veg'],
    ['FAT', 'Fat'],
  ]
  return (
    <ul className="flex flex-wrap justify-center gap-x-4 gap-y-1 text-xs text-ink-soft" aria-label="Colour key">
      {items.map(([role, label]) => (
        <li key={role} className="inline-flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full" style={{ background: ROLE_COLORS[role] }} />
          {label}
        </li>
      ))}
    </ul>
  )
}
