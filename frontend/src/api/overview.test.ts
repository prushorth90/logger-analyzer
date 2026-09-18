import { afterEach, expect, it, vi } from 'vitest'
import { fetchLogOverview } from './overview'

const overview = {
  startTimestamp: '2026-09-16T12:00:00Z', endTimestamp: '2026-09-17T12:00:00Z',
  totalLogs: 12, errorCount: 2, warningCount: 3, activeServices: 2,
  logsByService: [{ name: 'billing-api', count: 8 }],
  logsBySeverity: [{ name: 'INFO', count: 7 }],
  logsOverTime: [{ timestamp: '2026-09-17T11:00:00Z', count: 4 }],
  errorsByService: [{ name: 'billing-api', count: 2 }],
}

afterEach(() => vi.unstubAllGlobals())

it('requests overview metrics for the selected range', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(overview)))
  vi.stubGlobal('fetch', fetchMock)

  await fetchLogOverview(overview.startTimestamp, overview.endTimestamp)

  const url = new URL(fetchMock.mock.calls[0][0], 'http://localhost')
  expect(url.pathname).toBe('/api/logs/overview')
  expect(Object.fromEntries(url.searchParams)).toEqual({
    startTimestamp: overview.startTimestamp,
    endTimestamp: overview.endTimestamp,
  })
})

it('rejects malformed overview responses', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}')))
  await expect(fetchLogOverview(overview.startTimestamp, overview.endTimestamp)).rejects.toThrow('unexpected response')
})