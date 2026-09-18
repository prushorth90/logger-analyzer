export interface HealthResponse {
  status: 'UP' | 'DOWN'
  service: string
  database: 'UP' | 'DOWN'
  timestamp: string
}

export interface HealthCheck {
  data: HealthResponse | null
  statusCode: number | null
  duration: number
  checkedAt: Date
  error: string | null
}

function isHealthResponse(value: unknown): value is HealthResponse {
  if (!value || typeof value !== 'object') return false
  const data = value as Record<string, unknown>
  return (data.status === 'UP' || data.status === 'DOWN')
    && (data.database === 'UP' || data.database === 'DOWN')
    && data.service === 'log-analyzer'
    && typeof data.timestamp === 'string'
    && !Number.isNaN(Date.parse(data.timestamp))
}

export async function fetchHealth(signal: AbortSignal): Promise<HealthCheck> {
  const startedAt = performance.now()
  let statusCode: number | null = null
  try {
    const response = await fetch('/api/health', {
      signal: AbortSignal.any([signal, AbortSignal.timeout(6000)]),
      cache: 'no-store',
      headers: { Accept: 'application/json' },
    })
    statusCode = response.status
    if (response.status !== 200 && response.status !== 503) throw new Error('Unexpected HTTP status.')
    const data: unknown = await response.json()
    if (!isHealthResponse(data)) throw new Error('Unexpected health response.')
    return { data, statusCode, duration: Math.round(performance.now() - startedAt), checkedAt: new Date(), error: null }
  } catch (error) {
    return {
      data: null, statusCode, duration: Math.round(performance.now() - startedAt), checkedAt: new Date(),
      error: error instanceof Error && error.name === 'TimeoutError'
        ? 'The backend did not respond within 6 seconds.'
        : 'The backend could not be reached or returned an invalid response.',
    }
  }
}