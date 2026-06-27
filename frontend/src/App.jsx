import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/common/Layout'
import OrganizationsPage from './pages/OrganizationsPage'
import LocationsPage from './pages/LocationsPage'
import UsersPage from './pages/UsersPage'

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to="/organizations" replace />} />
        <Route path="/organizations" element={<OrganizationsPage />} />
        <Route path="/organizations/:orgId/locations" element={<LocationsPage />} />
        <Route path="/organizations/:orgId/users" element={<UsersPage />} />
      </Routes>
    </Layout>
  )
}
