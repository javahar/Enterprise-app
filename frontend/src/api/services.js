import apiClient from './apiClient'

export const PAGE_SIZE = 20

// ─── Organizations ───────────────────────────────────────────────────────────

export const organizationApi = {
  getAll: (params = {}) => apiClient.get('/organizations', { params }).then(r => r.data),
  getById: (id) => apiClient.get(`/organizations/${id}`).then(r => r.data),
  create: (data) => apiClient.post('/organizations', data).then(r => r.data),
  update: (id, data) => apiClient.put(`/organizations/${id}`, data).then(r => r.data),
  delete: (id) => apiClient.delete(`/organizations/${id}`),
}

// ─── Locations ───────────────────────────────────────────────────────────────

export const locationApi = {
  getByOrg: (orgId, params = {}) => apiClient.get(`/organizations/${orgId}/locations`, { params }).then(r => r.data),
  getById: (id) => apiClient.get(`/locations/${id}`).then(r => r.data),
  create: (orgId, data) => apiClient.post(`/organizations/${orgId}/locations`, data).then(r => r.data),
  update: (id, data) => apiClient.put(`/locations/${id}`, data).then(r => r.data),
  delete: (id) => apiClient.delete(`/locations/${id}`),
}

// ─── Users ───────────────────────────────────────────────────────────────────

export const userApi = {
  getByOrg: (orgId, params = {}) => apiClient.get(`/organizations/${orgId}/users`, { params }).then(r => r.data),
  getById: (id) => apiClient.get(`/users/${id}`).then(r => r.data),
  create: (orgId, data) => apiClient.post(`/organizations/${orgId}/users`, data).then(r => r.data),
  update: (id, data) => apiClient.put(`/users/${id}`, data).then(r => r.data),
  delete: (id) => apiClient.delete(`/users/${id}`),
  getLocations: (userId, params = {}) => apiClient.get(`/users/${userId}/locations`, { params }).then(r => r.data),
  assignLocation: (userId, locationId, role) =>
    apiClient.post(`/users/${userId}/locations`, { locationId, role }).then(r => r.data),
  removeLocation: (userId, locationId) =>
    apiClient.delete(`/users/${userId}/locations/${locationId}`),
}
