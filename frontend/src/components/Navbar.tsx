import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const NAV = [
  { to: '/dashboard',    label: 'Dashboard'    },
  { to: '/plan',         label: 'Diet Plan'    },
  { to: '/progress',     label: 'Progress'     },
  { to: '/achievements', label: 'Achievements' },
  { to: '/restaurants',  label: 'Near Me'      },
]

export default function Navbar() {
  const { email, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const initial = email?.charAt(0).toUpperCase() ?? 'U'

  const handleLogout = () => { logout(); navigate('/auth') }

  return (
    <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-slate-200">
      <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">

        {/* Logo */}
        <NavLink to="/dashboard" className="flex items-center gap-2.5 shrink-0 select-none">
          <div className="w-8 h-8 rounded-lg flex items-center justify-center shadow-sm"
               style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)' }}>
            <span className="text-white font-black text-sm">N</span>
          </div>
          <span className="font-black text-slate-900 text-lg tracking-tight">NutriCook</span>
        </NavLink>

        {/* Desktop nav */}
        <nav className="hidden md:flex items-center gap-0.5">
          {NAV.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `px-3.5 py-2 rounded-xl text-sm font-semibold transition-all ${
                  isActive
                    ? 'text-brand-700 bg-brand-50'
                    : 'text-slate-500 hover:text-slate-900 hover:bg-slate-100'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>

        {/* User area */}
        <div className="hidden md:flex items-center gap-3">
          <span className="text-xs text-slate-400 truncate max-w-[150px]">{email}</span>
          <button
            aria-label="User menu"
            className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-black shadow-sm"
            style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)' }}
          >
            {initial}
          </button>
          <button
            onClick={handleLogout}
            className="btn-ghost text-sm text-slate-500 hover:text-red-600 hover:bg-red-50"
          >
            Sign out
          </button>
        </div>

        {/* Mobile hamburger */}
        <button
          onClick={() => setOpen(v => !v)}
          className="md:hidden p-2 rounded-xl text-slate-500 hover:bg-slate-100 transition-colors"
          aria-label="Menu"
        >
          {open ? (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M6 18L18 6M6 6l12 12"/>
            </svg>
          ) : (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M4 6h16M4 12h16M4 18h16"/>
            </svg>
          )}
        </button>
      </div>

      {/* Mobile menu */}
      {open && (
        <div className="md:hidden border-t border-slate-100 bg-white shadow-lg animate-fade-in">
          <nav className="px-4 py-3 space-y-0.5">
            {NAV.map(({ to, label }) => (
              <NavLink
                key={to}
                to={to}
                onClick={() => setOpen(false)}
                className={({ isActive }) =>
                  `block px-4 py-2.5 rounded-xl text-sm font-semibold transition-colors ${
                    isActive ? 'text-brand-700 bg-brand-50' : 'text-slate-600 hover:bg-slate-50'
                  }`
                }
              >
                {label}
              </NavLink>
            ))}
            <div className="border-t border-slate-100 pt-2 mt-2">
              <p className="px-4 py-1.5 text-xs text-slate-400">{email}</p>
              <button
                onClick={() => { handleLogout(); setOpen(false) }}
                className="w-full text-left px-4 py-2.5 text-sm font-semibold text-red-600 hover:bg-red-50 rounded-xl"
              >
                Sign out
              </button>
            </div>
          </nav>
        </div>
      )}
    </header>
  )
}
