import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { AlertCircle, BookmarkPlus, ChevronLeft, ChevronRight, RefreshCw, Search, Trash2, X } from 'lucide-react'
import { fetchLogs, type LogEntry, type LogFilters, type LogSeverity, type PagedLogResponse } from '../api/logs'
import { createSavedSearch, deleteSavedSearch, fetchSavedSearches, type SavedSearch, type SavedSearchDefinition } from '../api/savedSearches'

const PAGE_SIZE = 20
const EMPTY_FILTERS = { severity: '', serviceName: '', environment: '', traceId: '', search: '', startDate: '', endDate: '', sortDirection: 'NEWEST' }
type FilterForm = typeof EMPTY_FILTERS

function toTimestamp(date: string, endOfDay = false) {
  if (!date) return undefined
  return new Date(`${date}T${endOfDay ? '23:59:59.999' : '00:00:00.000'}`).toISOString()
}

function formatTimestamp(timestamp: string) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit',
  }).format(new Date(timestamp))
}

function buildFilters(form: FilterForm, page: number): LogFilters {
  return {
    page,
    size: PAGE_SIZE,
    severity: (form.severity || undefined) as LogSeverity | undefined,
    serviceName: form.serviceName || undefined,
    environment: form.environment || undefined,
    traceId: form.traceId || undefined,
    search: form.search || undefined,
    startTimestamp: toTimestamp(form.startDate),
    endTimestamp: toTimestamp(form.endDate, true),
    sortDirection: form.sortDirection as LogFilters['sortDirection'],
  }
}

function toDateInput(timestamp: string | null) {
  return timestamp?.slice(0, 10) ?? ''
}

