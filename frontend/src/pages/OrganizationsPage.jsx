import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Box, Typography, Button, Alert, Snackbar, CircularProgress } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import { organizationApi, PAGE_SIZE } from '../api/services'
import OrgList from '../components/organizations/OrgList'
import OrgForm from '../components/organizations/OrgForm'

export default function OrganizationsPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [toast, setToast] = useState({ open: false, message: '', severity: 'success' })

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['organizations', page],
    queryFn: () => organizationApi.getAll({ page, size: PAGE_SIZE }),
  })

  const orgs = data?.content ?? []
  const totalElements = data?.totalElements ?? 0

  const createMutation = useMutation({
    mutationFn: (data) => organizationApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] })
      setPage(0)
      setFormOpen(false)
      showToast('Organization created')
    },
    onError: (err) => showToast(err.message, 'error'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => organizationApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] })
      setFormOpen(false)
      setEditing(null)
      showToast('Organization updated')
    },
    onError: (err) => showToast(err.message, 'error'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id) => organizationApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] })
      setPage(0)
      showToast('Organization deleted')
    },
    onError: (err) => showToast(err.message, 'error'),
  })

  function showToast(message, severity = 'success') {
    setToast({ open: true, message, severity })
  }

  function handleEdit(org) {
    setEditing(org)
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

  const isSaving = createMutation.isPending || updateMutation.isPending

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Organizations</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setFormOpen(true)}>
          Add Organization
        </Button>
      </Box>

      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
          <CircularProgress />
        </Box>
      )}

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>{error.message}</Alert>
      )}

      {!isLoading && !isError && (
        <OrgList
          orgs={orgs}
          onEdit={handleEdit}
          onDelete={(id) => deleteMutation.mutate(id)}
          deleteLoading={deleteMutation.isPending}
          page={page}
          totalElements={totalElements}
          onPageChange={setPage}
        />
      )}

      <OrgForm
        open={formOpen}
        org={editing}
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
