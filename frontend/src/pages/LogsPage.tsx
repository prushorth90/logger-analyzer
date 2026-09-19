import { useEffect, useState, type FormEvent } from 'react'
import { AlertCircle, ChevronLeft, ChevronRight, RefreshCw, Search, SlidersHorizontal, X } from 'lucide-react'
import { fetchLogs, type LogEntry, type LogFilters, type LogSeverity, type PagedLogResponse } from '../api/logs'

const PAGE_SIZE = 20
const EMPTY_FILTERS = { severity: '', serviceName: '', environment: '', search: '', startDate: '', endDate: '' }
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
    search: form.search || undefined,
    startTimestamp: toTimestamp(form.startDate),
    endTimestamp: toTimestamp(form.endDate, true),
  }
}

export function LogsPage() {
  const [form, setForm] = useState<FilterForm>(EMPTY_FILTERS)
  const [filters, setFilters] = useState<LogFilters>(() => buildFilters(EMPTY_FILTERS, 0))
  const [result, setResult] = useState<PagedLogResponse | null>(null)
  const [selectedLog, setSelectedLog] = useState<LogEntry | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [requestVersion, setRequestVersion] = useState(0)

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

  return (
    <section className="page-content logs-page">
      <div className="page-heading logs-heading">
        <div><p className="eyebrow">OBSERVABILITY / LOGS</p><h1>Log explorer</h1><p>Search and inspect application events across your services.</p></div>
        <button className="button" onClick={retry} disabled={loading}>
          <RefreshCw size={14} className={loading ? 'spin' : ''} />Refresh
        </button>
      </div>

      <form className="log-filters" onSubmit={applyFilters}>
        <label className="search-filter"><span>Full-text search</span><div><Search size={15} /><input value={form.search} onChange={event => updateFilter('search', event.target.value)} placeholder="Message, service, trace ID…" /></div></label>
        <label><span>Severity</span><select value={form.severity} onChange={event => updateFilter('severity', event.target.value)}><option value="">All levels</option><option>DEBUG</option><option>INFO</option><option>WARN</option><option>ERROR</option></select></label>
        <label><span>Service</span><input value={form.serviceName} onChange={event => updateFilter('serviceName', event.target.value)} placeholder="Any service" /></label>
        <label><span>Environment</span><input value={form.environment} onChange={event => updateFilter('environment', event.target.value)} placeholder="Any environment" /></label>
        <label><span>Start date</span><input type="date" value={form.startDate} onChange={event => updateFilter('startDate', event.target.value)} /></label>
        <label><span>End date</span><input type="date" min={form.startDate || undefined} value={form.endDate} onChange={event => updateFilter('endDate', event.target.value)} /></label>
        <div className="filter-actions"><button type="button" className="button" onClick={clearFilters}>Clear</button><button type="submit" className="button primary"><SlidersHorizontal size={14} />Apply</button></div>
      </form>

      <div className="log-results-heading">
        <div><h2>Events</h2><p>{result ? `${result.totalRecords.toLocaleString()} log ${result.totalRecords === 1 ? 'entry' : 'entries'}` : 'Querying log store'}</p></div>
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
              <td>{formatTimestamp(log.timestamp)}</td><td><span className={`severity severity-${log.severity.toLowerCase()}`}>{log.severity}</span></td><td className="service-cell">{log.serviceName}</td><td>{log.environment}</td><td className="message-cell" title={log.message}>{log.message}</td><td><code>{log.traceId ?? '—'}</code></td>
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