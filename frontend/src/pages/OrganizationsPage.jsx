import { Typography, Box, Paper } from '@mui/material'
import BusinessIcon from '@mui/icons-material/Business'

export default function OrganizationsPage() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>Organizations</Typography>
      <Paper sx={{ p: 4, textAlign: 'center', mt: 2 }}>
        <BusinessIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
        <Typography variant="h6" color="text.secondary">
          Organization CRUD — coming in P1-2
        </Typography>
        <Typography variant="body2" color="text.disabled" mt={1}>
          Scaffold is wired up. Run the backend and this page will be implemented next.
        </Typography>
      </Paper>
    </Box>
  )
}
