import { useEffect, useState } from 'react'
import { AlertTriangle, ChevronLeft, ChevronRight, LoaderCircle, RefreshCw, RotateCcw, X } from 'lucide-react'
import { fetchDeadLetterEvents, retryDeadLetterEvent, type DeadLetterEvent, type PagedDeadLetterEventResponse } from '../api/deadLetters'

function summarizeFailure(reason: string) {
  if (reason.includes('Demo ingestion failure requested')) {
    return 'Simulated processing failure for DLQ demo'
  }
  const message = reason.split(';').map(part => part.trim()).filter(Boolean).at(-1)
  return message || reason
}

export function FailedIngestionsPage() {
  const [page, setPage] = useState(0)
  const [requestVersion, setRequestVersion] = useState(0)
  const [result, setResult] = useState<PagedDeadLetterEventResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [retryingId, setRetryingId] = useState<string | null>(null)
  const [selectedFailure, setSelectedFailure] = useState<DeadLetterEvent | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchDeadLetterEvents(page, controller.signal)
      .then(setResult)
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Unable to load failed events.')
      })
    return () => controller.abort()
  }, [page, requestVersion])

  async function retry(ingestionEventId: string) {
    setRetryingId(ingestionEventId)
    setError(null)
    try {
      await retryDeadLetterEvent(ingestionEventId)
      setRequestVersion((value) => value + 1)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to retry event.')
    } finally {
      setRetryingId(null)
    }
  }

  function changePage(nextPage: number) {
    setError(null)
    setPage(nextPage)
  }

  function refresh() {
    setError(null)
    setRequestVersion((value) => value + 1)
  }

  return <section className="page-content failed-page">
    <div className="page-heading logs-heading">
      <div><div className="eyebrow">INGESTION OPERATIONS</div><h1>Failed ingestion events</h1><p>Events exhausted by automatic processing and retained for inspection or manual replay.</p></div>
      <button className="button" onClick={refresh}><RefreshCw size={14} />Refresh</button>
    </div>

    {error && <div className="analytics-error" role="alert"><AlertTriangle size={20} /><div><strong>Dead-letter request failed</strong><p>{error}</p></div></div>}

    <div className="log-results-heading">
      <div><h2>Dead-letter queue</h2><p>{result?.totalRecords ?? 0} retained event{result?.totalRecords === 1 ? '' : 's'}</p></div>
      {!result && !error && <span className="table-loading"><LoaderCircle className="spin" size={13} />Loading</span>}
    </div>
    <div className="logs-table-wrap">
      <div className="table-scroll">
        <table className="failed-table">
          <thead><tr><th>Failed at</th><th>Service</th><th>Original event</th><th>Failure</th><th>Retries</th><th>Action</th></tr></thead>
          <tbody>
            {result?.content.map((event) => <tr key={event.id}>
              <td>{new Date(event.failedAt).toLocaleString()}</td>
              <td>{event.originalEvent.serviceName}</td>
              <td><code title={event.ingestionEventId}>{event.ingestionEventId}</code></td>
              <td><button className="failure-reason" title={event.failureReason} onClick={() => setSelectedFailure(event)}>{summarizeFailure(event.failureReason)}</button></td>
              <td>{event.retryCount} auto / {event.manualRetryCount} manual</td>
              <td><button className="button" disabled={retryingId === event.ingestionEventId || event.manualRetryCount >= 3} onClick={() => retry(event.ingestionEventId)} title="Republish this event to logs.raw"><RotateCcw size={13} />{retryingId === event.ingestionEventId ? 'Retrying' : 'Retry'}</button></td>
            </tr>)}
          </tbody>
        </table>
      </div>
      {result?.content.length === 0 && <div className="table-state"><AlertTriangle size={24} /><strong>No failed events</strong><p>The dead-letter queue projection is empty.</p></div>}
    </div>
    <div className="pagination">
      <span>Page {(result?.pageNumber ?? 0) + 1} of {Math.max(result?.totalPages ?? 1, 1)}</span>
      <div>
        <button className="icon-button" aria-label="Previous page" disabled={page === 0} onClick={() => changePage(page - 1)}><ChevronLeft size={15} /></button>
        <button className="icon-button" aria-label="Next page" disabled={!result || page + 1 >= result.totalPages} onClick={() => changePage(page + 1)}><ChevronRight size={15} /></button>
      </div>
    </div>
    {selectedFailure && <><button className="drawer-backdrop" aria-label="Close failure details" onClick={() => setSelectedFailure(null)} /><aside className="log-drawer" role="dialog" aria-modal="true" aria-labelledby="failure-detail-title">
      <div className="drawer-header"><div><p className="eyebrow">DEAD-LETTER EVENT</p><h2 id="failure-detail-title">Failure details</h2></div><button className="icon-button" aria-label="Close details" onClick={() => setSelectedFailure(null)}><X size={18} /></button></div>
      <div className="drawer-content"><dl className="log-facts"><div><dt>Service</dt><dd>{selectedFailure.originalEvent.serviceName}</dd></div><div><dt>Retries</dt><dd>{selectedFailure.retryCount} automatic / {selectedFailure.manualRetryCount} manual</dd></div><div className="wide"><dt>Event ID</dt><dd><code>{selectedFailure.ingestionEventId}</code></dd></div></dl><section className="detail-section"><h3>Summary</h3><p>{summarizeFailure(selectedFailure.failureReason)}</p></section><section className="detail-section"><h3>Full exception</h3><pre>{selectedFailure.failureReason}</pre></section><section className="detail-section"><h3>Original event</h3><pre>{JSON.stringify(selectedFailure.originalEvent, null, 2)}</pre></section></div>
    </aside></>}
  </section>
}