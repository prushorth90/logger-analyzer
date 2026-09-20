import { afterEach, expect, it, vi } from 'vitest'
import { fetchLogs } from './logs'

afterEach(() => vi.unstubAllGlobals())

it('serializes filters using the backend query contract', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
    content: [], pageNumber: 2, pageSize: 25, totalPages: 3, totalRecords: 52, queryExecutionMs: 14,
  })))
  vi.stubGlobal('fetch', fetchMock)

  await fetchLogs({
    page: 2,
    size: 25,
    severity: 'ERROR',
    serviceName: 'payments',
    environment: 'production',
    traceId: 'trace-42',
    search: 'timed out',
    sortDirection: 'OLDEST',
    startTimestamp: '2026-09-16T00:00:00.000Z',
    endTimestamp: '2026-09-17T23:59:59.000Z',
  })

  const url = new URL(fetchMock.mock.calls[0][0], 'http://localhost')
  expect(Object.fromEntries(url.searchParams)).toEqual({
    page: '2',
    size: '25',
    severity: 'ERROR',
    serviceName: 'payments',
    environment: 'production',
    traceId: 'trace-42',
    search: 'timed out',
    startTimestamp: '2026-09-16T00:00:00.000Z',
    endTimestamp: '2026-09-17T23:59:59.000Z',
    sort: 'timestamp,asc',
  })
})

it('rejects unsuccessful and malformed responses', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(new Response('', { status: 500 })))
  await expect(fetchLogs({ page: 0, size: 20 })).rejects.toThrow('HTTP 500')

  vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(new Response('{}')))
  await expect(fetchLogs({ page: 0, size: 20 })).rejects.toThrow('unexpected response')
})