import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, ERROR_MESSAGES } from '../lib/errors'
import type { DietPlan } from '../types'

const { apiGet, apiPost } = vi.hoisted(() => ({ apiGet: vi.fn(), apiPost: vi.fn() }))
vi.mock('../lib/api', () => ({ default: { get: apiGet, post: apiPost } }))
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => ({ userId: 7 }) }))

import SwapMenu from '../components/SwapMenu'
import ChatPanel, { MAX_MESSAGE_LENGTH } from '../components/ChatPanel'

const alternatives = [
  {
    role: 'PROTEIN',
    options: [
      { foodId: 11, foodName: 'Tofu (firm)', grams: 125, kcal: 180, proteinG: 21.6 },
      { foodId: 12, foodName: 'Lentils', grams: 175, kcal: 203, proteinG: 15.8 },
    ],
  },
  { role: 'CARB', options: [{ foodId: 21, foodName: 'Quinoa', grams: 300, kcal: 360, proteinG: 13.2 }] },
]

describe('SwapMenu', () => {
  beforeEach(() => {
    apiGet.mockReset()
    apiPost.mockReset()
  })

  it('BR_AI_03_listsAlternativesPerRoleAndSwapsOnClick', async () => {
    apiGet.mockResolvedValue({ data: alternatives })
    const updated = { id: 5 } as DietPlan
    apiPost.mockResolvedValue({ data: updated })
    const onSwapped = vi.fn()
    render(<SwapMenu planId={5} mealId={9} onSwapped={onSwapped} />)

    await userEvent.click(screen.getByRole('button', { name: 'Swap a food' }))
    expect(apiGet).toHaveBeenCalledWith('/users/7/diet-plans/5/meals/9/alternatives')
    expect(screen.getByRole('tab', { name: 'Protein' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByText('125 g · 180 kcal')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('tab', { name: 'Carbs' }))
    await userEvent.click(screen.getByRole('button', { name: /Quinoa/ }))

    expect(apiPost).toHaveBeenCalledWith('/users/7/diet-plans/5/meals/9/swap', { foodId: 21 })
    expect(onSwapped).toHaveBeenCalledWith(updated)
  })

  it('BR_LIF_04_showsTheServerReasonWhenASwapIsRefused', async () => {
    apiGet.mockResolvedValue({ data: alternatives })
    apiPost.mockRejectedValue(
      new ApiError({ code: 'PLAN_READ_ONLY', status: 409, detail: 'This plan is archived and can no longer be changed.' }),
    )
    render(<SwapMenu planId={5} mealId={9} onSwapped={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: 'Swap a food' }))
    await userEvent.click(screen.getByRole('button', { name: /Tofu/ }))

    expect(await screen.findByRole('alert')).toHaveTextContent('This plan is archived and can no longer be changed.')
  })
})

describe('ChatPanel', () => {
  // jsdom has no layout, so no scrollIntoView
  beforeAll(() => {
    Element.prototype.scrollIntoView = vi.fn()
  })
  beforeEach(() => apiPost.mockReset())

  /** Rejects real requests only: the test runner may invoke the mock without arguments. */
  function failWith(error: ApiError) {
    apiPost.mockImplementation((url?: string) =>
      url ? Promise.reject(error) : Promise.resolve({ data: {} }),
    )
  }

  async function openAndSend(text: string) {
    render(<ChatPanel planId={5} onPlanUpdate={vi.fn()} triggerMessage={text} onTriggerConsumed={vi.fn()} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Send message' }))
  }

  it('BR_AI_07_showsTheRateLimitMessage', async () => {
    failWith(new ApiError({ code: 'RATE_LIMITED', status: 429 }))
    await openAndSend('hello')
    expect(await screen.findByText(ERROR_MESSAGES.RATE_LIMITED)).toBeInTheDocument()
  })

  it('BR_AI_05_showsTheAiUnavailableMessage', async () => {
    failWith(new ApiError({ code: 'AI_UNAVAILABLE', status: 503 }))
    await openAndSend('hello')
    expect(await screen.findByText(ERROR_MESSAGES.AI_UNAVAILABLE)).toBeInTheDocument()
  })

  it('BR_AI_06_limitsMessagesTo500CharactersWithACounter', async () => {
    render(<ChatPanel planId={5} onPlanUpdate={vi.fn()} triggerMessage="hi" onTriggerConsumed={vi.fn()} />)
    const input = await screen.findByPlaceholderText('Ask me anything…')
    expect(input).toHaveAttribute('maxLength', String(MAX_MESSAGE_LENGTH))
    expect(screen.getByText(`2/${MAX_MESSAGE_LENGTH}`)).toBeInTheDocument()
  })
})
