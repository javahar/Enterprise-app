import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Box, Drawer, AppBar, Toolbar, Typography, List,
  ListItem, ListItemButton, ListItemIcon, ListItemText,
  IconButton, Divider,
} from '@mui/material'
import BusinessIcon from '@mui/icons-material/Business'
import LocationOnIcon from '@mui/icons-material/LocationOn'
import PeopleIcon from '@mui/icons-material/People'
import MenuIcon from '@mui/icons-material/Menu'
import AccountTreeIcon from '@mui/icons-material/AccountTree'

const DRAWER_WIDTH = 240

const navItems = [
  { label: 'Organizations', icon: <BusinessIcon />, path: '/organizations' },
]

export default function Layout({ children }) {
  const [mobileOpen, setMobileOpen] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()

  const drawer = (
    <Box>
      <Toolbar>
        <Typography variant="h6" fontWeight={700} color="primary">
          Enterprise App
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {navItems.map(({ label, icon, path }) => (
          <ListItem key={path} disablePadding>
            <ListItemButton
              selected={location.pathname.startsWith(path)}
              onClick={() => navigate(path)}
              sx={{
                '&.Mui-selected': {
                  backgroundColor: 'primary.50',
                  borderRight: '3px solid',
                  borderColor: 'primary.main',
                },
              }}
            >
              <ListItemIcon sx={{ color: location.pathname.startsWith(path) ? 'primary.main' : 'inherit' }}>
                {icon}
              </ListItemIcon>
              <ListItemText primary={label} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
      <Divider />
      <List>
        <ListItem sx={{ px: 2, pt: 1 }}>
          <Typography variant="caption" color="text.secondary" fontWeight={600}>
            COMING SOON
          </Typography>
        </ListItem>
        {[
          { label: 'Hierarchy', icon: <AccountTreeIcon /> },
          { label: 'Locations', icon: <LocationOnIcon /> },
          { label: 'Users', icon: <PeopleIcon /> },
        ].map(({ label, icon }) => (
          <ListItem key={label} disablePadding>
            <ListItemButton disabled>
              <ListItemIcon>{icon}</ListItemIcon>
              <ListItemText primary={label} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        elevation={0}
        sx={{ width: { sm: `calc(100% - ${DRAWER_WIDTH}px)` }, ml: { sm: `${DRAWER_WIDTH}px` }, borderBottom: '1px solid', borderColor: 'divider' }}
      >
        <Toolbar>
          <IconButton color="inherit" edge="start" onClick={() => setMobileOpen(true)} sx={{ mr: 2, display: { sm: 'none' } }}>
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div">
            {navItems.find(n => location.pathname.startsWith(n.path))?.label ?? 'Enterprise App'}
          </Typography>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { sm: DRAWER_WIDTH }, flexShrink: { sm: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{ display: { xs: 'block', sm: 'none' }, '& .MuiDrawer-paper': { width: DRAWER_WIDTH } }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{ display: { xs: 'none', sm: 'block' }, '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' } }}
          open
        >
          {drawer}
        </Drawer>
      </Box>

      <Box component="main" sx={{ flexGrow: 1, p: 3, width: { sm: `calc(100% - ${DRAWER_WIDTH}px)` }, mt: 8 }}>
        {children}
      </Box>
    </Box>
  )
}
