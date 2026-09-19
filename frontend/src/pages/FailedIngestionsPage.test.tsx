import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { afterEach, expect, it, vi } from 'vitest'
import { FailedIngestionsPage } from './FailedIngestionsPage'

const failedEvent = {
  id: 'dead-letter-1',
  ingestionEventId: 'a9f9d42f-f127-4d58-868b-aeb8a0f40d24',
  correlationId: 'correlation-123',
  originalEvent: {
    schemaVersion: 1,
    eventId: 'a9f9d42f-f127-4d58-868b-aeb8a0f40d24',
    correlationId: 'correlation-123',
    timestamp: '2026-09-17T12:00:00Z',
    serviceName: 'billing-api',
    environment: 'production',
    severity: 'ERROR',
    message: 'Payment failed',
    traceId: null,
    host: 'billing-01',
    metadata: {},
  },
  failureReason: 'database unavailable',
  retryCount: 2,
  failedAt: '2026-09-18T12:00:00Z',
  manualRetryCount: 0,
  lastRetriedAt: null,
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

it('shows failure details and explicitly retries an event', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(new Response(JSON.stringify({ content: [failedEvent], pageNumber: 0, pageSize: 20, totalPages: 1, totalRecords: 1 })))
    .mockResolvedValueOnce(new Response(JSON.stringify({ status: 'accepted' }), { status: 202 }))
    .mockResolvedValueOnce(new Response(JSON.stringify({ content: [{ ...failedEvent, manualRetryCount: 1 }], pageNumber: 0, pageSize: 20, totalPages: 1, totalRecords: 1 })))
  vi.stubGlobal('fetch', fetchMock)

  render(<FailedIngestionsPage />)
  expect(await screen.findByText('billing-api')).toBeInTheDocument()
  expect(screen.getByText('database unavailable')).toBeInTheDocument()
  expect(screen.getByText('2 auto / 0 manual')).toBeInTheDocument()

  fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
  await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
    `/api/dead-letter-events/${failedEvent.ingestionEventId}/retry`,
    expect.objectContaining({ method: 'POST' }),
  ))
  expect(await screen.findByText('2 auto / 1 manual')).toBeInTheDocument()
})