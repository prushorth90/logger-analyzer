import { cleanup, render, screen } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { afterEach, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { TracePage } from './TracePage'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

it('shows an ordered service sequence and emphasizes warning and error events', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
    traceId: 'trace-42',
    startedAt: '2026-09-17T12:00:00Z',
    endedAt: '2026-09-17T12:00:02Z',
    durationMs: 2000,
    serviceSequence: ['api-gateway', 'order-service', 'payment-service', 'inventory-service'],
    events: [
      { id: '1', timestamp: '2026-09-17T12:00:00Z', serviceName: 'api-gateway', environment: 'production', severity: 'INFO', message: 'Request received', traceId: 'trace-42', host: 'gateway-01', metadata: {} },
      { id: '2', timestamp: '2026-09-17T12:00:01Z', serviceName: 'order-service', environment: 'production', severity: 'WARN', message: 'Inventory response slow', traceId: 'trace-42', host: 'orders-01', metadata: {} },
      { id: '3', timestamp: '2026-09-17T12:00:02Z', serviceName: 'payment-service', environment: 'production', severity: 'ERROR', message: 'Payment failed', traceId: 'trace-42', host: 'payments-01', metadata: {} },
    ],
  }))))

  const { container } = render(<MemoryRouter initialEntries={['/traces/trace-42']}><Routes><Route path="/traces/:traceId" element={<TracePage />} /></Routes></MemoryRouter>)

  expect(await screen.findByText('Request received')).toBeInTheDocument()
  expect(screen.getByLabelText('Service sequence')).toHaveTextContent('api-gatewayorder-servicepayment-serviceinventory-service')
  expect(screen.getByText('3 events')).toBeInTheDocument()
  expect(screen.getByText('2.00 s')).toBeInTheDocument()
  expect(container.querySelector('.trace-event-warn')).toHaveTextContent('Inventory response slow')
  expect(container.querySelector('.trace-event-error')).toHaveTextContent('Payment failed')
})