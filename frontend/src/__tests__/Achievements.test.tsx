import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Achievement } from '../types'

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }))
vi.mock('../lib/api', () => ({ default: { get: apiGet } }))
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => ({ userId: 7 }) }))

import AchievementsPage from '../pages/AchievementsPage'

const achievement = (over: Partial<Achievement>): Achievement => ({
  id: 1,
  name: 'Hydration Hero',
  description: 'Drink at least 2,000 ml of water on 7 of the last 30 days.',
  iconName: 'droplet',
  type: 'HYDRATION',
  threshold: 7,
  currentValue: 3,
  earned: false,
  earnedAt: null,
  ...over,
})

describe('AchievementsPage', () => {
  it('BR_ACH_04_showsServerProgressForLockedAchievements', async () => {
    apiGet.mockResolvedValue({
      data: [
        achievement({}),
        achievement({
          id: 2,
          name: 'On the Move',
          type: 'WEIGHT_MILESTONE',
          threshold: 2,
          currentValue: 1.4,
          description: 'Change your weight by 2 kg in your goal direction.',
        }),
      ],
    })
    render(<AchievementsPage />)

    expect(apiGet).toHaveBeenCalledWith('/users/7/achievements')
    const hydration = await screen.findByRole('progressbar', { name: 'Hydration Hero progress' })
    expect(hydration).toHaveAttribute('aria-valuenow', '3')
    expect(hydration).toHaveAttribute('aria-valuemax', '7')
    expect(screen.getByText('3 / 7 hydrated days')).toBeInTheDocument()
    expect(screen.getByText('1.4 / 2 kg')).toBeInTheDocument()
  })

  it('BR_ACH_06_earnedAchievementsStayEarnedWhateverTheCurrentValue', async () => {
    apiGet.mockResolvedValue({
      data: [achievement({ earned: true, currentValue: 0, earnedAt: '2026-10-01T10:00:00' })],
    })
    render(<AchievementsPage />)

    expect(await screen.findByText('Earned')).toBeInTheDocument()
    expect(screen.getByText('Earned — 1')).toBeInTheDocument()
    expect(screen.queryByRole('progressbar', { name: 'Hydration Hero progress' })).not.toBeInTheDocument()
  })
})
