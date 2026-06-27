import { Typography, Box, Paper } from '@mui/material'
import LocationOnIcon from '@mui/icons-material/LocationOn'

export default function LocationsPage() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>Locations</Typography>
      <Paper sx={{ p: 4, textAlign: 'center', mt: 2 }}>
        <LocationOnIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
        <Typography variant="h6" color="text.secondary">
          Locations CRUD — coming in P1-3
        </Typography>
      </Paper>
    </Box>
  )
}
