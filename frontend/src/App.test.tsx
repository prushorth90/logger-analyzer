import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { afterEach, expect, it, vi } from 'vitest'
import App from './App'

const healthy = { status: 'UP', service: 'log-analyzer', database: 'UP', timestamp: '2026-09-17T12:00:00Z' }

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  window.history.replaceState({}, '', '/')
})

function renderSystemPage() {
  window.history.replaceState({}, '', '/system')
  return render(<App />)
}

it('shows connected services, refreshes, and navigates to the response', async () => {
  const fetchMock = vi.fn().mockImplementation(async () => new Response(JSON.stringify(healthy)))
  vi.stubGlobal('fetch', fetchMock)
  renderSystemPage()
  expect(await screen.findByText('All systems operational')).toBeInTheDocument()
  expect(screen.getAllByText('Connected')).toHaveLength(2)
  fireEvent.click(screen.getByRole('button', { name: 'Refresh' }))
  await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
  fireEvent.click(screen.getByRole('link', { name: 'Inspect API' }))
  expect(screen.getByRole('heading', { name: 'Health API' })).toBeInTheDocument()
  expect(screen.getByText(/"service": "log-analyzer"/)).toBeInTheDocument()
})

it('distinguishes a database outage from a disconnected backend', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ ...healthy, status: 'DOWN', database: 'DOWN' }), { status: 503 })))
  renderSystemPage()
  expect(await screen.findByText('Database connection unavailable')).toBeInTheDocument()
  expect(screen.getByText('Connected')).toBeInTheDocument()
  expect(screen.getByText('Unavailable')).toBeInTheDocument()
})

it('shows an unreachable backend and allows pausing automatic refresh', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Offline')))
  renderSystemPage()
  expect(await screen.findByText('Backend disconnected')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('checkbox', { name: 'Auto-refresh' }))
  expect(screen.getByText('Paused')).toBeInTheDocument()
})

it('shows a pending state until the first response', () => {
  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))
  renderSystemPage()
  expect(screen.getByText('Connecting to your stack')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Checking' })).toBeDisabled()
})