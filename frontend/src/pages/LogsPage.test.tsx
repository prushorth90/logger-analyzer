import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { afterEach, expect, it, vi } from 'vitest'
import { LogsPage } from './LogsPage'
import { MemoryRouter } from 'react-router-dom'

const log = {
  id: '690a930c-e0a6-42a6-bc92-f98221aba920',
  timestamp: '2026-09-17T12:34:56Z',
  serviceName: 'payments',
  environment: 'production',
  severity: 'ERROR',
  message: 'Payment provider timed out',
  traceId: 'trace-42',
  host: 'payments-7fd9',
  metadata: { provider: 'acme-pay', attempts: 3 },
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

it('filters, paginates, and opens complete log details', async () => {
  const fetchMock = vi.fn().mockImplementation(async (url: string) => {
    const page = new URL(url, 'http://localhost').searchParams.get('page')
    if (url === '/api/saved-searches') return new Response(JSON.stringify([]))
    return new Response(JSON.stringify({ content: [log], pageNumber: Number(page), pageSize: 20, totalPages: 2, totalRecords: 21, queryExecutionMs: 8 }))
  })
  vi.stubGlobal('fetch', fetchMock)
  render(<MemoryRouter><LogsPage /></MemoryRouter>)

  expect(await screen.findByText('Payment provider timed out')).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'trace-42' })).toHaveAttribute('href', '/traces/trace-42')
  fireEvent.change(screen.getByLabelText('Search logs'), { target: { value: 'timed out' } })
  fireEvent.change(screen.getByLabelText('Severity'), { target: { value: 'ERROR' } })
  fireEvent.change(screen.getByLabelText('Service'), { target: { value: 'payments' } })
  fireEvent.change(screen.getByLabelText('Environment'), { target: { value: 'production' } })
  fireEvent.change(screen.getByLabelText('Trace ID'), { target: { value: 'trace-42' } })
  fireEvent.change(screen.getByLabelText('Sort'), { target: { value: 'OLDEST' } })
  fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))

  await waitFor(() => expect(fetchMock.mock.calls.filter(call => String(call[0]).startsWith('/api/logs?'))).toHaveLength(2))
  const logCalls = fetchMock.mock.calls.filter(call => String(call[0]).startsWith('/api/logs?'))
  const filteredUrl = new URL(logCalls[1][0], 'http://localhost')
  expect(Object.fromEntries(filteredUrl.searchParams)).toMatchObject({ severity: 'ERROR', serviceName: 'payments', environment: 'production', traceId: 'trace-42', search: 'timed out', sort: 'timestamp,asc', page: '0' })
  expect(screen.getByText('21 matching records · 8 ms')).toBeInTheDocument()
  expect(screen.getByText('timed').tagName).toBe('MARK')
  expect(screen.getByText('out').tagName).toBe('MARK')

  fireEvent.click(screen.getByRole('button', { name: 'Next page' }))
  await waitFor(() => expect(fetchMock.mock.calls.filter(call => String(call[0]).startsWith('/api/logs?'))).toHaveLength(3))
  const pagedCall = fetchMock.mock.calls.filter(call => String(call[0]).startsWith('/api/logs?'))[2][0]
  expect(new URL(pagedCall, 'http://localhost').searchParams.get('page')).toBe('1')

  fireEvent.click(screen.getByRole('row', { name: 'View details for Payment provider timed out' }))
  expect(screen.getByRole('dialog', { name: 'Event details' })).toBeInTheDocument()
  expect(screen.getByText('payments-7fd9')).toBeInTheDocument()
  expect(screen.getByText(/"provider": "acme-pay"/)).toBeInTheDocument()
})

it('shows an error with a retry action', async () => {
  const fetchMock = vi.fn().mockImplementation(async (url: string) =>
    url === '/api/saved-searches' ? new Response(JSON.stringify([])) : new Response('', { status: 503 }))
  vi.stubGlobal('fetch', fetchMock)
  render(<MemoryRouter><LogsPage /></MemoryRouter>)

  expect(await screen.findByText('Logs could not be loaded')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  await waitFor(() => expect(fetchMock.mock.calls.filter(call => String(call[0]).startsWith('/api/logs?'))).toHaveLength(2))
})

it('creates, applies, and deletes a saved search', async () => {
  const saved = {
    id: 'a9f9d42f-f127-4d58-868b-aeb8a0f40d24', name: 'Payment errors', query: 'payment timeout',
    severity: 'ERROR', serviceName: 'payment-service', environment: 'production', traceId: null,
    startTimestamp: null, endTimestamp: null, sortDirection: 'NEWEST', createdAt: '2026-09-19T12:00:00Z',
  }
  const fetchMock = vi.fn().mockImplementation(async (url: string, init?: RequestInit) => {
    if (url === '/api/saved-searches' && init?.method === 'POST') return new Response(JSON.stringify(saved), { status: 201 })
    if (url === '/api/saved-searches') return new Response(JSON.stringify([]))
    if (url.startsWith('/api/saved-searches/') && init?.method === 'DELETE') return new Response(null, { status: 204 })
    return new Response(JSON.stringify({ content: [log], pageNumber: 0, pageSize: 20, totalPages: 1, totalRecords: 1, queryExecutionMs: 4 }))
  })
  vi.stubGlobal('fetch', fetchMock)
  render(<MemoryRouter><LogsPage /></MemoryRouter>)
  await screen.findByText('Payment provider timed out')

  fireEvent.change(screen.getByLabelText('Search logs'), { target: { value: 'payment timeout' } })
  fireEvent.change(screen.getByLabelText('Severity'), { target: { value: 'ERROR' } })
  fireEvent.change(screen.getByLabelText('Saved search name'), { target: { value: 'Payment errors' } })
  fireEvent.click(screen.getByRole('button', { name: 'Save' }))

  const savedButton = await screen.findByRole('button', { name: 'Payment errors' })
  fireEvent.click(savedButton)
  await waitFor(() => expect(screen.getByLabelText('Service')).toHaveValue('payment-service'))
  fireEvent.click(screen.getByRole('button', { name: 'Delete Payment errors' }))
  await waitFor(() => expect(screen.queryByRole('button', { name: 'Payment errors' })).not.toBeInTheDocument())
})