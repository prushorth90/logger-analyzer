import { useEffect, useState } from 'react'
import { AlertCircle, ArrowLeft, ChevronRight, Clock3, RefreshCw, Route } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { fetchTrace, type TraceResponse } from '../api/traces'

function formatTimestamp(timestamp: string) {
  return new Intl.DateTimeFormat(undefined, {
    hour: '2-digit', minute: '2-digit', second: '2-digit', fractionalSecondDigits: 3,
  }).format(new Date(timestamp))
}

function formatDuration(durationMs: number) {
  return durationMs < 1000 ? `${durationMs} ms` : `${(durationMs / 1000).toFixed(2)} s`
}

export function TracePage() {
  const { traceId = '' } = useParams()
  const [trace, setTrace] = useState<TraceResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [requestVersion, setRequestVersion] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchTrace(traceId, controller.signal)
      .then(setTrace)
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Unable to load trace.')
      })
    return () => controller.abort()
  }, [traceId, requestVersion])

  function refresh() {
    setError(null)
    setRequestVersion(value => value + 1)
  }

  return <section className="page-content trace-page">
    <div className="trace-toolbar"><Link className="subtle-link" to="/"><ArrowLeft size={14} />Back to logs</Link><button className="button" onClick={refresh}><RefreshCw size={14} />Refresh</button></div>
    <div className="page-heading trace-heading">
      <div><p className="eyebrow">LOG CORRELATION</p><h1>Trace sequence</h1><p><code>{traceId}</code></p></div>
      {trace && <div className="trace-summary"><span><Route size={15} />{trace.events.length} events</span><span><Clock3 size={15} />{formatDuration(trace.durationMs)}</span></div>}
    </div>

    {error ? <div className="analytics-error" role="alert"><AlertCircle size={20} /><div><strong>Trace could not be loaded</strong><p>{error}</p></div></div>
      : !trace ? <div className="table-state"><RefreshCw className="spin" size={24} /><strong>Loading trace</strong><p>Correlating PostgreSQL log events.</p></div>
      : <>
        <section className="trace-flow" aria-label="Service sequence">
          {trace.serviceSequence.map((service, index) => <span className="trace-flow-step" key={`${service}-${index}`}><strong>{service}</strong>{index < trace.serviceSequence.length - 1 && <ChevronRight size={16} />}</span>)}
        </section>
        <div className="trace-timeline">
          {trace.events.map((event, index) => <article className={`trace-event trace-event-${event.severity.toLowerCase()}`} key={event.id}>
            <div className="trace-rail"><span>{index + 1}</span></div>
            <div className="trace-event-body">
              <header><div><strong>{event.serviceName}</strong><span>{event.environment}</span></div><time dateTime={event.timestamp}>{formatTimestamp(event.timestamp)}</time></header>
              <div className="trace-event-message"><span className={`severity severity-${event.severity.toLowerCase()}`}>{event.severity}</span><p>{event.message}</p></div>
              <footer><code>{event.host ?? 'unknown host'}</code></footer>
            </div>
          </article>)}
        </div>
      </>}
  </section>
}