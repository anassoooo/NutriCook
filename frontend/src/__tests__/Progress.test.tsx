import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../lib/errors'
import type { DietPlan, Meal, ProgressEntry } from '../types'

const { apiGet, apiPost, apiPut } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
}))
vi.mock('../lib/api', () => ({ default: { get: apiGet, post: apiPost, put: apiPut } }))
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => ({ userId: 7 }) }))

import MealCheck from '../components/MealCheck'
import ProgressPage from '../pages/ProgressPage'
import { BACKFILL_DAYS, daysAgo, localIsoDate } from '../lib/progressRules'

const lunch = { id: 9, name: 'Lunch', mealType: 'LUNCH', calories: 820, completed: false } as Meal

const entry = (over: Partial<ProgressEntry>): ProgressEntry => ({
  id: 1,
  date: localIsoDate(),
  weightKg: null,
  caloriesConsumed: 820,
  caloriesOverridden: false,
  mealsCompleted: 1,
  targetCalories: 2700,
  waterMl: null,
  exerciseMinutes: null,
  notes: null,
  ...over,
})

describe('MealCheck', () => {
  beforeEach(() => apiPut.mockReset())

  it('BR_PRG_05_checkingAMealOffUpdatesPlanAndDay', async () => {
    const plan = { id: 5 } as DietPlan
    const progress = entry({})
    apiPut.mockResolvedValue({ data: { plan, progress, newAchievements: [] } })
    const onChanged = vi.fn()
    render(<MealCheck planId={5} meal={lunch} onChanged={onChanged} />)

    const box = screen.getByRole('checkbox', { name: 'I ate lunch' })
    expect(box).toHaveAttribute('aria-checked', 'false')
    await userEvent.click(box)

    expect(apiPut).toHaveBeenCalledWith('/users/7/diet-plans/5/meals/9/completion', { completed: true })
    expect(onChanged).toHaveBeenCalledWith(plan, progress)
  })

  it('BR_PRG_02_showsWhyAMealCannotBeCheckedOff', async () => {
    const error = new ApiError({
      code: 'VALIDATION_FAILED',
      status: 400,
      detail: 'Progress can be logged for today and the past 30 days.',
    })
    // the test runner may invoke the mock without arguments: reject real requests only
    apiPut.mockImplementation((url?: string) => (url ? Promise.reject(error) : Promise.resolve({ data: {} })))
    render(<MealCheck planId={5} meal={{ ...lunch, completed: true }} onChanged={vi.fn()} />)

    await userEvent.click(screen.getByRole('checkbox', { name: 'I ate lunch' }))

    expect(apiPut).toHaveBeenCalledWith('/users/7/diet-plans/5/meals/9/completion', { completed: false })
    expect(await screen.findByRole('alert')).toHaveTextContent('the past 30 days')
  })
})

describe('ProgressPage', () => {
  beforeEach(() => {
    apiGet.mockReset()
    apiPost.mockReset()
  })

  it('BR_PRG_03_inputsMirrorTheServerBounds', async () => {
    apiGet.mockResolvedValue({ data: [] })
    const { container } = render(<ProgressPage />)

    const date = container.querySelector('input[type="date"]')!
    expect(date).toHaveAttribute('min', daysAgo(BACKFILL_DAYS))
    expect(date).toHaveAttribute('max', localIsoDate())
    const weight = screen.getByPlaceholderText('70.5')
    expect(weight).toHaveAttribute('min', '30')
    expect(weight).toHaveAttribute('max', '300')
    expect(screen.getByPlaceholderText('30')).toHaveAttribute('max', '1440')
    expect(screen.getByPlaceholderText('Feeling great!')).toHaveAttribute('maxLength', '1000')
  })

  it('BR_PRG_05_showsManualCaloriesWithAResetToMeals', async () => {
    const day = entry({ caloriesConsumed: 2600, caloriesOverridden: true })
    apiGet.mockResolvedValue({ data: [day] })
    apiPost.mockResolvedValue({ data: {} })
    render(<ProgressPage />)

    const history = (await screen.findByText('2600 kcal · manual')).parentElement!
    await userEvent.click(within(history).getByRole('button', { name: 'Use checked meals' }))

    expect(apiPost).toHaveBeenCalledWith('/users/7/progress', { date: day.date, resetCalories: true })
  })

  it('BR_PRG_05_derivedCaloriesAreMarkedAuto', async () => {
    apiGet.mockResolvedValue({ data: [entry({})] })
    render(<ProgressPage />)
    expect(await screen.findByText('820 kcal · auto')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Use checked meals' })).not.toBeInTheDocument()
  })

  it('BR_PRG_03_listsTheFieldsTheServerRejected', async () => {
    apiGet.mockResolvedValue({ data: [] })
    apiPost.mockImplementation((url?: string) =>
      url
        ? Promise.reject(
            new ApiError({
              code: 'VALIDATION_FAILED',
              status: 400,
              detail: 'Some fields are invalid.',
              fieldErrors: [{ field: 'waterMl', message: 'must be between 0 and 10000' }],
            }),
          )
        : Promise.resolve({ data: {} }),
    )
    render(<ProgressPage />)

    await userEvent.type(screen.getByPlaceholderText('2000'), '500')
    await userEvent.click(screen.getByRole('button', { name: 'Save entry' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Some fields are invalid.')
    expect(alert).toHaveTextContent('Water (ml): must be between 0 and 10000')
  })
})
