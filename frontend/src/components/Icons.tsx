// Line icons drawn for NutriCook: 24×24, 1.75 stroke, currentColor.
import type { SVGProps } from 'react'

type IconProps = SVGProps<SVGSVGElement> & { size?: number }

function Icon({ size = 20, children, ...rest }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      {...rest}
    >
      {children}
    </svg>
  )
}

export const PlateIcon = (p: IconProps) => (
  <Icon {...p}>
    <circle cx="12" cy="12" r="8.5" />
    <circle cx="12" cy="12" r="4.5" />
  </Icon>
)
export const TrendIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M3.5 17.5l5-5 3.5 3.5 7.5-7.5" />
    <path d="M15 8.5h4.5V13" />
  </Icon>
)
export const MedalIcon = (p: IconProps) => (
  <Icon {...p}>
    <circle cx="12" cy="14.5" r="5" />
    <path d="M9 9.8L6.5 3.5h4l1.5 3.5 1.5-3.5h4L15 9.8" />
  </Icon>
)
export const PinIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M12 21s-6.5-6.2-6.5-11a6.5 6.5 0 0113 0c0 4.8-6.5 11-6.5 11z" />
    <circle cx="12" cy="10" r="2.3" />
  </Icon>
)
export const PersonIcon = (p: IconProps) => (
  <Icon {...p}>
    <circle cx="12" cy="8.5" r="3.8" />
    <path d="M4.5 20c1.2-3.6 4-5.5 7.5-5.5s6.3 1.9 7.5 5.5" />
  </Icon>
)
export const ShelfIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M4 5.5h16M4 12h16M4 18.5h16" />
    <path d="M7 5.5v3M11 12v3M15 5.5v3M17 12v3M9 18.5v-3" />
  </Icon>
)
export const ExitIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M14.5 4.5H18a1.5 1.5 0 011.5 1.5v12a1.5 1.5 0 01-1.5 1.5h-3.5" />
    <path d="M10 16.5L5.5 12 10 7.5M5.5 12h10" />
  </Icon>
)
export const SwapIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M7 4.5L4 7.5l3 3M4 7.5h12.5M17 13.5l3 3-3 3M20 16.5H7.5" />
  </Icon>
)
export const ChevronIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M6.5 9.5L12 15l5.5-5.5" />
  </Icon>
)
export const CheckIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M5 12.5l4.5 4.5L19 7.5" />
  </Icon>
)
export const SparkIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M12 3.5l1.8 5.2 5.2 1.8-5.2 1.8L12 17.5l-1.8-5.2L5 10.5l5.2-1.8z" />
    <path d="M18.5 16.5l.7 2 2 .7-2 .7-.7 2-.7-2-2-.7 2-.7z" />
  </Icon>
)
export const RefreshIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M19.5 12a7.5 7.5 0 01-13.2 4.9M4.5 12a7.5 7.5 0 0113.2-4.9" />
    <path d="M18 3.5v4h-4M6 20.5v-4h4" />
  </Icon>
)
export const DropIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M12 3.5s6 6.4 6 10.5a6 6 0 01-12 0c0-4.1 6-10.5 6-10.5z" />
  </Icon>
)
export const ScaleIcon = (p: IconProps) => (
  <Icon {...p}>
    <rect x="4" y="4" width="16" height="16" rx="4" />
    <path d="M8 10.5a5 5 0 018 0" />
    <path d="M12 11l1.6-2.6" />
  </Icon>
)
export const FlameIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M12 20.5c3.6 0 6-2.4 6-5.8 0-4.5-4.7-6.2-4.2-11.2-3.4 1.6-5.6 4.8-5 8.3-1-.4-1.6-1.4-1.8-2.6C5.4 10.8 6 13 6 14.7c0 3.4 2.4 5.8 6 5.8z" />
  </Icon>
)
export const StrideIcon = (p: IconProps) => (
  <Icon {...p}>
    <circle cx="14" cy="4.8" r="1.8" />
    <path d="M9 21l2.5-6.5 3 2.5v4M7 11.5l3-3.5 4 1 2.5 3.5 2.5.5M11.5 14.5l1.5-5" />
  </Icon>
)
export const ChatIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M20 12a7.5 7.5 0 01-11.2 6.5L4 19.5l1.2-4.3A7.5 7.5 0 1120 12z" />
  </Icon>
)
export const CloseIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M6 6l12 12M18 6L6 18" />
  </Icon>
)
export const SendIcon = (p: IconProps) => (
  <Icon {...p}>
    <path d="M4.5 12h14M13 6.5l5.5 5.5-5.5 5.5" />
  </Icon>
)
