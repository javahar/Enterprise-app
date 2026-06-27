import { useState, useEffect } from 'react'
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, FormControlLabel, Checkbox, Stack,
} from '@mui/material'

const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

const EMPTY = { firstName: '', lastName: '', email: '', isActive: true }

export default function UserForm({ open, user, onClose, onSubmit, loading }) {
  const [values, setValues] = useState(EMPTY)
  const [errors, setErrors] = useState({})

  useEffect(() => {
    setValues(user
      ? { firstName: user.firstName ?? '', lastName: user.lastName ?? '', email: user.email ?? '', isActive: user.isActive ?? true }
      : EMPTY)
    setErrors({})
  }, [user, open])

  function handle(field) {
    return (e) => setValues(v => ({
      ...v,
      [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value,
    }))
  }

  function validate() {
    const e = {}
    if (!values.firstName.trim()) e.firstName = 'First name is required'
    if (!values.lastName.trim()) e.lastName = 'Last name is required'
    if (!values.email.trim()) {
      e.email = 'Email is required'
    } else if (!EMAIL_REGEX.test(values.email.trim())) {
      e.email = 'Enter a valid email address'
    }
    setErrors(e)
    return Object.keys(e).length === 0
  }

  function submit() {
    if (validate()) onSubmit(values)
  }

  const isEdit = !!user

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{isEdit ? 'Edit User' : 'New User'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="First Name"
            value={values.firstName}
            onChange={handle('firstName')}
            error={!!errors.firstName}
            helperText={errors.firstName}
            fullWidth
            required
            autoFocus
          />
          <TextField
            label="Last Name"
            value={values.lastName}
            onChange={handle('lastName')}
            error={!!errors.lastName}
            helperText={errors.lastName}
            fullWidth
            required
          />
          <TextField
            label="Email"
            value={values.email}
            onChange={handle('email')}
            error={!!errors.email}
            helperText={errors.email}
            fullWidth
            required
            type="email"
            inputProps={{ autoComplete: 'off' }}
          />
          {isEdit && (
            <FormControlLabel
              control={<Checkbox checked={values.isActive} onChange={handle('isActive')} />}
              label="Active"
            />
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} disabled={loading}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={loading}>
          {loading ? 'Saving…' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
