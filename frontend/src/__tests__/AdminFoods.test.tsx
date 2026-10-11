import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../lib/errors'
import type { CatalogFood } from '../types'

const { apiGet, apiPost, apiPut, apiDelete, auth } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiDelete: vi.fn(),
  auth: { role: 'ADMIN' as string | null, isAuthenticated: true },
}))
vi.mock('../lib/api', () => ({
  default: { get: apiGet, post: apiPost, put: apiPut, delete: apiDelete },
}))
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => auth }))
// the rest of the app is not under test here
vi.mock('../components/Layout', async () => {
  const { Outlet } = await import('react-router-dom')
  return { default: () => <Outlet /> }
})

import App from '../App'

const food = (over: Partial<CatalogFood>): CatalogFood => ({
  id: 1,
  name: 'Chicken Breast',
  category: 'meat',
  role: 'PROTEIN',
  state: 'COOKED',
  caloriesPer100g: 165,
  proteinPer100g: 31,
  carbsPer100g: 0,
  fatPer100g: 3.6,
  sugarPer100g: 0,
  fiberPer100g: 0,
  sodiumMgPer100g: 74,
  portionMinG: 75,
  portionMaxG: 250,
  portionStepG: 5,
  defaultPortionG: 150,
  allergens: [],
  dietFlags: [],
  suitableSlots: ['LUNCH', 'DINNER'],
  meat: true,
  source: 'USDA SR Legacy',
  origin: 'SEED',
  active: true,
  ...over,
})

const page = (items: CatalogFood[]) => ({
  data: { items, page: 0, size: 20, totalItems: items.length, totalPages: 1 },
})

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('Admin food catalog', () => {
  beforeEach(() => {
    apiGet.mockReset()
    apiPost.mockReset()
    apiPut.mockReset()
    apiDelete.mockReset()
    auth.role = 'ADMIN'
  })

  it('BR_CAT_05_nonAdminsAreSentAway', async () => {
    auth.role = 'USER'
    apiGet.mockResolvedValue({ data: [] })
    renderAt('/admin/foods')
    expect(screen.queryByRole('heading', { name: 'Food catalog' })).not.toBeInTheDocument()
    expect(apiGet).not.toHaveBeenCalledWith(expect.stringContaining('/admin/foods'))
  })

  it('BR_CAT_05_seededFoodsCanOnlyBeDeactivated', async () => {
    apiGet.mockResolvedValue(page([food({}), food({ id: 2, name: 'Seitan', origin: 'ADMIN', meat: false })]))
    apiDelete.mockResolvedValue({ data: {} })
    renderAt('/admin/foods')

    const chicken = (await screen.findByText('Chicken Breast')).closest('tr')!
    expect(within(chicken).queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
    const seitan = screen.getByText('Seitan').closest('tr')!
    expect(within(seitan).getByRole('button', { name: 'Edit' })).toBeInTheDocument()

    await userEvent.click(within(chicken).getByRole('button', { name: 'Deactivate' }))
    expect(apiDelete).toHaveBeenCalledWith('/admin/foods/1')
  })

  it('BR_CAT_02_listsWhatTheQualityGateRejected', async () => {
    apiGet.mockResolvedValue(page([]))
    apiPost.mockImplementation((url?: string) =>
      url
        ? Promise.reject(
            new ApiError({
              code: 'VALIDATION_FAILED',
              status: 400,
              detail: 'This food fails the catalog quality checks.',
              fieldErrors: [{ field: 'nutrients', message: 'energy 900 kcal does not match the macros (372 kcal)' }],
            }),
          )
        : Promise.resolve({ data: {} }),
    )
    renderAt('/admin/foods')

    await userEvent.click(await screen.findByRole('button', { name: 'New food' }))
    const form = screen.getByRole('form', { name: 'New food' })
    await userEvent.type(within(form).getByLabelText('Name'), 'Seitan')
    await userEvent.type(within(form).getByLabelText('Source'), 'USDA')
    await userEvent.click(within(form).getByLabelText('Gluten'))
    await userEvent.click(within(form).getByLabelText('Lunch'))
    await userEvent.click(within(form).getByRole('button', { name: 'Save food' }))

    expect(apiPost).toHaveBeenCalledWith(
      '/admin/foods',
      expect.objectContaining({ name: 'Seitan', allergens: ['GLUTEN'], suitableSlots: ['LUNCH'] }),
    )
    const alert = await within(form).findByRole('alert')
    expect(alert).toHaveTextContent('nutrients: energy 900 kcal does not match the macros')
  }, 30_000) // ~1 s alone, but typing into a large form is slow when the whole suite runs in parallel

  it('searchesByNameRoleAndStatus', async () => {
    apiGet.mockResolvedValue(page([]))
    renderAt('/admin/foods')
    await screen.findByRole('heading', { name: 'Food catalog' })

    await userEvent.selectOptions(screen.getByLabelText('Role'), 'CARB')
    await userEvent.selectOptions(screen.getByLabelText('Status'), 'false')

    expect(apiGet).toHaveBeenLastCalledWith('/admin/foods?page=0&size=20&role=CARB&active=false')
  })
})
