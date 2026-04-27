import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ThemeProvider, createTheme } from '@mui/material/styles'
import CssBaseline from '@mui/material/CssBaseline'
import App from './App'
import { AuthProvider } from './contexts/AuthContext'
import './index.css'

const theme = createTheme({
  palette: {
    primary:   { main: '#16a34a', light: '#22c55e', dark: '#15803d' },
    secondary: { main: '#0d9488', light: '#14b8a6', dark: '#0f766e' },
    error:     { main: '#ef4444' },
    warning:   { main: '#f59e0b' },
    info:      { main: '#3b82f6' },
    success:   { main: '#16a34a' },
    background:{ default: '#f8fafc', paper: '#ffffff' },
  },
  typography: {
    fontFamily: 'Inter, system-ui, sans-serif',
    button: { textTransform: 'none', fontWeight: 600 },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          background: '#0f172a',
          fontSize: '0.75rem',
          borderRadius: 8,
          padding: '6px 10px',
        },
      },
    },
    MuiLinearProgress: {
      styleOverrides: {
        root: { borderRadius: 9999, height: 6 },
      },
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      {/* CssBaseline scoped — don't reset body styles since Tailwind handles them */}
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </React.StrictMode>
)
