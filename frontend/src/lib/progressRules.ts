/**
 * Progress logging limits, mirroring BR-PRG-02/03 (docs/domain/business-rules.md §8).
 * They only guide the form: the server checks every value again and stays authoritative.
 */
export const PROGRESS_BOUNDS = {
  caloriesConsumed: { min: 0, max: 10000 },
  waterMl: { min: 0, max: 10000 },
  exerciseMinutes: { min: 0, max: 1440 },
  weightKg: { min: 30, max: 300 },
} as const

export const MAX_NOTES_LENGTH = 1000

/** Days in the past that can still be logged (BR-PRG-02). */
export const BACKFILL_DAYS = 30

/** A local calendar date as YYYY-MM-DD (not the UTC date that `toISOString` gives). */
export function localIsoDate(d: Date = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function daysAgo(n: number, from: Date = new Date()): string {
  const d = new Date(from)
  d.setDate(d.getDate() - n)
  return localIsoDate(d)
}
