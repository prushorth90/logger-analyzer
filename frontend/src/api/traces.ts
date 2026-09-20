import type { LogEntry } from './logs'

export interface TraceResponse {
  traceId: string
  startedAt: string
  endedAt: string
  durationMs: number
  serviceSequence: string[]
  events: LogEntry[]
}

function isTraceResponse(value: unknown): value is TraceResponse {
  if (!value || typeof value !== 'object') return false
  const data = value as Record<string, unknown>
  return typeof data.traceId === 'string'
    && typeof data.startedAt === 'string'
    && typeof data.endedAt === 'string'
    && typeof data.durationMs === 'number'
    && Array.isArray(data.serviceSequence)
    && Array.isArray(data.events)
}

export async function fetchTrace(traceId: string, signal?: AbortSignal): Promise<TraceResponse> {
  const response = await fetch(`/api/traces/${encodeURIComponent(traceId)}`, {
    signal,
    cache: 'no-store',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) throw new Error(`Unable to load trace (HTTP ${response.status}).`)
  const data: unknown = await response.json()
  if (!isTraceResponse(data)) throw new Error('The trace service returned an unexpected response.')
  return data
}