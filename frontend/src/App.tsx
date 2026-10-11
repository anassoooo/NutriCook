import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './contexts/AuthContext'
import Layout from './components/Layout'
import LandingPage from './pages/LandingPage'
import AuthPage from './pages/AuthPage'

// Each signed-in page loads with its own code (the charts library only with Progress)
const OnboardingPage = lazy(() => import('./pages/OnboardingPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const ProfilePage = lazy(() => import('./pages/ProfilePage'))
const ProgressPage = lazy(() => import('./pages/ProgressPage'))
const AchievementsPage = lazy(() => import('./pages/AchievementsPage'))
const RestaurantsPage = lazy(() => import('./pages/RestaurantsPage'))
const FoodsPage = lazy(() => import('./pages/admin/FoodsPage'))

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <>{children}</> : <Navigate to="/auth" replace />
}

/** The server enforces the role too (BR-CAT-05); this only keeps users off a page they can't use. */
function RequireAdmin({ children }: { children: React.ReactNode }) {
  const { role } = useAuth()
  return role === 'ADMIN' ? <>{children}</> : <Navigate to="/dashboard" replace />
}

export default function App() {
  return (
    <Suspense
      fallback={
        <p className="sr-only" role="status">
          Loading…
        </p>
      }
    >
      <Routes>
        {/* Public */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/auth" element={<AuthPage />} />
        {/* the old marketing pages are now sections of the landing page */}
        <Route path="/features" element={<Navigate to="/#safe" replace />} />
        <Route path="/how-it-works" element={<Navigate to="/#how" replace />} />

        {/* Onboarding — auth required, no navbar */}
        <Route
          path="/onboarding"
          element={
            <RequireAuth>
              <OnboardingPage />
            </RequireAuth>
          }
        />

        {/* Main app — auth required, with navbar */}
        <Route
          element={
            <RequireAuth>
              <Layout />
            </RequireAuth>
          }
        >
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="progress" element={<ProgressPage />} />
          <Route path="achievements" element={<AchievementsPage />} />
          <Route path="restaurants" element={<RestaurantsPage />} />
          <Route
            path="admin/foods"
            element={
              <RequireAdmin>
                <FoodsPage />
              </RequireAdmin>
            }
          />
        </Route>

        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  )
}
