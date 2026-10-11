import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, ERROR_MESSAGES } from '../lib/errors'

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }))
vi.mock('../lib/api', () => ({ default: { get: apiGet } }))

import RestaurantsPage from '../pages/RestaurantsPage'

describe('RestaurantsPage', () => {
  beforeEach(() => {
    apiGet.mockReset()
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: {
        getCurrentPosition: (ok: (p: { coords: { latitude: number; longitude: number } }) => void) =>
          ok({ coords: { latitude: 36.8065, longitude: 10.1815 } }),
      },
    })
  })

  it('saysTheMapIsUnavailableInsteadOfNothingNearby', async () => {
    const error = new ApiError({ code: 'MAP_UNAVAILABLE', status: 503 })
    // the test runner may invoke the mock without arguments: reject real requests only
    apiGet.mockImplementation((url?: string) => (url ? Promise.reject(error) : Promise.resolve({ data: [] })))
    render(<RestaurantsPage />)

    await userEvent.click(screen.getByRole('button', { name: /Find near me/ }))

    expect(apiGet).toHaveBeenCalledWith('/location/restaurants?lat=36.8065&lng=10.1815')
    expect(await screen.findByRole('alert')).toHaveTextContent(ERROR_MESSAGES.MAP_UNAVAILABLE)
    expect(screen.queryByText('No restaurants found')).not.toBeInTheDocument()
  })

  it('anEmptyAnswerReallyMeansNothingNearby', async () => {
    apiGet.mockResolvedValue({ data: [] })
    render(<RestaurantsPage />)

    await userEvent.click(screen.getByRole('button', { name: /Find near me/ }))

    expect(await screen.findByText('No restaurants found')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
