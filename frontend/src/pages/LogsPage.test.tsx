import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { afterEach, expect, it, vi } from 'vitest'
import { LogsPage } from './LogsPage'

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
    return new Response(JSON.stringify({ content: [log], pageNumber: Number(page), pageSize: 20, totalPages: 2, totalRecords: 21 }))
  })
  vi.stubGlobal('fetch', fetchMock)
  render(<LogsPage />)

  expect(await screen.findByText('Payment provider timed out')).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Message search'), { target: { value: 'timed out' } })
  fireEvent.change(screen.getByLabelText('Severity'), { target: { value: 'ERROR' } })
  fireEvent.change(screen.getByLabelText('Service'), { target: { value: 'payments' } })
  fireEvent.change(screen.getByLabelText('Environment'), { target: { value: 'production' } })
  fireEvent.click(screen.getByRole('button', { name: 'Apply' }))

  await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
  const filteredUrl = new URL(fetchMock.mock.calls[1][0], 'http://localhost')
  expect(Object.fromEntries(filteredUrl.searchParams)).toMatchObject({ severity: 'ERROR', serviceName: 'payments', environment: 'production', search: 'timed out', page: '0' })

  fireEvent.click(screen.getByRole('button', { name: 'Next page' }))
  await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3))
  expect(new URL(fetchMock.mock.calls[2][0], 'http://localhost').searchParams.get('page')).toBe('1')

  fireEvent.click(screen.getByText('Payment provider timed out'))
  expect(screen.getByRole('dialog', { name: 'Event details' })).toBeInTheDocument()
  expect(screen.getByText('payments-7fd9')).toBeInTheDocument()
  expect(screen.getByText(/"provider": "acme-pay"/)).toBeInTheDocument()
})

it('shows an error with a retry action', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response('', { status: 503 }))
  vi.stubGlobal('fetch', fetchMock)
  render(<LogsPage />)

  expect(await screen.findByText('Logs could not be loaded')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
})