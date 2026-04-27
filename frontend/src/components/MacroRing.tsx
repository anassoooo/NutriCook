interface MacroRingProps {
  label: string
  value: number
  max: number
  color: string
  trackColor?: string
  unit?: string
  size?: number
  strokeWidth?: number
}

export default function MacroRing({
  label, value, max, color, trackColor = '#f1f5f9',
  unit = 'g', size = 84, strokeWidth = 9,
}: MacroRingProps) {
  const r     = (size - strokeWidth) / 2
  const circ  = 2 * Math.PI * r
  const pct   = Math.min(value / max, 1)
  const offset = circ * (1 - pct)
  const cx    = size / 2
  const cy    = size / 2

  return (
    <div className="flex flex-col items-center gap-2.5">
      <div className="relative" style={{ width: size, height: size }}>
        <svg
          width={size}
          height={size}
          viewBox={`0 0 ${size} ${size}`}
          style={{ transform: 'rotate(-90deg)' }}
        >
          {/* Track */}
          <circle cx={cx} cy={cy} r={r} fill="none" stroke={trackColor} strokeWidth={strokeWidth} />
          {/* Fill */}
          <circle
            cx={cx} cy={cy} r={r}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeLinecap="round"
            strokeDasharray={circ}
            strokeDashoffset={offset}
            style={{
              transition: 'stroke-dashoffset 1.1s cubic-bezier(.4,0,.2,1)',
            }}
          />
        </svg>
        {/* Center label */}
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-px">
          <span className="text-sm font-bold text-slate-900 leading-none">
            {Math.round(value)}
          </span>
          <span className="text-[10px] text-slate-400 leading-none">{unit}</span>
        </div>
      </div>
      <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider leading-none">
        {label}
      </span>
    </div>
  )
}
