import { useState } from 'react'
import {
  Table, TableHead, TableBody, TableRow, TableCell,
  TableContainer, Paper, IconButton, Tooltip, TablePagination,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button,
  Typography,
} from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { PAGE_SIZE } from '../../api/services'

export default function LocationList({ locations, onEdit, onDelete, deleteLoading, readOnly, page, totalElements, onPageChange }) {
  const [confirmId, setConfirmId] = useState(null)

  function handleDeleteConfirm() {
    onDelete(confirmId)
    setConfirmId(null)
  }

  if (!locations.length) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ mt: 4, textAlign: 'center' }}>
        No locations yet. Click "Add Location" to create one.
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
              <TableCell>Address</TableCell>
              <TableCell>City</TableCell>
              <TableCell>State</TableCell>
              <TableCell>ZIP</TableCell>
              {!readOnly && <TableCell align="right">Actions</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {locations.map((loc) => (
              <TableRow key={loc.id} hover>
                <TableCell sx={{ fontWeight: 500 }}>{loc.name}</TableCell>
                <TableCell sx={{ color: 'text.secondary' }}>{loc.address || '—'}</TableCell>
                <TableCell sx={{ color: 'text.secondary' }}>{loc.city || '—'}</TableCell>
                <TableCell sx={{ color: 'text.secondary' }}>{loc.stateCode || '—'}</TableCell>
                <TableCell sx={{ color: 'text.secondary' }}>{loc.zip || '—'}</TableCell>
                {!readOnly && (
                  <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                    <Tooltip title="Edit">
                      <IconButton size="small" onClick={() => onEdit(loc)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete">
                      <IconButton size="small" color="error" onClick={() => setConfirmId(loc.id)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                )}
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
        <DialogTitle>Delete location?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            This will permanently delete the location and cannot be undone.
          </DialogContentText>
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
