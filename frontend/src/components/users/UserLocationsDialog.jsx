import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, Chip, IconButton, Tooltip, Typography, Box, Divider,
  Select, MenuItem, FormControl, InputLabel, CircularProgress, Alert,
  Table, TableHead, TableBody, TableRow, TableCell, TableContainer, Paper,
} from '@mui/material'
import DeleteIcon from '@mui/icons-material/Delete'
import { userApi } from '../../api/services'

const ROLES = ['READ', 'WRITE', 'ADMIN']

const ROLE_COLORS = { READ: 'default', WRITE: 'primary', ADMIN: 'warning' }

export default function UserLocationsDialog({ open, user, orgLocations = [], onClose }) {
  const queryClient = useQueryClient()
  const [newLocationId, setNewLocationId] = useState('')
  const [newRole, setNewRole] = useState('READ')
  const [errorMsg, setErrorMsg] = useState(null)

  const queryKey = ['user-locations', user?.id]

  const { data: assignmentsData, isLoading } = useQuery({
    queryKey,
    queryFn: () => userApi.getLocations(user.id, { size: 100 }),
    enabled: open && !!user,
  })

  const assignments = assignmentsData?.content ?? []

  const assignMutation = useMutation({
    mutationFn: ({ locationId, role }) => userApi.assignLocation(user.id, locationId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey })
      setNewLocationId('')
      setNewRole('READ')
      setErrorMsg(null)
    },
    onError: (err) => setErrorMsg(err.message),
  })

  const removeMutation = useMutation({
    mutationFn: (locationId) => userApi.removeLocation(user.id, locationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey })
      setErrorMsg(null)
    },
    onError: (err) => setErrorMsg(err.message),
  })

  function handleClose() {
    setNewLocationId('')
    setNewRole('READ')
    setErrorMsg(null)
    onClose()
  }

  const assignedIds = new Set(assignments.map(a => a.locationId))
  const availableLocations = orgLocations.filter(l => !assignedIds.has(l.id))

  if (!user) return null

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
      <DialogTitle>
        Manage Locations
        <Typography variant="body2" color="text.secondary">
          {user.firstName} {user.lastName}
        </Typography>
      </DialogTitle>

      <DialogContent>
        {errorMsg && (
          <Alert severity="error" onClose={() => setErrorMsg(null)} sx={{ mb: 2 }}>
            {errorMsg}
          </Alert>
        )}
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
            <CircularProgress size={28} />
          </Box>
        ) : (
          <>
            {/* Current assignments */}
            <Typography variant="subtitle2" gutterBottom>Assigned Locations</Typography>
            {assignments.length === 0 ? (
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                No locations assigned yet.
              </Typography>
            ) : (
              <TableContainer component={Paper} variant="outlined" sx={{ mb: 2 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow sx={{ '& th': { fontWeight: 600, backgroundColor: 'grey.50' } }}>
                      <TableCell>Location</TableCell>
                      <TableCell>City / State</TableCell>
                      <TableCell>Role</TableCell>
                      <TableCell align="right"></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {assignments.map((a) => (
                      <TableRow key={a.locationId}>
                        <TableCell sx={{ fontWeight: 500 }}>{a.locationName}</TableCell>
                        <TableCell sx={{ color: 'text.secondary' }}>
                          {[a.city, a.stateCode].filter(Boolean).join(', ') || '—'}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={a.role}
                            size="small"
                            color={ROLE_COLORS[a.role] ?? 'default'}
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell align="right">
                          <Tooltip title="Remove">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => removeMutation.mutate(a.locationId)}
                              disabled={removeMutation.isPending}
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}

            {/* Add new assignment */}
            {availableLocations.length > 0 && (
              <>
                <Divider sx={{ mb: 2 }} />
                <Typography variant="subtitle2" gutterBottom>Add Assignment</Typography>
                <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
                  <FormControl size="small" sx={{ flex: 2 }}>
                    <InputLabel>Location</InputLabel>
                    <Select
                      value={newLocationId}
                      label="Location"
                      onChange={(e) => setNewLocationId(e.target.value)}
                    >
                      {availableLocations.map(l => (
                        <MenuItem key={l.id} value={l.id}>{l.name}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <FormControl size="small" sx={{ flex: 1 }}>
                    <InputLabel>Role</InputLabel>
                    <Select value={newRole} label="Role" onChange={(e) => setNewRole(e.target.value)}>
                      {ROLES.map(r => <MenuItem key={r} value={r}>{r}</MenuItem>)}
                    </Select>
                  </FormControl>
                  <Button
                    variant="contained"
                    size="small"
                    sx={{ height: 40, mt: 0 }}
                    disabled={!newLocationId || assignMutation.isPending}
                    onClick={() => assignMutation.mutate({ locationId: newLocationId, role: newRole })}
                  >
                    Assign
                  </Button>
                </Box>
              </>
            )}

            {availableLocations.length === 0 && assignments.length > 0 && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                All available locations are assigned.
              </Typography>
            )}

            {orgLocations.length === 0 && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                No locations exist in this organization yet.
              </Typography>
            )}
          </>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={handleClose}>Close</Button>
      </DialogActions>
    </Dialog>
  )
}
