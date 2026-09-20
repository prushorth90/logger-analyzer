import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { afterEach, expect, it, vi } from 'vitest'
import { AlertsPage } from './AlertsPage'

const rule = {
  id: 'rule-1', name: 'Payment errors', serviceName: 'payment-service', thresholdCount: 20,
  windowMinutes: 5, cooldownMinutes: 30, active: true, createdAt: '2026-09-20T12:00:00Z',
}

const alert = {
  id: 'alert-1', ruleId: 'rule-1', ruleName: 'Payment errors', serviceName: 'payment-service',
  status: 'OPEN', thresholdCount: 20, observedCount: 24, windowMinutes: 5,
  windowStartedAt: '2026-09-20T11:55:00Z', windowEndedAt: '2026-09-20T12:00:00Z',
  openedAt: '2026-09-20T12:00:00Z', acknowledgedAt: null, resolvedAt: null, resolutionReason: null,
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

it('creates rules and acknowledges an open alert', async () => {
  const fetchMock = vi.fn().mockImplementation(async (url: string, init?: RequestInit) => {
    if (url === '/api/alerts/rules' && init?.method === 'POST') {
      return new Response(JSON.stringify({ ...rule, id: 'rule-2', name: 'Order errors', serviceName: 'order-service' }), { status: 201 })
    }
    if (url === '/api/alerts/rules') return new Response(JSON.stringify([rule]))
    if (url === '/api/alerts/alert-1/acknowledge') {
      return new Response(JSON.stringify({ ...alert, status: 'ACKNOWLEDGED', acknowledgedAt: '2026-09-20T12:01:00Z' }))
    }
    if (url.startsWith('/api/alerts?')) {
      return new Response(JSON.stringify({ content: [alert], number: 0, size: 20, totalPages: 1, totalElements: 1 }))
    }
    return new Response('{}', { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)
  render(<AlertsPage />)

  expect(await screen.findByText((_content, element) =>
    element?.tagName === 'P' && element.textContent === 'ERROR count for payment-service > 20 during 5 minutes')).toBeInTheDocument()
  expect(screen.getByText('24')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Acknowledge' }))
  expect(await screen.findByText('ACKNOWLEDGED')).toBeInTheDocument()

  fireEvent.change(screen.getByLabelText('Rule name'), { target: { value: 'Order errors' } })
  fireEvent.change(screen.getByLabelText('Service'), { target: { value: 'order-service' } })
  fireEvent.click(screen.getByRole('button', { name: 'Create rule' }))
  expect(await screen.findByText('Order errors')).toBeInTheDocument()
  await waitFor(() => expect(fetchMock).toHaveBeenCalledWith('/api/alerts/rules', expect.objectContaining({ method: 'POST' })))
})