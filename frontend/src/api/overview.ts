export interface NamedCount {
  name: string
  count: number
}

export interface TimeCount {
  timestamp: string
  count: number
}

export interface LogOverview {
  startTimestamp: string
  endTimestamp: string
  totalLogs: number
  errorCount: number
  warningCount: number
  activeServices: number
  logsByService: NamedCount[]
  logsBySeverity: NamedCount[]
  logsOverTime: TimeCount[]
  errorsByService: NamedCount[]
}

function isNamedCount(value: unknown): value is NamedCount {
  return typeof value === 'object' && value !== null
    && typeof (value as NamedCount).name === 'string'
    && typeof (value as NamedCount).count === 'number'
}

function isOverview(value: unknown): value is LogOverview {
  if (typeof value !== 'object' || value === null) return false
  const overview = value as LogOverview
  return typeof overview.startTimestamp === 'string'
    && typeof overview.endTimestamp === 'string'
    && ['totalLogs', 'errorCount', 'warningCount', 'activeServices'].every(key =>
      typeof overview[key as keyof LogOverview] === 'number')
    && [overview.logsByService, overview.logsBySeverity, overview.errorsByService]
      .every(items => Array.isArray(items) && items.every(isNamedCount))
    && Array.isArray(overview.logsOverTime)
    && overview.logsOverTime.every(item => typeof item.timestamp === 'string' && typeof item.count === 'number')
}

export async function fetchLogOverview(startTimestamp: string, endTimestamp: string, signal?: AbortSignal) {
  const query = new URLSearchParams({ startTimestamp, endTimestamp })
  const response = await fetch(`/api/logs/overview?${query}`, { signal })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  const body: unknown = await response.json()
  if (!isOverview(body)) throw new Error('The server returned an unexpected response')
  return body
}