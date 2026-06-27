import { Typography, Box, Paper } from '@mui/material'
import PeopleIcon from '@mui/icons-material/People'

export default function UsersPage() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>Users</Typography>
      <Paper sx={{ p: 4, textAlign: 'center', mt: 2 }}>
        <PeopleIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
        <Typography variant="h6" color="text.secondary">
          Users CRUD — coming in P1-4
        </Typography>
      </Paper>
    </Box>
  )
}
