import type { LogSeverity } from './logs'

export type SearchSortDirection = 'NEWEST' | 'OLDEST'

export interface SavedSearchDefinition {
  name: string
  query: string | null
  severity: LogSeverity | null
  serviceName: string | null
  environment: string | null
  traceId: string | null
  startTimestamp: string | null
  endTimestamp: string | null
  sortDirection: SearchSortDirection
}

export interface SavedSearch extends SavedSearchDefinition {
  id: string
  createdAt: string
}

function isSavedSearch(value: unknown): value is SavedSearch {
  if (!value || typeof value !== 'object') return false
  const data = value as Record<string, unknown>
  return typeof data.id === 'string'
    && typeof data.name === 'string'
    && typeof data.sortDirection === 'string'
    && typeof data.createdAt === 'string'
}

export async function fetchSavedSearches(signal?: AbortSignal): Promise<SavedSearch[]> {
  const response = await fetch('/api/saved-searches', { signal, cache: 'no-store', headers: { Accept: 'application/json' } })
  if (!response.ok) throw new Error(`Unable to load saved searches (HTTP ${response.status}).`)
  const data: unknown = await response.json()
  if (!Array.isArray(data) || !data.every(isSavedSearch)) throw new Error('The saved-search service returned an unexpected response.')
  return data
}

export async function createSavedSearch(definition: SavedSearchDefinition): Promise<SavedSearch> {
  const response = await fetch('/api/saved-searches', {
    method: 'POST',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify(definition),
  })
  if (!response.ok) throw new Error(`Unable to save search (HTTP ${response.status}).`)
  const data: unknown = await response.json()
  if (!isSavedSearch(data)) throw new Error('The saved-search service returned an unexpected response.')
  return data
}

export async function deleteSavedSearch(id: string): Promise<void> {
  const response = await fetch(`/api/saved-searches/${encodeURIComponent(id)}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`Unable to delete saved search (HTTP ${response.status}).`)
}