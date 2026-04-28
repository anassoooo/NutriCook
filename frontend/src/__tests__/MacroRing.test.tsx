import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import MacroRing from '../components/MacroRing'

// MUI Tooltip wraps content — mock it to avoid emotion/provider issues
vi.mock('@mui/material/Tooltip', () => ({
  default: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

describe('MacroRing', () => {
  it('renders the label', () => {
    render(<MacroRing label="Protein" value={50} max={100} color="#3b82f6" />)
    expect(screen.getByText('Protein')).toBeInTheDocument()
  })

  it('renders the numeric value', () => {
    render(<MacroRing label="Carbs" value={75} max={200} color="#f59e0b" />)
    expect(screen.getByText('75')).toBeInTheDocument()
  })

  it('renders the unit (default g)', () => {
    render(<MacroRing label="Fat" value={20} max={90} color="#ef4444" />)
    expect(screen.getByText('g')).toBeInTheDocument()
  })

  it('renders a custom unit', () => {
    render(<MacroRing label="Calories" value={300} max={2000} color="#10b981" unit="kcal" />)
    expect(screen.getByText('kcal')).toBeInTheDocument()
  })

  it('rounds the displayed value', () => {
    render(<MacroRing label="Test" value={33.7} max={100} color="#000" />)
    expect(screen.getByText('34')).toBeInTheDocument()
  })

  it('renders an SVG ring', () => {
    const { container } = render(<MacroRing label="Ring" value={50} max={100} color="#16a34a" />)
    expect(container.querySelector('svg')).toBeInTheDocument()
  })
})
