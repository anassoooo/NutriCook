import type { ActivityLevel, HealthGoal } from '../types'

/** Accepted ranges, mirroring BR-PRF-03. They guide the form; the server checks again. */
export const PROFILE_BOUNDS = {
  weightKg: { min: 30, max: 300 },
  heightCm: { min: 120, max: 230 },
} as const

export const ACTIVITY_OPTIONS: { value: ActivityLevel; label: string; sub: string }[] = [
  { value: 'SEDENTARY', label: 'Mostly sitting', sub: 'Desk job, little exercise' },
  { value: 'LIGHTLY_ACTIVE', label: 'Lightly active', sub: 'Exercise 1–3 times a week' },
  { value: 'MODERATELY_ACTIVE', label: 'Moderately active', sub: 'Exercise 3–5 times a week' },
  { value: 'VERY_ACTIVE', label: 'Very active', sub: 'Exercise 6–7 times a week' },
  { value: 'EXTRA_ACTIVE', label: 'Extra active', sub: 'Physical job or athlete training' },
]

export const GOAL_OPTIONS: { value: HealthGoal; label: string; sub: string }[] = [
  { value: 'LOSE_WEIGHT', label: 'Lose weight', sub: 'A moderate daily deficit' },
  { value: 'MAINTAIN_WEIGHT', label: 'Keep my weight', sub: 'Eat what you burn' },
  { value: 'GAIN_MUSCLE', label: 'Build muscle', sub: 'A small surplus, more protein' },
  { value: 'IMPROVE_HEALTH', label: 'Eat healthier', sub: 'Balanced meals, no deficit' },
]

/** How restriction types are grouped on screen. */
export const RESTRICTION_GROUPS: { type: string; label: string; hint: string }[] = [
  { type: 'ALLERGY', label: 'Allergies', hint: 'Foods that contain these are never planned.' },
  { type: 'INTOLERANCE', label: 'Intolerances', hint: 'Foods that contain these are left out.' },
  { type: 'PREFERENCE', label: 'Diets', hint: 'Only foods labelled for the diet are planned.' },
  { type: 'RELIGIOUS', label: 'Religious', hint: 'Only foods labelled for it are planned.' },
]
