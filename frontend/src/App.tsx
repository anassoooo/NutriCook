import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './contexts/AuthContext'
import Layout from './components/Layout'
import LandingPage from './pages/LandingPage'
import AuthPage from './pages/AuthPage'
import OnboardingPage from './pages/OnboardingPage'
import DashboardPage from './pages/DashboardPage'
import ProfilePage from './pages/ProfilePage'
import ProgressPage from './pages/ProgressPage'
import AchievementsPage from './pages/AchievementsPage'
import RestaurantsPage from './pages/RestaurantsPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <>{children}</> : <Navigate to="/auth" replace />
}

export default function App() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/auth" element={<AuthPage />} />

      {/* Onboarding — auth required, no navbar */}
      <Route path="/onboarding" element={
        <RequireAuth>
          <OnboardingPage />
        </RequireAuth>
      } />

      {/* Main app — auth required, with navbar */}
      <Route element={
        <RequireAuth>
          <Layout />
        </RequireAuth>
      }>
        <Route path="dashboard"    element={<DashboardPage />} />
        <Route path="profile"      element={<ProfilePage />} />
        <Route path="progress"     element={<ProgressPage />} />
        <Route path="achievements" element={<AchievementsPage />} />
        <Route path="restaurants"  element={<RestaurantsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
