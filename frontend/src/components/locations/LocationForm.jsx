import { useState, useEffect } from 'react'
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Stack, Box,
} from '@mui/material'

const EMPTY = { name: '', address: '', city: '', stateCode: '', zip: '' }

export default function LocationForm({ open, location, existingNames = [], onClose, onSubmit, loading }) {
  const [values, setValues] = useState(EMPTY)
  const [errors, setErrors] = useState({})

  useEffect(() => {
    setValues(location
      ? {
          name: location.name ?? '',
          address: location.address ?? '',
          city: location.city ?? '',
          stateCode: location.stateCode ?? '',
          zip: location.zip ?? '',
        }
      : EMPTY)
    setErrors({})
  }, [location, open])

  function handle(field) {
    return (e) => setValues(v => ({
      ...v,
      [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value,
    }))
  }

  function validate() {
    const e = {}
    const trimmed = values.name.trim()
    if (!trimmed) {
      e.name = 'Name is required'
    } else if (existingNames.some(n => n.toLowerCase() === trimmed.toLowerCase())) {
      e.name = 'A location with this name already exists in this organization'
    }
    setErrors(e)
    return Object.keys(e).length === 0
  }

  function submit() {
    if (validate()) onSubmit({ ...values, name: values.name.trim() })
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{location ? 'Edit Location' : 'New Location'}</DialogTitle>
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
            label="Address"
            value={values.address}
            onChange={handle('address')}
            fullWidth
          />
          <Box sx={{ display: 'grid', gridTemplateColumns: '3fr 1fr 1.5fr', gap: 2 }}>
            <TextField label="City" value={values.city} onChange={handle('city')} />
            <TextField
              label="State"
              value={values.stateCode}
              onChange={handle('stateCode')}
              inputProps={{ maxLength: 2, style: { textTransform: 'uppercase' } }}
            />
            <TextField label="ZIP" value={values.zip} onChange={handle('zip')} />
          </Box>
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
