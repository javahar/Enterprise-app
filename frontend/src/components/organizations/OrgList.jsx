import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Table, TableHead, TableBody, TableRow, TableCell,
  TableContainer, Paper, IconButton, Chip, Tooltip, TablePagination,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button,
  Typography,
} from '@mui/material'
import { PAGE_SIZE } from '../../api/services'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import LocationOnIcon from '@mui/icons-material/LocationOn'
import PeopleIcon from '@mui/icons-material/People'

export default function OrgList({ orgs, onEdit, onDelete, deleteLoading, page, totalElements, onPageChange }) {
  const navigate = useNavigate()
  const [confirmId, setConfirmId] = useState(null)

  function handleDeleteConfirm() {
    onDelete(confirmId)
    setConfirmId(null)
  }

  if (!orgs.length) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ mt: 4, textAlign: 'center' }}>
        No organizations yet. Click "Add Organization" to create one.
      </Typography>
    )
  }

  return (
    <>
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow sx={{ '& th': { fontWeight: 600, backgroundColor: 'grey.50' } }}>
              <TableCell>Name</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {orgs.map((org) => (
              <TableRow key={org.id} hover>
                <TableCell sx={{ fontWeight: 500 }}>{org.name}</TableCell>
                <TableCell sx={{ color: 'text.secondary', maxWidth: 300 }}>
                  {org.description || '—'}
                </TableCell>
                <TableCell>
                  <Chip
                    label={org.isActive ? 'Active' : 'Inactive'}
                    size="small"
                    color={org.isActive ? 'success' : 'default'}
                    variant="outlined"
                  />
                </TableCell>
                <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                  <Tooltip title={org.isActive ? 'Locations' : 'Organization is inactive'}>
                    <span>
                      <IconButton
                        size="small"
                        onClick={() => navigate(`/organizations/${org.id}/locations`)}
                        disabled={!org.isActive}
                      >
                        <LocationOnIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Tooltip title={org.isActive ? 'Users' : 'Organization is inactive'}>
                    <span>
                      <IconButton
                        size="small"
                        onClick={() => navigate(`/organizations/${org.id}/users`)}
                        disabled={!org.isActive}
                      >
                        <PeopleIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Tooltip title="Edit">
                    <IconButton size="small" onClick={() => onEdit(org)}>
                      <EditIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Delete">
                    <IconButton size="small" color="error" onClick={() => setConfirmId(org.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {totalElements > PAGE_SIZE && (
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          rowsPerPage={PAGE_SIZE}
          rowsPerPageOptions={[]}
          onPageChange={(_, newPage) => onPageChange(newPage)}
        />
      )}

      <Dialog open={!!confirmId} onClose={() => setConfirmId(null)}>
        <DialogTitle>Delete organization?</DialogTitle>
        <DialogContent>
          <DialogContentText>This action cannot be undone.</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmId(null)}>Cancel</Button>
          <Button color="error" variant="contained" onClick={handleDeleteConfirm} disabled={deleteLoading}>
            {deleteLoading ? 'Deleting…' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
