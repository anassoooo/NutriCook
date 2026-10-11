import type { PlanWarning } from '../types'

/** Non-blocking issues the generator reported, e.g. TARGET_DEVIATION or LOW_PROTEIN (BR-CMP-05). */
export default function PlanWarnings({ warnings }: { warnings: PlanWarning[] | undefined }) {
  if (!warnings?.length) return null
  return (
    <div className="banner-warning text-sm" role="status" aria-label="Plan warnings">
      <span aria-hidden="true">⚠️</span>
      <ul className="space-y-0.5">
        {warnings.map((w) => (
          <li key={w.code}>{w.message}</li>
        ))}
      </ul>
    </div>
  )
}
