import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box, Typography, Button, Alert, Snackbar, CircularProgress, IconButton, Tooltip,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import AddIcon from '@mui/icons-material/Add'
import { organizationApi, userApi, locationApi, PAGE_SIZE } from '../api/services'
import UserList from '../components/users/UserList'
import UserForm from '../components/users/UserForm'
import UserLocationsDialog from '../components/users/UserLocationsDialog'

export default function UsersPage() {
  const { orgId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [locationsUser, setLocationsUser] = useState(null)
  const [toast, setToast] = useState({ open: false, message: '', severity: 'success' })

  const { data: org, isLoading: orgLoading, isError: orgError } = useQuery({
    queryKey: ['organization', orgId],
    queryFn: () => organizationApi.getById(orgId),
    enabled: !!orgId,
  })

  const { data: usersData, isLoading: usersLoading, isError: usersError, error: usersFetchError } = useQuery({
    queryKey: ['users', orgId, page],
    queryFn: () => userApi.getByOrg(orgId, { page, size: PAGE_SIZE }),
    enabled: !!orgId,
  })

  const users = usersData?.content ?? []
  const totalElements = usersData?.totalElements ?? 0

  // Fetch all locations for the assignment dialog dropdown — size:100 avoids modal pagination
  const { data: orgLocationsData } = useQuery({
    queryKey: ['locations', orgId, 'all'],
    queryFn: () => locationApi.getByOrg(orgId, { size: 100 }),
    enabled: !!orgId,
  })

  const orgLocations = orgLocationsData?.content ?? []

  const createMutation = useMutation({
    mutationFn: (data) => userApi.create(orgId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users', orgId] })
      setPage(0)
      setFormOpen(false)
      showToast('User created')
    },
    onError: (err) => showToast(err.message, 'error'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => userApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users', orgId] })
      setFormOpen(false)
      setEditing(null)
      showToast('User updated')
    },
    onError: (err) => showToast(err.message, 'error'),
  })

  const deactivateMutation = useMutation({
    mutationFn: (user) => userApi.update(user.id, {
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      isActive: false,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users', orgId] })
      showToast('User deactivated')
    },
    onError: (err) => showToast(err.message, 'error'),
  })

  function showToast(message, severity = 'success') {
    setToast({ open: true, message, severity })
  }

  function handleEdit(user) {
    setEditing(user)
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
  const isLoading = orgLoading || usersLoading

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
          <Typography variant="h4">Users</Typography>
        </Box>
        <Tooltip title={orgInactive ? 'Organization is inactive — cannot add users' : ''}>
          <span>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setFormOpen(true)}
              disabled={!org || orgInactive || isLoading}
            >
              Add User
            </Button>
          </span>
        </Tooltip>
      </Box>

      {orgInactive && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          This organization is inactive. Users are shown read-only and new users cannot be added.
        </Alert>
      )}

      {orgError && (
        <Alert severity="error" sx={{ mb: 2 }}>Could not load organization details.</Alert>
      )}

      {usersError && (
        <Alert severity="error" sx={{ mb: 2 }}>{usersFetchError.message}</Alert>
      )}

      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
          <CircularProgress />
        </Box>
      )}

      {!isLoading && !usersError && (
        <UserList
          users={users}
          onEdit={handleEdit}
          onDeactivate={(user) => deactivateMutation.mutate(user)}
          deactivateLoading={deactivateMutation.isPending}
          onManageLocations={(user) => setLocationsUser(user)}
          page={page}
          totalElements={totalElements}
          onPageChange={setPage}
        />
      )}

      <UserForm
        open={formOpen}
        user={editing}
        onClose={handleFormClose}
        onSubmit={handleSubmit}
        loading={isSaving}
      />

      <UserLocationsDialog
        open={!!locationsUser}
        user={locationsUser}
        orgLocations={orgLocations}
        onClose={() => setLocationsUser(null)}
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
