import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import OrganizationsPage from './OrganizationsPage'
import { organizationApi } from '../api/services'

vi.mock('../api/services', () => ({
  PAGE_SIZE: 20,
  organizationApi: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}))

const ORGS = [
  { id: '1', name: 'Acme Corp', description: 'Main org', isActive: true },
  { id: '2', name: 'Globex', description: '', isActive: false },
]

const pageOf = (items) => ({ content: items, totalElements: items.length, page: 0, size: 20, totalPages: 1, last: true })

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <OrganizationsPage />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  vi.resetAllMocks()
})

describe('OrganizationsPage', () => {
  it('shows a loading spinner while data is fetching', () => {
    organizationApi.getAll.mockReturnValue(new Promise(() => {}))
    renderPage()
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  it('renders the organization list after data loads', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf(ORGS))
    renderPage()
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument())
    expect(screen.getByText('Globex')).toBeInTheDocument()
    expect(screen.getByText('Main org')).toBeInTheDocument()
  })

  it('shows Active / Inactive status chips', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf(ORGS))
    renderPage()
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument())
    expect(screen.getByText('Active')).toBeInTheDocument()
    expect(screen.getByText('Inactive')).toBeInTheDocument()
  })

  it('shows empty-state message when there are no organizations', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() =>
      expect(screen.getByText(/no organizations yet/i)).toBeInTheDocument()
    )
  })

  it('shows an error alert when fetch fails', async () => {
    organizationApi.getAll.mockRejectedValue(new Error('Server error'))
    renderPage()
    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('Server error')
    )
  })

  it('opens the New Organization dialog when Add Organization is clicked', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf([]))
    renderPage()
    await userEvent.click(screen.getByRole('button', { name: /add organization/i }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('New Organization')).toBeInTheDocument()
  })

  it('calls organizationApi.create with form values on submit', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf([]))
    organizationApi.create.mockResolvedValue({ id: '3', name: 'New Org', description: 'Desc', isActive: true })
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: /add organization/i }))

    const dialog = screen.getByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText(/name/i), 'New Org')
    await userEvent.type(within(dialog).getByLabelText(/description/i), 'Desc')
    await userEvent.click(within(dialog).getByRole('button', { name: /save/i }))

    await waitFor(() =>
      expect(organizationApi.create).toHaveBeenCalledWith({
        name: 'New Org',
        description: 'Desc',
        isActive: true,
      })
    )
  })

  it('does not submit the form when name is empty', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf([]))
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: /add organization/i }))
    const dialog = screen.getByRole('dialog')
    await userEvent.click(within(dialog).getByRole('button', { name: /save/i }))

    expect(organizationApi.create).not.toHaveBeenCalled()
    expect(screen.getByText(/name is required/i)).toBeInTheDocument()
  })

  it('closes the dialog on Cancel', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf([]))
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: /add organization/i }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /cancel/i }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('opens the edit dialog pre-filled with the org data', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf(ORGS))
    renderPage()
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument())

    const editButtons = screen.getAllByRole('button', { name: /edit/i })
    await userEvent.click(editButtons[0])

    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByText('Edit Organization')).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('Acme Corp')).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('Main org')).toBeInTheDocument()
  })

  it('calls organizationApi.update with edited values on submit', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf(ORGS))
    organizationApi.update.mockResolvedValue({ ...ORGS[0], name: 'Acme Corp Updated' })
    renderPage()
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument())

    const editButtons = screen.getAllByRole('button', { name: /edit/i })
    await userEvent.click(editButtons[0])

    const dialog = screen.getByRole('dialog')
    const nameInput = within(dialog).getByDisplayValue('Acme Corp')
    await userEvent.clear(nameInput)
    await userEvent.type(nameInput, 'Acme Corp Updated')
    await userEvent.click(within(dialog).getByRole('button', { name: /save/i }))

    await waitFor(() =>
      expect(organizationApi.update).toHaveBeenCalledWith(
        '1',
        expect.objectContaining({ name: 'Acme Corp Updated' })
      )
    )
  })

  it('shows a delete confirmation dialog when Delete is clicked', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf(ORGS))
    renderPage()
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument())

    const deleteButtons = screen.getAllByRole('button', { name: /delete/i })
    await userEvent.click(deleteButtons[0])

    expect(screen.getByText(/delete organization\?/i)).toBeInTheDocument()
  })

  it('calls organizationApi.delete when delete is confirmed', async () => {
    organizationApi.getAll.mockResolvedValue(pageOf(ORGS))
    organizationApi.delete.mockResolvedValue()
    renderPage()
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument())

    const deleteButtons = screen.getAllByRole('button', { name: /delete/i })
    await userEvent.click(deleteButtons[0])

    const confirmDialog = screen.getByRole('dialog')
    await userEvent.click(within(confirmDialog).getByRole('button', { name: /^delete$/i }))

    await waitFor(() => expect(organizationApi.delete).toHaveBeenCalledWith('1'))
  })
})
