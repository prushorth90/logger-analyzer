import type { LogEntry } from './logs'

export interface DeadLetterEvent {
  id: string
  ingestionEventId: string
  correlationId: string
  originalEvent: Omit<LogEntry, 'id'> & { eventId: string; schemaVersion: number }
  failureReason: string
  retryCount: number
  failedAt: string
  manualRetryCount: number
  lastRetriedAt: string | null
}

export interface PagedDeadLetterEventResponse {
  content: DeadLetterEvent[]
  pageNumber: number
  pageSize: number
  totalPages: number
  totalRecords: number
}

function isPagedResponse(value: unknown): value is PagedDeadLetterEventResponse {
  if (!value || typeof value !== 'object') return false
  const data = value as Record<string, unknown>
  return Array.isArray(data.content)
    && typeof data.pageNumber === 'number'
    && typeof data.pageSize === 'number'
    && typeof data.totalPages === 'number'
    && typeof data.totalRecords === 'number'
}

export async function fetchDeadLetterEvents(page: number, signal?: AbortSignal) {
  const response = await fetch(`/api/dead-letter-events?page=${page}&size=20`, {
    signal,
    cache: 'no-store',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) throw new Error(`Unable to load failed events (HTTP ${response.status}).`)
  const data: unknown = await response.json()
  if (!isPagedResponse(data)) throw new Error('The dead-letter service returned an unexpected response.')
  return data
}

export async function retryDeadLetterEvent(ingestionEventId: string): Promise<void> {
  const response = await fetch(`/api/dead-letter-events/${encodeURIComponent(ingestionEventId)}/retry`, {
    method: 'POST',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) throw new Error(`Unable to retry event (HTTP ${response.status}).`)
}