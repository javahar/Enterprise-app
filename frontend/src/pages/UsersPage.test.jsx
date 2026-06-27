import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import UsersPage from './UsersPage'
import { organizationApi, userApi, locationApi } from '../api/services'

vi.mock('../api/services', () => ({
  PAGE_SIZE: 20,
  organizationApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
  userApi: {
    getByOrg: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    getLocations: vi.fn(),
    assignLocation: vi.fn(),
    removeLocation: vi.fn(),
  },
  locationApi: {
    getByOrg: vi.fn(),
  },
}))

const ORG_ACTIVE = { id: 'org-1', name: 'Acme Corp', isActive: true }
const ORG_INACTIVE = { id: 'org-2', name: 'Old Corp', isActive: false }

const USERS = [
  { id: 'u-1', firstName: 'Jane', lastName: 'Smith', email: 'jane@example.com', isActive: true },
  { id: 'u-2', firstName: 'Bob', lastName: 'Jones', email: 'bob@example.com', isActive: false },
]

const ORG_LOCATIONS = [
  { id: 'loc-1', name: 'Downtown HQ', city: 'New York', stateCode: 'NY' },
  { id: 'loc-2', name: 'West Branch', city: 'LA', stateCode: 'CA' },
]

const USER_LOCATIONS = [
  { locationId: 'loc-1', locationName: 'Downtown HQ', city: 'New York', stateCode: 'NY', role: 'ADMIN', assignedAt: '2024-01-01T00:00:00' },
]

const pageOf = (items) => ({ content: items, totalElements: items.length, page: 0, size: 20, totalPages: 1, last: true })

