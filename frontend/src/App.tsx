import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './contexts/AuthContext'
import Layout from './components/Layout'
import AuthPage from './pages/AuthPage'
import DashboardPage from './pages/DashboardPage'
import ProfilePage from './pages/ProfilePage'
import DietPlanPage from './pages/DietPlanPage'
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
      <Route path="/auth" element={<AuthPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard"    element={<DashboardPage />} />
        <Route path="profile"      element={<ProfilePage />} />
        <Route path="plan"         element={<DietPlanPage />} />
        <Route path="progress"     element={<ProgressPage />} />
        <Route path="achievements" element={<AchievementsPage />} />
        <Route path="restaurants"  element={<RestaurantsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
