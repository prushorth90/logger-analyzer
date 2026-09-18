export type LogSeverity = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'

export interface LogEntry {
  id: string
  timestamp: string
  serviceName: string
  environment: string
  severity: LogSeverity
  message: string
  traceId: string | null
  host: string | null
  metadata: Record<string, unknown>
}

export interface PagedLogResponse {
  content: LogEntry[]
  pageNumber: number
  pageSize: number
  totalPages: number
  totalRecords: number
}

export interface LogFilters {
  severity?: LogSeverity
  serviceName?: string
  environment?: string
  search?: string
  startTimestamp?: string
  endTimestamp?: string
  page: number
  size: number
}

function isPagedLogResponse(value: unknown): value is PagedLogResponse {
  if (!value || typeof value !== 'object') return false
  const data = value as Record<string, unknown>
  return Array.isArray(data.content)
    && typeof data.pageNumber === 'number'
    && typeof data.pageSize === 'number'
    && typeof data.totalPages === 'number'
    && typeof data.totalRecords === 'number'
}

export async function fetchLogs(filters: LogFilters, signal?: AbortSignal): Promise<PagedLogResponse> {
  const params = new URLSearchParams({ page: String(filters.page), size: String(filters.size) })
  const optionalFilters: [string, string | undefined][] = [
    ['severity', filters.severity],
    ['serviceName', filters.serviceName],
    ['environment', filters.environment],
    ['search', filters.search],
    ['startTimestamp', filters.startTimestamp],
    ['endTimestamp', filters.endTimestamp],
  ]
  optionalFilters.forEach(([key, value]) => {
    if (value?.trim()) params.set(key, value.trim())
  })

  const response = await fetch(`/api/logs?${params}`, {
    signal,
    cache: 'no-store',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) throw new Error(`Unable to load logs (HTTP ${response.status}).`)
  const data: unknown = await response.json()
  if (!isPagedLogResponse(data)) throw new Error('The log service returned an unexpected response.')
  return data
}