import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { afterEach, expect, it, vi } from 'vitest'
import App from './App'

const healthy = { status: 'UP', service: 'log-analyzer', database: 'UP', timestamp: '2026-09-17T12:00:00Z' }
const overview = {
  startTimestamp: '2026-09-16T12:00:00Z', endTimestamp: '2026-09-17T12:00:00Z',
  totalLogs: 1482, errorCount: 28, warningCount: 73, activeServices: 4,
  logsByService: [{ name: 'billing-api', count: 900 }, { name: 'orders-api', count: 582 }],
  logsBySeverity: [{ name: 'INFO', count: 1381 }, { name: 'WARN', count: 73 }, { name: 'ERROR', count: 28 }],
  logsOverTime: [{ timestamp: '2026-09-17T10:00:00Z', count: 700 }, { timestamp: '2026-09-17T11:00:00Z', count: 782 }],
  errorsByService: [{ name: 'billing-api', count: 20 }, { name: 'orders-api', count: 8 }],
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  window.history.replaceState({}, '', '/')
})

function renderSystemPage() {
  window.history.replaceState({}, '', '/system')
  return render(<App />)
}

function mockEndpoints(healthResponse: unknown = healthy, overviewResponse: unknown = overview) {
  return vi.fn().mockImplementation(async (input: RequestInfo | URL) => {
    const url = String(input)
    return new Response(JSON.stringify(url.startsWith('/api/logs/overview') ? overviewResponse : healthResponse))
  })
}

it('shows operational metrics, changes period, and refreshes both endpoints', async () => {
  const fetchMock = mockEndpoints()
  vi.stubGlobal('fetch', fetchMock)
  renderSystemPage()
  expect(await screen.findByText('Ingestion stack operational')).toBeInTheDocument()
  expect(await screen.findByText('1,482')).toBeInTheDocument()
  expect(screen.getByText('Errors').nextElementSibling).toHaveTextContent('28')
  expect(screen.getByText('Log volume')).toBeInTheDocument()
  expect(screen.getByText('Errors by service')).toBeInTheDocument()
  fireEvent.change(screen.getByRole('combobox', { name: 'Time period' }), { target: { value: '6' } })
  await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3))
  fireEvent.click(screen.getByRole('button', { name: 'Refresh' }))
  await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(5))
})

it('distinguishes a database outage from a disconnected backend', async () => {
  vi.stubGlobal('fetch', mockEndpoints({ ...healthy, status: 'DOWN', database: 'DOWN' }))
  renderSystemPage()
  expect(await screen.findByText('PostgreSQL unavailable')).toBeInTheDocument()
  expect(await screen.findByText('1,482')).toBeInTheDocument()
})

it('shows an unreachable backend and unavailable overview', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Offline')))
  renderSystemPage()
  expect(await screen.findByText('Backend disconnected')).toBeInTheDocument()
  expect(await screen.findByText('Overview unavailable')).toBeInTheDocument()
})

it('shows a pending state until the first response', () => {
  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))
  renderSystemPage()
  expect(screen.getByText('Checking ingestion stack')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Refresh' })).toBeDisabled()
})