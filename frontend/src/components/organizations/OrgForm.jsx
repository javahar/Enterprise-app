import { useState, useEffect } from 'react'
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, FormControlLabel, Checkbox, Stack,
} from '@mui/material'

const EMPTY = { name: '', description: '', isActive: true }

export default function OrgForm({ open, org, onClose, onSubmit, loading }) {
  const [values, setValues] = useState(EMPTY)
  const [errors, setErrors] = useState({})

  useEffect(() => {
    setValues(org ? { name: org.name, description: org.description ?? '', isActive: org.isActive ?? true } : EMPTY)
    setErrors({})
  }, [org, open])

  function handle(field) {
    return (e) => setValues(v => ({ ...v, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }))
  }

  function validate() {
    const e = {}
    if (!values.name.trim()) e.name = 'Name is required'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  function submit() {
    if (validate()) onSubmit(values)
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{org ? 'Edit Organization' : 'New Organization'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Name"
            value={values.name}
            onChange={handle('name')}
            error={!!errors.name}
            helperText={errors.name}
            fullWidth
            required
            autoFocus
          />
          <TextField
            label="Description"
            value={values.description}
            onChange={handle('description')}
            fullWidth
            multiline
            rows={2}
          />
          <FormControlLabel
            control={<Checkbox checked={values.isActive} onChange={handle('isActive')} />}
            label="Active"
          />
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