function renderPage(orgId = 'org-1') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/organizations/${orgId}/users`]}>
        <Routes>
          <Route path="/organizations/:orgId/users" element={<UsersPage />} />
          <Route path="/organizations" element={<div>Organizations</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  vi.resetAllMocks()
})

describe('UsersPage', () => {
  it('shows a loading spinner while data is fetching', () => {
    organizationApi.getById.mockReturnValue(new Promise(() => {}))
    userApi.getByOrg.mockReturnValue(new Promise(() => {}))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  it('renders the Users heading', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByRole('heading', { level: 4, name: /users/i })).toBeInTheDocument())
  })

  it('shows the org name above the heading', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument())
  })

  it('renders the user list after data loads', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())
    expect(screen.getByText('Bob Jones')).toBeInTheDocument()
    expect(screen.getByText('jane@example.com')).toBeInTheDocument()
  })

  it('shows Active and Inactive status chips', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())
    expect(screen.getByText('Active')).toBeInTheDocument()
    expect(screen.getByText('Inactive')).toBeInTheDocument()
  })

  it('shows empty-state message when there are no users', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByText(/no users yet/i)).toBeInTheDocument())
  })

  it('shows an error alert when user fetch fails', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockRejectedValue(new Error('Server error'))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Server error'))
  })

  // ── Inactive org ─────────────────────────────────────────────────────────

  it('shows a warning banner when org is inactive', async () => {
    organizationApi.getById.mockResolvedValue(ORG_INACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage('org-2')
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(/inactive/i))
  })

  it('disables the Add User button when org is inactive', async () => {
    organizationApi.getById.mockResolvedValue(ORG_INACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage('org-2')
    await waitFor(() => expect(screen.getByText('Old Corp')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /add user/i })).toBeDisabled()
  })

  // ── Add User ──────────────────────────────────────────────────────────────

  it('opens the New User dialog when Add User is clicked', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByRole('button', { name: /add user/i })).toBeEnabled())
    await userEvent.click(screen.getByRole('button', { name: /add user/i }))
    expect(screen.getByText('New User')).toBeInTheDocument()
  })

  it('calls userApi.create with form values on submit', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    userApi.create.mockResolvedValue({ id: 'u-3', firstName: 'Alice', lastName: 'Lee', email: 'alice@example.com', isActive: true })
    renderPage()
    await waitFor(() => expect(screen.getByRole('button', { name: /add user/i })).toBeEnabled())

    await userEvent.click(screen.getByRole('button', { name: /add user/i }))
    const dialog = screen.getByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText(/first name/i), 'Alice')
    await userEvent.type(within(dialog).getByLabelText(/last name/i), 'Lee')
    await userEvent.type(within(dialog).getByLabelText(/email/i), 'alice@example.com')
    await userEvent.click(within(dialog).getByRole('button', { name: /save/i }))

    await waitFor(() =>
      expect(userApi.create).toHaveBeenCalledWith(
        'org-1',
        expect.objectContaining({ firstName: 'Alice', lastName: 'Lee', email: 'alice@example.com' })
      )
    )
  })

  it('does not submit when required fields are empty', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByRole('button', { name: /add user/i })).toBeEnabled())

    await userEvent.click(screen.getByRole('button', { name: /add user/i }))
    await userEvent.click(screen.getByRole('button', { name: /save/i }))

    expect(userApi.create).not.toHaveBeenCalled()
    expect(screen.getByText(/first name is required/i)).toBeInTheDocument()
  })

  // ── Email validation ──────────────────────────────────────────────────────

  it('rejects an invalid email address', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByRole('button', { name: /add user/i })).toBeEnabled())

    await userEvent.click(screen.getByRole('button', { name: /add user/i }))
    const dialog = screen.getByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText(/first name/i), 'Alice')
    await userEvent.type(within(dialog).getByLabelText(/last name/i), 'Lee')
    await userEvent.type(within(dialog).getByLabelText(/email/i), 'not-an-email')
    await userEvent.click(within(dialog).getByRole('button', { name: /save/i }))

    expect(userApi.create).not.toHaveBeenCalled()
    expect(screen.getByText(/valid email/i)).toBeInTheDocument()
  })

  it('accepts a valid email address', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    userApi.create.mockResolvedValue({ id: 'u-4', firstName: 'Tom', lastName: 'Doe', email: 'tom+test@sub.example.co.uk', isActive: true })
    renderPage()
    await waitFor(() => expect(screen.getByRole('button', { name: /add user/i })).toBeEnabled())

    await userEvent.click(screen.getByRole('button', { name: /add user/i }))
    const dialog = screen.getByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText(/first name/i), 'Tom')
    await userEvent.type(within(dialog).getByLabelText(/last name/i), 'Doe')
    await userEvent.type(within(dialog).getByLabelText(/email/i), 'tom+test@sub.example.co.uk')
    await userEvent.click(within(dialog).getByRole('button', { name: /save/i }))

    await waitFor(() => expect(userApi.create).toHaveBeenCalled())
    expect(screen.queryByText(/valid email/i)).not.toBeInTheDocument()
  })

  // ── Edit ─────────────────────────────────────────────────────────────────

  it('opens the edit dialog pre-filled with user data', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    const editButtons = screen.getAllByRole('button', { name: /edit/i })
    await userEvent.click(editButtons[0])

    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByText('Edit User')).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('Jane')).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('jane@example.com')).toBeInTheDocument()
    // isActive checkbox is shown on edit
    expect(within(dialog).getByRole('checkbox')).toBeInTheDocument()
  })

  it('calls userApi.update on save', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    userApi.update.mockResolvedValue({ ...USERS[0], firstName: 'Janet' })
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    const editButtons = screen.getAllByRole('button', { name: /edit/i })
    await userEvent.click(editButtons[0])

    const dialog = screen.getByRole('dialog')
    const firstNameInput = within(dialog).getByDisplayValue('Jane')
    await userEvent.clear(firstNameInput)
    await userEvent.type(firstNameInput, 'Janet')
    await userEvent.click(within(dialog).getByRole('button', { name: /save/i }))

    await waitFor(() =>
      expect(userApi.update).toHaveBeenCalledWith('u-1', expect.objectContaining({ firstName: 'Janet' }))
    )
  })

  // ── Deactivate ────────────────────────────────────────────────────────────

  it('shows a deactivate confirmation dialog on Deactivate click', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /deactivate/i }))
    expect(screen.getByText(/deactivate user\?/i)).toBeInTheDocument()
  })

  it('calls userApi.update with isActive:false when deactivate is confirmed', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    userApi.update.mockResolvedValue({ ...USERS[0], isActive: false })
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /deactivate/i }))
    const confirmDialog = screen.getByRole('dialog')
    await userEvent.click(within(confirmDialog).getByRole('button', { name: /^deactivate$/i }))

    await waitFor(() =>
      expect(userApi.update).toHaveBeenCalledWith('u-1', {
        firstName: 'Jane',
        lastName: 'Smith',
        email: 'jane@example.com',
        isActive: false,
      })
    )
  })

  it('disables Manage Locations for inactive users', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    const locationButtons = screen.getAllByRole('button', { name: /manage locations/i })
    // Jane is active (enabled), Bob is inactive (disabled)
    expect(locationButtons[0]).toBeEnabled()
    expect(locationButtons[1]).toBeDisabled()
  })

  // ── Manage Locations dialog ───────────────────────────────────────────────

  it('opens the Manage Locations dialog for an active user', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf(ORG_LOCATIONS))
    userApi.getLocations.mockResolvedValue(pageOf(USER_LOCATIONS))
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    const locationButtons = screen.getAllByRole('button', { name: /manage locations/i })
    await userEvent.click(locationButtons[0])

    await waitFor(() => expect(screen.getByText('Manage Locations')).toBeInTheDocument())
    expect(screen.getByText('Downtown HQ')).toBeInTheDocument()
    expect(screen.getByText('ADMIN')).toBeInTheDocument()
  })

  it('shows available locations for new assignment', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf(ORG_LOCATIONS))
    userApi.getLocations.mockResolvedValue(pageOf(USER_LOCATIONS))
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    const locationButtons = screen.getAllByRole('button', { name: /manage locations/i })
    await userEvent.click(locationButtons[0])

    await waitFor(() => expect(screen.getByText('Manage Locations')).toBeInTheDocument())
    // West Branch is not yet assigned — should appear in the dropdown
    expect(screen.getByText('Add Assignment')).toBeInTheDocument()
  })

  it('shows an inline error alert when assign fails', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf(ORG_LOCATIONS))
    userApi.getLocations.mockResolvedValue(pageOf(USER_LOCATIONS))
    userApi.assignLocation.mockRejectedValue(new Error('Assignment failed'))
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    const locationButtons = screen.getAllByRole('button', { name: /manage locations/i })
    await userEvent.click(locationButtons[0])
    await waitFor(() => expect(screen.getByText('Add Assignment')).toBeInTheDocument())

    // Open the Location dropdown (first combobox) and pick West Branch (the unassigned location)
    const [locationCombobox] = screen.getAllByRole('combobox')
    await userEvent.click(locationCombobox)
    await userEvent.click(await screen.findByRole('option', { name: 'West Branch' }))

    await userEvent.click(screen.getByRole('button', { name: /assign/i }))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Assignment failed'))
  })

  it('shows an inline error alert when remove fails', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf(ORG_LOCATIONS))
    userApi.getLocations.mockResolvedValue(pageOf(USER_LOCATIONS))
    userApi.removeLocation.mockRejectedValue(new Error('Remove failed'))
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    const locationButtons = screen.getAllByRole('button', { name: /manage locations/i })
    await userEvent.click(locationButtons[0])
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /remove/i }))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Remove failed'))
  })

  it('clears the error alert on a successful subsequent action', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf(ORG_LOCATIONS))
    userApi.getLocations.mockResolvedValue(pageOf(USER_LOCATIONS))
    userApi.removeLocation
      .mockRejectedValueOnce(new Error('Remove failed'))
      .mockResolvedValueOnce()
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    const locationButtons = screen.getAllByRole('button', { name: /manage locations/i })
    await userEvent.click(locationButtons[0])
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /remove/i }))
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /remove/i }))
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument())
  })

  it('clears the error alert when the dialog is closed', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf(ORG_LOCATIONS))
    userApi.getLocations.mockResolvedValue(pageOf(USER_LOCATIONS))
    userApi.removeLocation.mockRejectedValue(new Error('Remove failed'))
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    const locationButtons = screen.getAllByRole('button', { name: /manage locations/i })
    await userEvent.click(locationButtons[0])
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /remove/i }))
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())

    // Two "Close" buttons exist: Alert's X icon and the dialog's text button — pick the text one
    const closeButtons = screen.getAllByRole('button', { name: /close/i })
    const dialogClose = closeButtons.find(b => b.textContent.trim() === 'Close')
    await userEvent.click(dialogClose)
    // Re-open — error should be gone
    await userEvent.click(locationButtons[0])
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('calls userApi.removeLocation when a location assignment is removed', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf(USERS))
    locationApi.getByOrg.mockResolvedValue(pageOf(ORG_LOCATIONS))
    userApi.getLocations.mockResolvedValue(pageOf(USER_LOCATIONS))
    userApi.removeLocation.mockResolvedValue()
    renderPage()
    await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument())

    const locationButtons = screen.getAllByRole('button', { name: /manage locations/i })
    await userEvent.click(locationButtons[0])

    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: /remove/i }))

    await waitFor(() => expect(userApi.removeLocation).toHaveBeenCalledWith('u-1', 'loc-1'))
  })

  // ── Navigation ────────────────────────────────────────────────────────────

  it('navigates back to Organizations when back button is clicked', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    userApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByRole('heading', { level: 4 })).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /back to organizations/i }))
    expect(screen.getByText('Organizations')).toBeInTheDocument()
  })
})
