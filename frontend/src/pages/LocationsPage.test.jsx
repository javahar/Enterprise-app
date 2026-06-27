import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import LocationsPage from './LocationsPage'
import { organizationApi, locationApi } from '../api/services'

vi.mock('../api/services', () => ({
  PAGE_SIZE: 20,
  organizationApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
  locationApi: {
    getByOrg: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}))

const ORG_ACTIVE = { id: 'org-1', name: 'Acme Corp', description: '', isActive: true }
const ORG_INACTIVE = { id: 'org-2', name: 'Old Corp', description: '', isActive: false }

const LOCATIONS = [
  { id: 'loc-1', name: 'Downtown HQ', address: '1 Main St', city: 'New York', stateCode: 'NY', zip: '10001' },
  { id: 'loc-2', name: 'West Branch', address: '99 Oak Ave', city: 'LA', stateCode: 'CA', zip: '90001' },
]

const pageOf = (items) => ({ content: items, totalElements: items.length, page: 0, size: 20, totalPages: 1, last: true })

function renderPage(orgId = 'org-1') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/organizations/${orgId}/locations`]}>
        <Routes>
          <Route path="/organizations/:orgId/locations" element={<LocationsPage />} />
          <Route path="/organizations" element={<div>Organizations</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  vi.resetAllMocks()
})

describe('LocationsPage', () => {
  it('shows a loading spinner while data is fetching', () => {
    organizationApi.getById.mockReturnValue(new Promise(() => {}))
    locationApi.getByOrg.mockReturnValue(new Promise(() => {}))
    renderPage()
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  it('renders the Locations heading', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByRole('heading', { level: 4, name: /locations/i })).toBeInTheDocument())
  })

  it('shows the org name above the heading', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument())
  })

  it('renders the location list after data loads', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf(LOCATIONS))
    renderPage()
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())
    expect(screen.getByText('West Branch')).toBeInTheDocument()
    expect(screen.getByText('New York')).toBeInTheDocument()
  })

  it('shows empty-state message when there are no locations', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByText(/no locations yet/i)).toBeInTheDocument())
  })

  it('shows an error alert when location fetch fails', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockRejectedValue(new Error('Network error'))
    renderPage()
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Network error'))
  })

  // ── Inactive org rules ────────────────────────────────────────────────────

  it('shows a warning banner when org is inactive', async () => {
    organizationApi.getById.mockResolvedValue(ORG_INACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf(LOCATIONS))
    renderPage('org-2')
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(/inactive/i))
  })

  it('disables the Add Location button when org is inactive', async () => {
    organizationApi.getById.mockResolvedValue(ORG_INACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage('org-2')
    await waitFor(() => expect(screen.getByText('Old Corp')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /add location/i })).toBeDisabled()
  })

  it('hides edit and delete actions when org is inactive', async () => {
    organizationApi.getById.mockResolvedValue(ORG_INACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf(LOCATIONS))
    renderPage('org-2')
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument()
  })

  // ── Add Location flow ─────────────────────────────────────────────────────

  it('opens the New Location dialog when Add Location is clicked', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByRole('button', { name: /add location/i })).toBeEnabled())
    await userEvent.click(screen.getByRole('button', { name: /add location/i }))
    expect(screen.getByText('New Location')).toBeInTheDocument()
  })

  it('calls locationApi.create with form values on submit', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    locationApi.create.mockResolvedValue({ id: 'loc-3', name: 'New Spot', isActive: true })
    renderPage()
    await waitFor(() => expect(screen.getByRole('button', { name: /add location/i })).toBeEnabled())

    await userEvent.click(screen.getByRole('button', { name: /add location/i }))
    const dialog = screen.getByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText(/^name/i), 'New Spot')
    await userEvent.type(within(dialog).getByLabelText(/address/i), '5 Elm St')
    await userEvent.click(within(dialog).getByRole('button', { name: /save/i }))

    await waitFor(() =>
      expect(locationApi.create).toHaveBeenCalledWith(
        'org-1',
        expect.objectContaining({ name: 'New Spot', address: '5 Elm St' })
      )
    )
  })

  it('does not submit when name is empty', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByRole('button', { name: /add location/i })).toBeEnabled())

    await userEvent.click(screen.getByRole('button', { name: /add location/i }))
    await userEvent.click(screen.getByRole('button', { name: /save/i }))

    expect(locationApi.create).not.toHaveBeenCalled()
    expect(screen.getByText(/name is required/i)).toBeInTheDocument()
  })

  it('rejects a duplicate location name within the same org', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf(LOCATIONS))
    renderPage()
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /add location/i }))
    await userEvent.type(screen.getByLabelText(/^name/i), 'Downtown HQ')
    await userEvent.click(screen.getByRole('button', { name: /save/i }))

    expect(locationApi.create).not.toHaveBeenCalled()
    expect(screen.getByText(/already exists/i)).toBeInTheDocument()
  })

  // ── Edit flow ─────────────────────────────────────────────────────────────

  it('opens the edit dialog pre-filled with the location data', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf(LOCATIONS))
    renderPage()
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())

    const editButtons = screen.getAllByRole('button', { name: /edit/i })
    await userEvent.click(editButtons[0])

    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByText('Edit Location')).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('Downtown HQ')).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('1 Main St')).toBeInTheDocument()
  })

  it('allows saving with the same name when editing that location', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf(LOCATIONS))
    locationApi.update.mockResolvedValue(LOCATIONS[0])
    renderPage()
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())

    const editButtons = screen.getAllByRole('button', { name: /edit/i })
    await userEvent.click(editButtons[0])

    // Name unchanged — should not trigger duplicate error
    await userEvent.click(screen.getByRole('button', { name: /save/i }))

    await waitFor(() =>
      expect(locationApi.update).toHaveBeenCalledWith(
        'loc-1',
        expect.objectContaining({ name: 'Downtown HQ' })
      )
    )
  })

  // ── Delete flow ───────────────────────────────────────────────────────────

  it('shows a delete confirmation dialog on Delete click', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf(LOCATIONS))
    renderPage()
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())

    const deleteButtons = screen.getAllByRole('button', { name: /delete/i })
    await userEvent.click(deleteButtons[0])

    expect(screen.getByText(/delete location\?/i)).toBeInTheDocument()
    expect(screen.getByText(/permanently delete/i)).toBeInTheDocument()
  })

  it('calls locationApi.delete when delete is confirmed', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf(LOCATIONS))
    locationApi.delete.mockResolvedValue()
    renderPage()
    await waitFor(() => expect(screen.getByText('Downtown HQ')).toBeInTheDocument())

    const deleteButtons = screen.getAllByRole('button', { name: /delete/i })
    await userEvent.click(deleteButtons[0])

    const confirmDialog = screen.getByRole('dialog')
    await userEvent.click(within(confirmDialog).getByRole('button', { name: /^delete$/i }))

    await waitFor(() => expect(locationApi.delete).toHaveBeenCalledWith('loc-1'))
  })

  // ── Navigation ────────────────────────────────────────────────────────────

  it('navigates back to Organizations when back button is clicked', async () => {
    organizationApi.getById.mockResolvedValue(ORG_ACTIVE)
    locationApi.getByOrg.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() => expect(screen.getByRole('heading', { level: 4 })).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /back to organizations/i }))
    expect(screen.getByText('Organizations')).toBeInTheDocument()
  })
})
