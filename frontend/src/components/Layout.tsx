import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { Wordmark } from './Brand'
import { ExitIcon, MedalIcon, PersonIcon, PinIcon, PlateIcon, ShelfIcon, TrendIcon } from './Icons'

const NAV = [
  { to: '/dashboard', label: 'Today', Icon: PlateIcon },
  { to: '/progress', label: 'Progress', Icon: TrendIcon },
  { to: '/achievements', label: 'Achievements', Icon: MedalIcon },
  { to: '/restaurants', label: 'Nearby', Icon: PinIcon },
  { to: '/profile', label: 'Profile', Icon: PersonIcon },
]

const ADMIN_NAV = [{ to: '/admin/foods', label: 'Catalog', Icon: ShelfIcon }]

/**
 * App shell: a left rail on wide screens, a bottom tab bar on phones. No page-change animation:
 * pages appear at once, and motion is kept for what the user does.
 */
export default function Layout() {
  const { email, role, logout } = useAuth()
  const navigate = useNavigate()
  const links = role === 'ADMIN' ? [...NAV, ...ADMIN_NAV] : NAV

  const signOut = () => { logout(); navigate('/') }

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[232px_1fr]">
      {/* Rail (desktop) */}
      <aside className="hidden lg:flex flex-col sticky top-0 h-screen border-r border-line px-5 py-6">
        <Wordmark to="/dashboard" />
        <nav aria-label="Main" className="mt-10 flex flex-col gap-1">
          {links.map(({ to, label, Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-ctl px-3 py-2.5 text-[15px] font-semibold transition-colors ${
                  isActive ? 'bg-ink text-chalk' : 'text-ink-soft hover:bg-wash hover:text-ink'
                }`
              }
            >
              <Icon />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto border-t border-line pt-4">
          <p className="truncate text-sm text-ink-soft" title={email ?? ''}>{email}</p>
          <button type="button" onClick={signOut} className="btn-ghost -ml-3 mt-1">
            <ExitIcon size={18} />
            Sign out
          </button>
        </div>
      </aside>

      {/* Top bar (phones and tablets) */}
      <header className="lg:hidden sticky top-0 z-30 flex items-center justify-between border-b border-line bg-chalk/95 px-4 py-3 backdrop-blur">
        <Wordmark to="/dashboard" />
        <button type="button" onClick={signOut} className="btn-ghost" aria-label="Sign out">
          <ExitIcon size={18} />
        </button>
      </header>

      <main className="min-w-0 px-4 pb-28 pt-6 sm:px-8 lg:px-12 lg:pb-28 lg:pt-10">
        <div className="mx-auto max-w-[1080px]">
          <Outlet />
        </div>
      </main>

      {/* Tab bar (phones and tablets) */}
      <nav aria-label="Main" className="lg:hidden fixed inset-x-0 bottom-0 z-30 border-t border-line bg-chalk/95 backdrop-blur pb-[env(safe-area-inset-bottom)]">
        <div className="mx-auto grid max-w-lg" style={{ gridTemplateColumns: `repeat(${links.length}, minmax(0, 1fr))` }}>
          {links.map(({ to, label, Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex flex-col items-center gap-1 py-2.5 text-[11px] font-semibold ${isActive ? 'text-ink' : 'text-ink-faint'}`
              }
            >
              {({ isActive }) => (
                <>
                  <span className={`grid h-8 w-12 place-items-center rounded-full ${isActive ? 'bg-ink text-chalk' : ''}`}>
                    <Icon size={19} />
                  </span>
                  {label}
                </>
              )}
            </NavLink>
          ))}
        </div>
      </nav>
    </div>
  )
}