function highlightMessage(message: string, query: string): ReactNode {
  const text = query.replace(/(?:^|\s)(?:severity|service|environment|traceId):(?:"[^"]*"|\S+)/gi, ' ')
  const terms = text.trim().split(/\s+/).filter(term => term.length > 1)
  if (terms.length === 0) return message
  const pattern = new RegExp(`(${terms.map(term => term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')})`, 'gi')
  return message.split(pattern).map((part, index) =>
    terms.some(term => term.toLowerCase() === part.toLowerCase()) ? <mark key={`${part}-${index}`}>{part}</mark> : part,
  )
}

export function LogsPage() {
  const [form, setForm] = useState<FilterForm>(EMPTY_FILTERS)
  const [filters, setFilters] = useState<LogFilters>(() => buildFilters(EMPTY_FILTERS, 0))
  const [result, setResult] = useState<PagedLogResponse | null>(null)
  const [selectedLog, setSelectedLog] = useState<LogEntry | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [requestVersion, setRequestVersion] = useState(0)
  const [savedSearches, setSavedSearches] = useState<SavedSearch[]>([])
  const [saveName, setSaveName] = useState('')
  const [saving, setSaving] = useState(false)
  const [savedSearchError, setSavedSearchError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchLogs(filters, controller.signal)
      .then(setResult)
      .catch((requestError: unknown) => {
        if (requestError instanceof Error && requestError.name !== 'AbortError') setError(requestError.message)
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [filters, requestVersion])

  useEffect(() => {
    const controller = new AbortController()
    fetchSavedSearches(controller.signal).then(setSavedSearches).catch((cause: unknown) => {
      if (!controller.signal.aborted) setSavedSearchError(cause instanceof Error ? cause.message : 'Unable to load saved searches.')
    })
    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (!selectedLog) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setSelectedLog(null)
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [selectedLog])

  function updateFilter(name: keyof FilterForm, value: string) {
    setForm(current => ({ ...current, [name]: value }))
  }

  function applyFilters(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError(null)
    setFilters(buildFilters(form, 0))
  }

  function clearFilters() {
    setForm(EMPTY_FILTERS)
    setLoading(true)
    setError(null)
    setFilters(buildFilters(EMPTY_FILTERS, 0))
  }

  function goToPage(page: number) {
    setLoading(true)
    setError(null)
    setFilters(current => ({ ...current, page }))
  }

  function retry() {
    setLoading(true)
    setError(null)
    setRequestVersion(version => version + 1)
  }

  function applySavedSearch(savedSearch: SavedSearch) {
    const nextForm: FilterForm = {
      search: savedSearch.query ?? '',
      severity: savedSearch.severity ?? '',
      serviceName: savedSearch.serviceName ?? '',
      environment: savedSearch.environment ?? '',
      traceId: savedSearch.traceId ?? '',
      startDate: toDateInput(savedSearch.startTimestamp),
      endDate: toDateInput(savedSearch.endTimestamp),
      sortDirection: savedSearch.sortDirection,
    }
    setForm(nextForm)
    setFilters(buildFilters(nextForm, 0))
    setLoading(true)
    setError(null)
  }

  async function saveCurrentSearch(event: FormEvent) {
    event.preventDefault()
    if (!saveName.trim()) return
    setSaving(true)
    setSavedSearchError(null)
    const definition: SavedSearchDefinition = {
      name: saveName.trim(),
      query: form.search || null,
      severity: (form.severity || null) as LogSeverity | null,
      serviceName: form.serviceName || null,
      environment: form.environment || null,
      traceId: form.traceId || null,
      startTimestamp: toTimestamp(form.startDate) ?? null,
      endTimestamp: toTimestamp(form.endDate, true) ?? null,
      sortDirection: form.sortDirection as SavedSearchDefinition['sortDirection'],
    }
    try {
      const saved = await createSavedSearch(definition)
      setSavedSearches(current => [...current, saved].sort((left, right) => left.name.localeCompare(right.name)))
      setSaveName('')
    } catch (cause) {
      setSavedSearchError(cause instanceof Error ? cause.message : 'Unable to save search.')
    } finally {
      setSaving(false)
    }
  }

  async function removeSavedSearch(id: string) {
    setSavedSearchError(null)
    try {
      await deleteSavedSearch(id)
      setSavedSearches(current => current.filter(saved => saved.id !== id))
    } catch (cause) {
      setSavedSearchError(cause instanceof Error ? cause.message : 'Unable to delete saved search.')
    }
  }

  return (
    <section className="page-content logs-page">
      <div className="page-heading logs-heading">
        <div><p className="eyebrow">OBSERVABILITY / LOGS</p><h1>Log explorer</h1><p>Search and inspect application events across your services.</p></div>
        <button className="button" onClick={retry} disabled={loading}>
          <RefreshCw size={14} className={loading ? 'spin' : ''} />Refresh
        </button>
      </div>

      <form className="search-workbench" onSubmit={applyFilters}>
        <label className="search-command"><span>Search logs</span><div><Search size={21} /><input value={form.search} onChange={event => updateFilter('search', event.target.value)} placeholder="severity:ERROR payment-service" /><button type="submit" className="button primary">Search</button></div></label>
        <div className="log-filters">
        <label><span>Severity</span><select value={form.severity} onChange={event => updateFilter('severity', event.target.value)}><option value="">All levels</option><option>DEBUG</option><option>INFO</option><option>WARN</option><option>ERROR</option></select></label>
        <label><span>Service</span><input value={form.serviceName} onChange={event => updateFilter('serviceName', event.target.value)} placeholder="Any service" /></label>
        <label><span>Environment</span><input value={form.environment} onChange={event => updateFilter('environment', event.target.value)} placeholder="Any environment" /></label>
        <label><span>Trace ID</span><input value={form.traceId} onChange={event => updateFilter('traceId', event.target.value)} placeholder="Any trace" /></label>
        <label><span>Start date</span><input type="date" value={form.startDate} onChange={event => updateFilter('startDate', event.target.value)} /></label>
        <label><span>End date</span><input type="date" min={form.startDate || undefined} value={form.endDate} onChange={event => updateFilter('endDate', event.target.value)} /></label>
        <label><span>Sort</span><select value={form.sortDirection} onChange={event => updateFilter('sortDirection', event.target.value)}><option value="NEWEST">Newest first</option><option value="OLDEST">Oldest first</option></select></label>
        <div className="filter-actions"><button type="button" className="button" onClick={clearFilters}>Clear</button><button type="submit" className="button">Apply filters</button></div>
        </div>
      </form>

      <div className="saved-search-bar">
        <div className="saved-search-list" aria-label="Saved searches">
          {savedSearches.map(saved => <span className="saved-search" key={saved.id}><button type="button" onClick={() => applySavedSearch(saved)}>{saved.name}</button><button type="button" className="saved-search-delete" aria-label={`Delete ${saved.name}`} onClick={() => removeSavedSearch(saved.id)}><Trash2 size={12} /></button></span>)}
          {savedSearches.length === 0 && <span className="saved-search-empty">No saved searches</span>}
        </div>
        <form className="save-search-form" onSubmit={saveCurrentSearch}><input aria-label="Saved search name" value={saveName} maxLength={120} onChange={event => setSaveName(event.target.value)} placeholder="Search name" /><button className="button" disabled={saving || !saveName.trim()}><BookmarkPlus size={14} />Save</button></form>
      </div>
      {savedSearchError && <p className="saved-search-error" role="alert">{savedSearchError}</p>}

      <div className="log-results-heading">
        <div><h2>Events</h2><p>{result ? `${result.totalRecords.toLocaleString()} matching ${result.totalRecords === 1 ? 'record' : 'records'} · ${result.queryExecutionMs} ms` : 'Querying log store'}</p></div>
        {loading && result && <span className="table-loading"><RefreshCw size={12} className="spin" />Updating</span>}
      </div>

      <div className="logs-table-wrap" aria-busy={loading}>
        {error ? (
          <div className="table-state error-state"><AlertCircle size={24} /><strong>Logs could not be loaded</strong><p>{error}</p><button className="button" onClick={retry}>Try again</button></div>
        ) : loading && !result ? (
          <div className="table-state"><RefreshCw size={24} className="spin" /><strong>Loading logs</strong><p>Fetching the latest events from the log service.</p></div>
        ) : result?.content.length === 0 ? (
          <div className="table-state"><Search size={24} /><strong>No logs found</strong><p>Adjust or clear the filters to broaden this query.</p></div>
        ) : (
          <div className="table-scroll"><table className="logs-table"><thead><tr><th>Timestamp</th><th>Severity</th><th>Service</th><th>Environment</th><th>Message</th><th>Trace ID</th></tr></thead><tbody>
            {result?.content.map(log => <tr key={log.id} tabIndex={0} onClick={() => setSelectedLog(log)} onKeyDown={event => { if (event.key === 'Enter' || event.key === ' ') setSelectedLog(log) }} aria-label={`View details for ${log.message}`}>
              <td>{formatTimestamp(log.timestamp)}</td><td><span className={`severity severity-${log.severity.toLowerCase()}`}>{log.severity}</span></td><td className="service-cell">{log.serviceName}</td><td>{log.environment}</td><td className="message-cell" title={log.message}>{highlightMessage(log.message, filters.search ?? '')}</td><td><code>{log.traceId ?? '—'}</code></td>
            </tr>)}
          </tbody></table></div>
        )}
      </div>

      {result && result.totalPages > 0 && <div className="pagination"><span>Page {result.pageNumber + 1} of {result.totalPages}</span><div><button className="icon-button" title="Previous page" aria-label="Previous page" disabled={loading || result.pageNumber === 0} onClick={() => goToPage(result.pageNumber - 1)}><ChevronLeft size={16} /></button><button className="icon-button" title="Next page" aria-label="Next page" disabled={loading || result.pageNumber + 1 >= result.totalPages} onClick={() => goToPage(result.pageNumber + 1)}><ChevronRight size={16} /></button></div></div>}

      {selectedLog && <><button className="drawer-backdrop" aria-label="Close log details" onClick={() => setSelectedLog(null)} /><aside className="log-drawer" role="dialog" aria-modal="true" aria-labelledby="log-detail-title">
        <div className="drawer-header"><div><p className="eyebrow">LOG EVENT</p><h2 id="log-detail-title">Event details</h2></div><button className="icon-button" aria-label="Close details" onClick={() => setSelectedLog(null)}><X size={18} /></button></div>
        <div className="drawer-content"><span className={`severity severity-${selectedLog.severity.toLowerCase()}`}>{selectedLog.severity}</span><dl className="log-facts"><div><dt>Timestamp</dt><dd>{new Date(selectedLog.timestamp).toLocaleString()}</dd></div><div><dt>Service</dt><dd>{selectedLog.serviceName}</dd></div><div><dt>Environment</dt><dd>{selectedLog.environment}</dd></div><div><dt>Host</dt><dd>{selectedLog.host ?? 'Not recorded'}</dd></div><div className="wide"><dt>Trace ID</dt><dd><code>{selectedLog.traceId ?? 'Not recorded'}</code></dd></div></dl><section className="detail-section"><h3>Message</h3><p>{selectedLog.message}</p></section><section className="detail-section"><h3>Metadata</h3><pre>{JSON.stringify(selectedLog.metadata ?? {}, null, 2)}</pre></section></div>
      </aside></>}
    </section>
  )
}