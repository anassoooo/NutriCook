import axios from 'axios'
import { shouldRedirectToLogin, toApiError } from './errors'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('nc_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Every failure reaches callers as an ApiError (code + safe message), never a raw AxiosError.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const hadToken = !!err.config?.headers?.Authorization
    if (shouldRedirectToLogin(err.response?.status, err.config?.url, hadToken)) {
      localStorage.removeItem('nc_token')
      localStorage.removeItem('nc_user')
      if (window.location.pathname !== '/auth') window.location.href = '/auth'
    }
    return Promise.reject(toApiError(err))
  },
)

export default api
