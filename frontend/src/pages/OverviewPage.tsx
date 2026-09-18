import { useEffect, useState } from 'react'
import { Activity, AlertCircle, Boxes, CircleCheck, CircleDashed, RefreshCw, TriangleAlert } from 'lucide-react'
import { fetchLogOverview, type LogOverview, type NamedCount, type TimeCount } from '../api/overview'
import { StatusBadge } from '../components/StatusBadge'
import type { HealthState } from '../hooks/useHealth'

const PERIODS = [
  { label: '1 hour', hours: 1 },
  { label: '6 hours', hours: 6 },
  { label: '24 hours', hours: 24 },
  { label: '7 days', hours: 168 },
]

const numberFormat = new Intl.NumberFormat()

function TimelineChart({ points }: { points: TimeCount[] }) {
  if (points.length === 0) return <div className="chart-empty">No logs in this period</div>
  const width = 640
  const height = 190
  const max = Math.max(...points.map(point => point.count), 1)
  const coordinates = points.map((point, index) => ({
    ...point,
    x: points.length === 1 ? width / 2 : index * width / (points.length - 1),
    y: height - (point.count / max) * (height - 24) - 12,
  }))
  const line = coordinates.map(point => `${point.x},${point.y}`).join(' ')
  return (
    <div className="timeline-chart">
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Log volume over time">
        <line x1="0" y1={height - 12} x2={width} y2={height - 12} className="chart-axis" />
        <polyline points={line} className="chart-line" />
        {coordinates.map(point => <circle key={point.timestamp} cx={point.x} cy={point.y} r="4"><title>{`${new Date(point.timestamp).toLocaleString()}: ${point.count} logs`}</title></circle>)}
      </svg>
      <div className="chart-range"><span>{new Date(points[0].timestamp).toLocaleString([], { month: 'short', day: 'numeric', hour: 'numeric' })}</span><span>{new Date(points.at(-1)!.timestamp).toLocaleString([], { month: 'short', day: 'numeric', hour: 'numeric' })}</span></div>
    </div>
  )
}

function ErrorBars({ items }: { items: NamedCount[] }) {
  if (items.length === 0) return <div className="chart-empty">No errors in this period</div>
  const max = Math.max(...items.map(item => item.count), 1)
  return <div className="error-bars">{items.map(item => <div className="error-bar" key={item.name}><div><span>{item.name}</span><strong>{numberFormat.format(item.count)}</strong></div><div className="bar-track"><span style={{ width: `${item.count / max * 100}%` }} /></div></div>)}</div>
}

export function OverviewPage({ health }: { health: HealthState }) {
  const { latest, refresh: refreshHealth } = health
  const [periodHours, setPeriodHours] = useState(24)
  const [overview, setOverview] = useState<LogOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [requestVersion, setRequestVersion] = useState(0)
  const state = !latest ? 'pending' : latest.data?.status === 'UP' ? 'up' : 'down'
  const BannerIcon = state === 'up' ? CircleCheck : state === 'down' ? TriangleAlert : CircleDashed

  useEffect(() => {
    const controller = new AbortController()
    const end = new Date()
    const start = new Date(end.getTime() - periodHours * 60 * 60 * 1000)
    fetchLogOverview(start.toISOString(), end.toISOString(), controller.signal)
      .then(setOverview)
      .catch((requestError: unknown) => {
        if (requestError instanceof Error && requestError.name !== 'AbortError') setError(requestError.message)
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [periodHours, requestVersion])

  const changePeriod = (hours: number) => {
    setLoading(true)
    setError(null)
    setPeriodHours(hours)
  }

  const refresh = () => {
    setLoading(true)
    setError(null)
    refreshHealth()
    setRequestVersion(version => version + 1)
  }

  return (
    <div className="page-content dashboard-page">
      <div className="page-heading"><div><div className="eyebrow">LOGS / OVERVIEW</div><h1>Operational overview</h1><p>Volume, severity, and service health at a glance.</p></div><div className="dashboard-actions"><label><span>Time period</span><select aria-label="Time period" value={periodHours} onChange={event => changePeriod(Number(event.target.value))}>{PERIODS.map(period => <option key={period.hours} value={period.hours}>{period.label}</option>)}</select></label><button className="button" onClick={refresh} disabled={loading}><RefreshCw size={14} className={loading ? 'spin' : ''} />Refresh</button></div></div>
      <section className={`health-banner ${state}`} role="status" aria-live="polite">
        <BannerIcon size={22} />
        <div><h2>{state === 'up' ? 'Ingestion stack operational' : state === 'pending' ? 'Checking ingestion stack' : latest?.data ? 'PostgreSQL unavailable' : 'Backend disconnected'}</h2><p>{state === 'up' ? `Spring Boot and PostgreSQL healthy · ${latest?.duration} ms` : state === 'pending' ? 'Waiting for the health endpoint.' : latest?.error ?? 'The backend is reachable, but PostgreSQL is not responding.'}</p></div>
        <StatusBadge state={state}>{state === 'up' ? 'Healthy' : state === 'pending' ? 'Checking' : 'Attention needed'}</StatusBadge>
      </section>
      {error && !overview ? <section className="analytics-error"><AlertCircle size={20} /><div><strong>Overview unavailable</strong><p>{error}. Check that the backend is running, then refresh.</p></div></section> : <>
        <div className={`metric-grid ${loading ? 'loading' : ''}`} aria-busy={loading}>
          <article className="metric-card"><span className="metric-icon total"><Activity size={18} /></span><div><span>Total logs</span><strong>{numberFormat.format(overview?.totalLogs ?? 0)}</strong></div></article>
          <article className="metric-card"><span className="metric-icon errors"><AlertCircle size={18} /></span><div><span>Errors</span><strong>{numberFormat.format(overview?.errorCount ?? 0)}</strong></div></article>
          <article className="metric-card"><span className="metric-icon warnings"><TriangleAlert size={18} /></span><div><span>Warnings</span><strong>{numberFormat.format(overview?.warningCount ?? 0)}</strong></div></article>
          <article className="metric-card"><span className="metric-icon services"><Boxes size={18} /></span><div><span>Active services</span><strong>{numberFormat.format(overview?.activeServices ?? 0)}</strong></div></article>
        </div>
        <div className="chart-grid">
          <section className="chart-panel"><div className="chart-heading"><div><h2>Log volume</h2><p>Hourly events across the selected period</p></div><span>{numberFormat.format(overview?.totalLogs ?? 0)} events</span></div><TimelineChart points={overview?.logsOverTime ?? []} /><div className="severity-key">{overview?.logsBySeverity.map(item => <span key={item.name}><i className={`severity-dot ${item.name.toLowerCase()}`} />{item.name} <strong>{numberFormat.format(item.count)}</strong></span>)}</div></section>
          <section className="chart-panel"><div className="chart-heading"><div><h2>Errors by service</h2><p>Services contributing error events</p></div><span>{overview?.errorsByService.length ?? 0} affected</span></div><ErrorBars items={overview?.errorsByService ?? []} /></section>
        </div>
        <section className="service-volume"><div className="chart-heading"><div><h2>Service volume</h2><p>All events grouped by source service</p></div></div><div className="service-volume-list">{overview?.logsByService.length ? overview.logsByService.map(item => <div key={item.name}><span>{item.name}</span><strong>{numberFormat.format(item.count)}</strong></div>) : <p className="chart-empty">No active services in this period</p>}</div></section>
      </>}
    </div>
  )
}