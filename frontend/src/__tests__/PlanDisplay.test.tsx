import { act, render, renderHook, screen } from '@testing-library/react'
import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import MealComponents, { MealComponentsSummary } from '../components/MealComponents'
import PlanWarnings from '../components/PlanWarnings'
import { planErrorMessage, ERROR_MESSAGES } from '../lib/errors'
import type { DietPlan, MealComponent } from '../types'

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }))
vi.mock('../lib/api', () => ({ default: { get: apiGet } }))

import { useAiRefresh, AI_REFRESH_DELAYS } from '../hooks/useAiRefresh'

const lunch: MealComponent[] = [
  { foodId: 1, foodName: 'Chicken Breast', role: 'PROTEIN', grams: 115, kcal: 189.8, proteinG: 35.7, carbsG: 0, fatG: 4.1 },
  { foodId: 2, foodName: 'White Rice', role: 'CARB', grams: 300, kcal: 390, proteinG: 8.1, carbsG: 84.6, fatG: 0.9 },
  { foodId: 3, foodName: 'Broccoli', role: 'PRODUCE', grams: 150, kcal: 51, proteinG: 4.2, carbsG: 9.9, fatG: 0.6 },
]

function problem(status: number, body: Record<string, unknown>) {
  const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig
  return new AxiosError('x', undefined, config, {}, { status, data: body, statusText: '', headers: {}, config })
}

describe('meal components', () => {
  it('BR_CMP_02_listsEveryFoodWithItsPortion', () => {
    render(<MealComponents components={lunch} />)
    expect(screen.getByRole('list', { name: 'Foods in this meal' }).children).toHaveLength(3)
    expect(screen.getByText('Chicken Breast')).toBeInTheDocument()
    expect(screen.getByText('300 g · 390 kcal')).toBeInTheDocument()
    expect(screen.getByText('Fruit & veg')).toBeInTheDocument()
  })

  it('summarises the meal on one line', () => {
    render(<MealComponentsSummary components={lunch} />)
    expect(screen.getByTestId('meal-summary')).toHaveTextContent(
      'Chicken Breast 115 g · White Rice 300 g · Broccoli 150 g',
    )
  })

  it('renders nothing for meals without components', () => {
    const { container } = render(<MealComponents components={[]} />)
    expect(container).toBeEmptyDOMElement()
  })
})

describe('plan warnings', () => {
  it('BR_CMP_05_showsEveryWarningMessage', () => {
    render(
      <PlanWarnings
        warnings={[
          { code: 'TARGET_DEVIATION', message: 'This plan is 6.2% below your calorie target.' },
          { code: 'LOW_PROTEIN', message: 'This plan has 72 g of protein; your target is 90 g.' },
        ]}
      />,
    )
    expect(screen.getByRole('status', { name: 'Plan warnings' })).toHaveTextContent('6.2% below')
    expect(screen.getByText(/72 g of protein/)).toBeInTheDocument()
  })

  it('is hidden when there are none', () => {
    const { container } = render(<PlanWarnings warnings={[]} />)
    expect(container).toBeEmptyDOMElement()
  })
})

describe('planErrorMessage', () => {
  it('BR_SAF_04_explainsWhichConstraintMadeThePlanInfeasible', () => {
    const msg = planErrorMessage(
      problem(422, {
        code: 'PLAN_INFEASIBLE',
        detail: 'We could not build a plan that respects Diabetes.',
        reason: 'The breakfast has 22.6 g of sugar; the limit for Diabetes is 15.0 g per meal.',
      }),
    )
    expect(msg).toBe(
      'We could not build a plan that respects Diabetes. The breakfast has 22.6 g of sugar; the limit for Diabetes is 15.0 g per meal.',
    )
  })

  it('uses the standard message for other errors', () => {
    expect(planErrorMessage(problem(429, { code: 'GENERATION_LIMIT' }))).toBe(ERROR_MESSAGES.GENERATION_LIMIT)
  })
})

describe('useAiRefresh', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    apiGet.mockReset()
  })
  afterEach(() => vi.useRealTimers())

  const pending = { id: 5, aiStatus: 'PENDING' } as DietPlan

  it('BR_LIF_07_refetchesUntilTheAiTextIsReady', async () => {
    const ready = { id: 5, aiStatus: 'READY', aiExplanation: 'Balanced day.' } as DietPlan
    apiGet.mockResolvedValue({ data: ready })
    const setPlan = vi.fn()

    renderHook(() => useAiRefresh(7, pending, setPlan))
    expect(apiGet).not.toHaveBeenCalled()

    await act(async () => {
      await vi.advanceTimersByTimeAsync(AI_REFRESH_DELAYS[0])
    })
    expect(apiGet).toHaveBeenCalledWith('/users/7/diet-plans/5')
    expect(setPlan).toHaveBeenCalledWith(ready)
  })

  it('does nothing once the AI text is ready', async () => {
    renderHook(() => useAiRefresh(7, { ...pending, aiStatus: 'READY' }, vi.fn()))
    await act(async () => {
      await vi.advanceTimersByTimeAsync(10_000)
    })
    expect(apiGet).not.toHaveBeenCalled()
  })
})
