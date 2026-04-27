import React, { createContext, useContext, useState, useCallback } from 'react'
import type { AuthResponse } from '../types'

interface AuthState {
  token: string | null
  email: string | null
  userId: number | null
  role: string | null
}

interface AuthContextValue extends AuthState {
  login: (response: AuthResponse) => void
  logout: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

function loadInitialState(): AuthState {
  try {
    const raw = localStorage.getItem('nc_user')
    if (raw) return JSON.parse(raw)
  } catch { /* ignore */ }
  return { token: null, email: null, userId: null, role: null }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>(loadInitialState)

  const login = useCallback((response: AuthResponse) => {
    const next: AuthState = {
      token: response.token,
      email: response.email,
      userId: response.userId,
      role: response.role,
    }
    localStorage.setItem('nc_token', response.token)
    localStorage.setItem('nc_user', JSON.stringify(next))
    setState(next)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('nc_token')
    localStorage.removeItem('nc_user')
    setState({ token: null, email: null, userId: null, role: null })
  }, [])

  return (
    <AuthContext.Provider
      value={{ ...state, login, logout, isAuthenticated: !!state.token }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
