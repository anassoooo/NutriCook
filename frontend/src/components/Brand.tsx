import { Link } from 'react-router-dom'
import { ROLE_COLORS } from '../lib/roles'

/** Logo mark: a plate whose rim carries the four role colours — the dashboard plate in miniature. */
export function PlateMark({ size = 28 }: { size?: number }) {
  const r = 10
  const c = 2 * Math.PI * r
  const seg = c / 4
  const colors = [ROLE_COLORS.PROTEIN, ROLE_COLORS.CARB, ROLE_COLORS.PRODUCE, ROLE_COLORS.FAT]
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" aria-hidden>
      <circle cx="14" cy="14" r="13" fill="#2B1E33" />
      {colors.map((color, i) => (
        <circle
          key={color}
          cx="14"
          cy="14"
          r={r}
          fill="none"
          stroke={color}
          strokeWidth="3.2"
          strokeDasharray={`${seg - 1.6} ${c - seg + 1.6}`}
          strokeDashoffset={-i * seg}
          transform="rotate(-90 14 14)"
        />
      ))}
      <circle cx="14" cy="14" r="5.2" fill="#F3F5F1" />
    </svg>
  )
}

export function Wordmark({ to = '/', light = false }: { to?: string; light?: boolean }) {
  return (
    <Link to={to} className="inline-flex items-center gap-2.5 select-none">
      <PlateMark />
      <span className={`font-heading text-[1.35rem] font-bold tracking-tight ${light ? 'text-chalk' : 'text-ink'}`}>
        NutriCook
      </span>
    </Link>
  )
}
