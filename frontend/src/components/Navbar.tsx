import { useState } from 'react'
import { NavLink, Link, useNavigate } from 'react-router-dom'
import Tooltip from '@mui/material/Tooltip'
import { useAuth } from '../contexts/AuthContext'

const NAV = [
  { to: '/dashboard',    label: 'Dashboard'    },
  { to: '/progress',     label: 'Progress'     },
  { to: '/achievements', label: 'Achievements' },
  { to: '/restaurants',  label: 'Near Me'      },
]

export default function Navbar() {
  const { email, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const initial = email?.charAt(0).toUpperCase() ?? 'U'

  const handleLogout = () => { logout(); navigate('/') }

  return (
    <header className="sticky top-0 z-50 px-4 md:px-8 pt-4 pb-2">

      {/* Floating pill — matches landing page navbar */}
      <nav className="liquid-glass rounded-2xl max-w-6xl mx-auto px-5 py-2.5 flex items-center justify-between">

        {/* Logo → landing page */}
        <Link to="/" className="flex items-center gap-2.5 shrink-0 select-none">
          <div style={{
            width: 32, height: 32, borderRadius: 8,
            background: 'linear-gradient(135deg,#16a34a,#0d9488)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 2px 8px rgba(22,163,74,.4)',
          }}>
            <span style={{ color: 'white', fontWeight: 900, fontSize: 13 }}>N</span>
          </div>
          <span className="font-black text-white text-lg tracking-tight" style={{ letterSpacing: '-0.02em' }}>
            NutriCook
          </span>
        </Link>

        {/* Desktop nav links */}
        <div className="hidden md:flex items-center gap-0.5">
          {NAV.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `px-3.5 py-2 rounded-xl text-sm font-semibold transition-all ${
                  isActive
                    ? 'text-white bg-white/15'
                    : 'text-white/65 hover:text-white hover:bg-white/10'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </div>

        {/* Right: user info + sign out */}
        <div className="hidden md:flex items-center gap-3">
          <span className="text-xs text-white/60 truncate max-w-[140px]">{email}</span>
          <Tooltip title="View profile" placement="bottom" arrow>
            <Link
              to="/profile"
              aria-label="User profile"
              className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-black transition-transform hover:scale-110"
              style={{ background: 'linear-gradient(135deg,#16a34a,#0d9488)' }}
            >
              {initial}
            </Link>
          </Tooltip>
          <button
            onClick={handleLogout}
            className="text-sm font-semibold px-4 py-1.5 rounded-lg text-white/65 hover:text-red-400 hover:bg-red-500/10 transition-all duration-200"
          >
            Sign out
          </button>
        </div>

        {/* Mobile hamburger */}
        <button
          onClick={() => setOpen(v => !v)}
          className="md:hidden p-2 rounded-xl text-white/65 hover:bg-white/10 transition-colors"
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
      </nav>

      {/* Mobile dropdown — also a floating pill */}
      {open && (
        <div className="md:hidden mt-2 max-w-6xl mx-auto liquid-glass rounded-2xl overflow-hidden animate-fade-in">
          <nav className="px-4 py-3 space-y-0.5">
            {NAV.map(({ to, label }) => (
              <NavLink
                key={to}
                to={to}
                onClick={() => setOpen(false)}
                className={({ isActive }) =>
                  `block px-4 py-2.5 rounded-xl text-sm font-semibold transition-colors ${
                    isActive ? 'text-white bg-white/15' : 'text-white/65 hover:text-white hover:bg-white/10'
                  }`
                }
              >
                {label}
              </NavLink>
            ))}
            <div className="pt-2 mt-2" style={{ borderTop: '1px solid rgba(255,255,255,0.08)' }}>
              <p className="px-4 py-1.5 text-xs text-white/55">{email}</p>
              <Link
                to="/profile"
                onClick={() => setOpen(false)}
                className="block px-4 py-2.5 text-sm font-semibold text-white/70 hover:text-white hover:bg-white/10 rounded-xl transition-colors"
              >
                My Profile
              </Link>
              <button
                onClick={() => { handleLogout(); setOpen(false) }}
                className="w-full text-left px-4 py-2.5 text-sm font-semibold text-red-400 hover:bg-red-500/10 rounded-xl transition-colors"
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
