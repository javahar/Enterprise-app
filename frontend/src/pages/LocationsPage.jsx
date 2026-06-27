import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box, Typography, Button, Alert, Snackbar, CircularProgress, IconButton, Tooltip,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import AddIcon from '@mui/icons-material/Add'
import { organizationApi, locationApi, PAGE_SIZE } from '../api/services'
import LocationList from '../components/locations/LocationList'
import LocationForm from '../components/locations/LocationForm'

export default function LocationsPage() {
  const { orgId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [toast, setToast] = useState({ open: false, message: '', severity: 'success' })

  const {
    data: org,
    isLoading: orgLoading,
    isError: orgError,
  } = useQuery({
    queryKey: ['organization', orgId],
    queryFn: () => organizationApi.getById(orgId),
    enabled: !!orgId,
  })

  const {
    data: locData,
    isLoading: locLoading,
    isError: locError,
    error: locFetchError,
  } = useQuery({
    queryKey: ['locations', orgId, page],
    queryFn: () => locationApi.getByOrg(orgId, { page, size: PAGE_SIZE }),
    enabled: !!orgId,
  })

  const locations = locData?.content ?? []
  const totalElements = locData?.totalElements ?? 0

  const createMutation = useMutation({
    mutationFn: (data) => locationApi.create(orgId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['locations', orgId] })
      setPage(0)
      setFormOpen(false)
      showToast('Location created')
    },
    onError: (err) => showToast(err.message, 'error'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => locationApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['locations', orgId] })
      setFormOpen(false)
      setEditing(null)
      showToast('Location updated')
    },
    onError: (err) => showToast(err.message, 'error'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id) => locationApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['locations', orgId] })
      setPage(0)
      showToast('Location deleted')
    },
    onError: (err) => showToast(err.message, 'error'),
  })

  function showToast(message, severity = 'success') {
    setToast({ open: true, message, severity })
  }

  function handleEdit(loc) {
    setEditing(loc)
    setFormOpen(true)
  }

  function handleFormClose() {
    setFormOpen(false)
    setEditing(null)
  }

  function handleSubmit(values) {
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: values })
    } else {
      createMutation.mutate(values)
    }
  }

  const orgInactive = org && !org.isActive
  const isSaving = createMutation.isPending || updateMutation.isPending
  const isLoading = orgLoading || locLoading

  // Uniqueness check is page-scoped; backend returns 400 for cross-page duplicates
  const existingNames = locations
    .filter(l => !editing || l.id !== editing.id)
    .map(l => l.name)

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <Tooltip title="Back to Organizations">
          <IconButton onClick={() => navigate('/organizations')}>
            <ArrowBackIcon />
          </IconButton>
        </Tooltip>
        <Box sx={{ flexGrow: 1 }}>
          {org && (
            <Typography variant="body2" color="text.secondary" lineHeight={1}>
              {org.name}
            </Typography>
          )}
          <Typography variant="h4">Locations</Typography>
        </Box>
        <Tooltip title={!org ? '' : orgInactive ? 'Organization is inactive — cannot add locations' : ''}>
          <span>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setFormOpen(true)}
              disabled={!org || orgInactive || isLoading}
            >
              Add Location
            </Button>
          </span>
        </Tooltip>
      </Box>

      {orgInactive && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          This organization is inactive. Locations are shown read-only and new locations cannot be added.
        </Alert>
      )}

      {orgError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Could not load organization details.
        </Alert>
      )}

      {locError && (
        <Alert severity="error" sx={{ mb: 2 }}>{locFetchError.message}</Alert>
      )}

      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
          <CircularProgress />
        </Box>
      )}

      {!isLoading && !locError && (
        <LocationList
          locations={locations}
          onEdit={handleEdit}
          onDelete={(id) => deleteMutation.mutate(id)}
          deleteLoading={deleteMutation.isPending}
          readOnly={orgInactive}
          page={page}
          totalElements={totalElements}
          onPageChange={setPage}
        />
      )}

      <LocationForm
        open={formOpen}
        location={editing}
        existingNames={existingNames}
        onClose={handleFormClose}
        onSubmit={handleSubmit}
        loading={isSaving}
      />

      <Snackbar
        open={toast.open}
        autoHideDuration={3000}
        onClose={() => setToast(t => ({ ...t, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={toast.severity} onClose={() => setToast(t => ({ ...t, open: false }))}>
          {toast.message}
        </Alert>
      </Snackbar>
    </Box>
  )
}
