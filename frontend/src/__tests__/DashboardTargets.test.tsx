import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { DietPlan, UserProfile } from '../types'

// Each macro line carries its "value / max" text in data-tooltip, which is what we assert on
vi.mock('../components/ChatPanel', () => ({ default: () => null }))
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => ({ userId: 7 }) }))

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }))
vi.mock('../lib/api', () => ({ default: { get: apiGet, post: vi.fn() } }))

import DashboardPage from '../pages/DashboardPage'

const baseProfile: UserProfile = {
  id: 7,
  firstName: 'Sami',
  lastName: null,
  weightKg: 80,
  heightCm: 180,
  dateOfBirth: '1996-01-01',
  gender: 'MALE',
  activityLevel: 'MODERATELY_ACTIVE',
  healthGoal: 'MAINTAIN_WEIGHT',
  timezone: 'Africa/Tunis',
  healthConditions: [],
  dietaryRestrictions: [],
  targets: {
    kcal: 2760,
    proteinG: 96,
    carbsG: 387,
    fatG: 92,
    fiberG: 39,
    bmr: 1780,
    tdee: 2759,
    floorApplied: false,
    slots: [],
  },
  targetsError: null,
  disclaimer: null,
}

const plan = {
  id: 1,
  title: 'Plan',
  totalCalories: 1950,
  totalProteinG: 80,
  totalCarbsG: 200,
  totalFatG: 70,
  totalSugarG: 45,
  validForDate: '2026-10-10',
  generatedAt: '2026-10-10T08:00:00',
  status: 'ACTIVE',
  aiExplanation: null,
  meals: [],
} as unknown as DietPlan

function mockApi(profile: UserProfile, plans: DietPlan[] = [plan]) {
  // Default param: the test runner can invoke the mock without arguments; the app always passes a URL
  apiGet.mockImplementation((url: string = '') => {
    if (url.endsWith('/profile')) return Promise.resolve({ data: profile })
    if (url.endsWith('/diet-plans')) return Promise.resolve({ data: plans })
    return Promise.resolve({ data: [] })
  })
}

function renderDashboard() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  )
}

describe('Dashboard targets', () => {
  beforeEach(() => apiGet.mockReset())

  it('BR_PRF_08_calorieBarAndMacroRingsUseTheApiTargets', async () => {
    mockApi(baseProfile)
    renderDashboard()

    expect(await screen.findByText(/\/ 2,760 kcal/)).toBeInTheDocument()
    expect(document.querySelector('[data-tooltip="80 g / 96 g (83%)"]')).not.toBeNull()
    expect(document.querySelector('[data-tooltip^="200 g / 387 g"]')).not.toBeNull()
    expect(document.querySelector('[data-tooltip^="70 g / 92 g"]')).not.toBeNull()
  })

  it('BR_PRF_08_noHardCodedTargetsWhenTheApiHasNone', async () => {
    mockApi({
      ...baseProfile,
      targets: null,
      targetsError: {
        code: 'PROFILE_INCOMPLETE',
        message: 'Please complete your profile.',
        details: { missingFields: ['dateOfBirth', 'weightKg'] },
      },
    })
    renderDashboard()

    expect(await screen.findByText(/Add your date of birth, weight so we can calculate/)).toBeInTheDocument()
    await waitFor(() => expect(apiGet).toHaveBeenCalled())
    expect(screen.queryByText(/2,200|2,500/)).toBeNull()
    expect(document.querySelector('[data-tooltip]')).toBeNull()
  })

  it('BR_PRF_02_ineligibleUsersSeeTheServerExplanation', async () => {
    mockApi({
      ...baseProfile,
      targets: null,
      targetsError: {
        code: 'PROFILE_INELIGIBLE',
        message: 'Personalized targets are available for adults aged 18 to 100.',
        details: { minAge: 18, maxAge: 100 },
      },
    })
    renderDashboard()

    expect(await screen.findByText('Plans are for adults')).toBeInTheDocument()
    expect(screen.getByText(/adults aged 18 to 100/)).toBeInTheDocument()
  })

  it('BR_MAC_07_totalSugarsShownWithoutAGenericLimit', async () => {
    mockApi(baseProfile)
    renderDashboard()

    expect(await screen.findByText(/Total sugars \(incl\. natural\)/)).toBeInTheDocument()
    expect(document.querySelector('[data-tooltip*="/ 50 g"]')).toBeNull()
    expect(screen.queryByText(/added sugar/i)).toBeNull()
  })
})
