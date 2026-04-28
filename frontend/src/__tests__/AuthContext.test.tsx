import { render, screen, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, beforeEach } from 'vitest'
import { AuthProvider, useAuth } from '../contexts/AuthContext'
import type { AuthResponse } from '../types'

const MOCK_AUTH: AuthResponse = {
  userId: 42,
  token: 'mock-jwt-token',
  email: 'test@example.com',
  role: 'USER',
}

function TestConsumer() {
  const { isAuthenticated, email, userId, login, logout } = useAuth()
  return (
    <div>
      <span data-testid="auth">{String(isAuthenticated)}</span>
      <span data-testid="email">{email ?? 'none'}</span>
      <span data-testid="userId">{userId ?? 'none'}</span>
      <button onClick={() => login(MOCK_AUTH)}>Login</button>
      <button onClick={logout}>Logout</button>
    </div>
  )
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('starts unauthenticated when localStorage is empty', () => {
    render(<AuthProvider><TestConsumer /></AuthProvider>)
    expect(screen.getByTestId('auth').textContent).toBe('false')
    expect(screen.getByTestId('email').textContent).toBe('none')
  })

  it('becomes authenticated after login', async () => {
    render(<AuthProvider><TestConsumer /></AuthProvider>)
    await userEvent.click(screen.getByText('Login'))
    expect(screen.getByTestId('auth').textContent).toBe('true')
    expect(screen.getByTestId('email').textContent).toBe('test@example.com')
    expect(screen.getByTestId('userId').textContent).toBe('42')
  })

  it('persists token to localStorage on login', async () => {
    render(<AuthProvider><TestConsumer /></AuthProvider>)
    await userEvent.click(screen.getByText('Login'))
    expect(localStorage.getItem('nc_token')).toBe('mock-jwt-token')
  })

  it('becomes unauthenticated after logout', async () => {
    render(<AuthProvider><TestConsumer /></AuthProvider>)
    await userEvent.click(screen.getByText('Login'))
    await userEvent.click(screen.getByText('Logout'))
    expect(screen.getByTestId('auth').textContent).toBe('false')
    expect(screen.getByTestId('email').textContent).toBe('none')
  })

  it('clears localStorage on logout', async () => {
    render(<AuthProvider><TestConsumer /></AuthProvider>)
    await userEvent.click(screen.getByText('Login'))
    await userEvent.click(screen.getByText('Logout'))
    expect(localStorage.getItem('nc_token')).toBeNull()
    expect(localStorage.getItem('nc_user')).toBeNull()
  })

  it('hydrates state from localStorage on mount', () => {
    const state = { token: 'stored-token', email: 'stored@example.com', userId: 7, role: 'USER' }
    localStorage.setItem('nc_user', JSON.stringify(state))
    render(<AuthProvider><TestConsumer /></AuthProvider>)
    expect(screen.getByTestId('auth').textContent).toBe('true')
    expect(screen.getByTestId('email').textContent).toBe('stored@example.com')
  })
})
