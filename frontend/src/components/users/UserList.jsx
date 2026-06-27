import { useState } from 'react'
import {
  Table, TableHead, TableBody, TableRow, TableCell,
  TableContainer, Paper, IconButton, Chip, Tooltip, TablePagination,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button,
  Typography,
} from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import PersonOffIcon from '@mui/icons-material/PersonOff'
import LocationOnIcon from '@mui/icons-material/LocationOn'
import { PAGE_SIZE } from '../../api/services'

export default function UserList({ users, onEdit, onDeactivate, deactivateLoading, onManageLocations, page, totalElements, onPageChange }) {
  const [confirmUser, setConfirmUser] = useState(null)

  function handleDeactivateConfirm() {
    onDeactivate(confirmUser)
    setConfirmUser(null)
  }

  if (!users.length) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ mt: 4, textAlign: 'center' }}>
        No users yet. Click "Add User" to create one.
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
              <TableCell>Email</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.map((user) => (
              <TableRow key={user.id} hover sx={{ opacity: user.isActive ? 1 : 0.6 }}>
                <TableCell sx={{ fontWeight: 500 }}>
                  {user.firstName} {user.lastName}
                </TableCell>
                <TableCell sx={{ color: 'text.secondary' }}>{user.email}</TableCell>
                <TableCell>
                  <Chip
                    label={user.isActive ? 'Active' : 'Inactive'}
                    size="small"
                    color={user.isActive ? 'success' : 'default'}
                    variant="outlined"
                  />
                </TableCell>
                <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                  <Tooltip title={user.isActive ? 'Manage Locations' : 'User is inactive'}>
                    <span>
                      <IconButton
                        size="small"
                        color="primary"
                        aria-label="Manage Locations"
                        onClick={() => onManageLocations(user)}
                        disabled={!user.isActive}
                      >
                        <LocationOnIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Tooltip title="Edit">
                    <IconButton size="small" onClick={() => onEdit(user)}>
                      <EditIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  {user.isActive && (
                    <Tooltip title="Deactivate">
                      <IconButton size="small" color="error" onClick={() => setConfirmUser(user)}>
                        <PersonOffIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )}
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

      <Dialog open={!!confirmUser} onClose={() => setConfirmUser(null)}>
        <DialogTitle>Deactivate user?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            The user will be marked as inactive and will no longer appear in active listings.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmUser(null)}>Cancel</Button>
          <Button color="error" variant="contained" onClick={handleDeactivateConfirm} disabled={deactivateLoading}>
            {deactivateLoading ? 'Deactivating…' : 'Deactivate'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
