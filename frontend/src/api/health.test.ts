import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchHealth } from './health'

const response = { status: 'UP', service: 'log-analyzer', database: 'UP', timestamp: '2026-09-17T12:00:00Z' }
afterEach(() => vi.unstubAllGlobals())

describe('fetchHealth', () => {
  it('fetches the same-origin endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(response)))
    vi.stubGlobal('fetch', fetchMock)
    const result = await fetchHealth(new AbortController().signal)
    expect(result.data).toEqual(response)
    expect(result.statusCode).toBe(200)
    expect(fetchMock).toHaveBeenCalledWith('/api/health', expect.objectContaining({ cache: 'no-store' }))
  })

  it('preserves a database outage response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ ...response, status: 'DOWN', database: 'DOWN' }), { status: 503 })))
    const result = await fetchHealth(new AbortController().signal)
    expect(result.data?.database).toBe('DOWN')
    expect(result.statusCode).toBe(503)
  })

  it('handles unreachable backends', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    const result = await fetchHealth(new AbortController().signal)
    expect(result.data).toBeNull()
    expect(result.error).toBeTruthy()
  })

  it('rejects malformed successful responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ status: 'UP' }))))
    const result = await fetchHealth(new AbortController().signal)
    expect(result.data).toBeNull()
    expect(result.error).toBeTruthy()
  })
})