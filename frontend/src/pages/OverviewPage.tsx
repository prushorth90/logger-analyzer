import { ArrowRight, ArrowUpRight, CircleCheck, CircleDashed, Clock3, Database, Globe, Info, Server, TriangleAlert } from 'lucide-react'
import { Link } from 'react-router-dom'
import { RefreshButton } from '../components/RefreshButton'
import { StatusBadge } from '../components/StatusBadge'
import type { HealthState } from '../hooks/useHealth'

export function OverviewPage({ health }: { health: HealthState }) {
  const { latest, loading, checks, autoRefresh, setAutoRefresh, refresh } = health
  const state = !latest ? 'pending' : latest.data?.status === 'UP' ? 'up' : 'down'
  const backendState = !latest ? 'pending' : latest.data ? 'up' : 'down'
  const databaseState = !latest || !latest.data ? 'pending' : latest.data.database === 'UP' ? 'up' : 'down'
  const BannerIcon = state === 'up' ? CircleCheck : state === 'down' ? TriangleAlert : CircleDashed

  return (
    <div className="page-content">
      <div className="page-heading"><div><div className="eyebrow">SYSTEM / OVERVIEW</div><h1>System overview</h1><p>Your local stack, at a glance.</p></div><RefreshButton loading={loading} onRefresh={refresh} /></div>
      <section className={`health-banner ${state}`} role="status" aria-live="polite">
        <BannerIcon size={29} />
        <div><h2>{state === 'up' ? 'All systems operational' : state === 'pending' ? 'Connecting to your stack' : latest?.data ? 'Database connection unavailable' : 'Backend disconnected'}</h2><p>{state === 'up' ? 'Backend connected. PostgreSQL is responding.' : state === 'pending' ? 'Waiting for the first health response.' : latest?.error ?? 'The backend is reachable, but PostgreSQL is not responding.'}</p></div>
        <StatusBadge state={state}>{state === 'up' ? 'Healthy' : state === 'pending' ? 'Checking' : 'Attention needed'}</StatusBadge>
      </section>
      <div className="service-grid">
        <article className="service-card"><div className="service-card-top"><span className="service-icon"><Globe size={19} /></span><StatusBadge state="up">Running</StatusBadge></div><h3>Frontend</h3><p>React + TypeScript</p><div className="service-card-bottom"><code>Browser client</code><span>Vite</span></div></article>
        <article className="service-card"><div className="service-card-top"><span className="service-icon backend"><Server size={19} /></span><StatusBadge state={backendState}>{backendState === 'up' ? 'Connected' : backendState === 'pending' ? 'Checking' : 'Disconnected'}</StatusBadge></div><h3>Backend</h3><p>Spring Boot + Java 21</p><div className="service-card-bottom"><code>/api/health</code><span>{latest?.data ? `${latest.duration} ms` : 'HTTP API'}</span></div></article>
        <article className="service-card"><div className="service-card-top"><span className="service-icon database"><Database size={19} /></span><StatusBadge state={databaseState}>{databaseState === 'up' ? 'Connected' : databaseState === 'down' ? 'Unavailable' : 'Unknown'}</StatusBadge></div><h3>Database</h3><p>PostgreSQL 17</p><div className="service-card-bottom"><code>Primary database</code><span>Persistent</span></div></article>
      </div>
      <section className="section"><div className="section-heading"><div><h2>Connection path</h2><p>One request. Three connected services.</p></div><Link className="subtle-link" to="/api-details">Inspect API<ArrowUpRight size={13} /></Link></div>
        <div className="connection-flow"><div className="flow-node"><Globe size={24} /><div><strong>React frontend</strong><span>Same-origin proxy</span></div></div><div className="flow-line">HTTP / JSON<div /></div><div className="flow-node"><Server size={24} /><div><strong>Spring Boot</strong><span>/api/health</span></div></div><div className="flow-line">JDBC / SQL<div /></div><div className="flow-node"><Database size={24} /><div><strong>PostgreSQL</strong><span>SELECT 1</span></div></div></div>
      </section>
      <section className="section"><div className="section-heading"><div><h2>Recent health checks</h2><p>Live responses from your backend.</p></div><div className="check-controls"><label><input type="checkbox" checked={autoRefresh} onChange={(event) => setAutoRefresh(event.target.checked)} />Auto-refresh</label><span>{autoRefresh ? 'Every 30 seconds' : 'Paused'}</span></div></div>
        <div className="table-scroll"><table><thead><tr><th>TIME</th><th>REQUEST</th><th>STATUS</th><th>RESPONSE TIME</th></tr></thead><tbody>{checks.length === 0 ? <tr><td colSpan={4} className="empty-row">Waiting for health checks...</td></tr> : checks.map((check, index) => <tr key={`${check.checkedAt.getTime()}-${index}`}><td>{check.checkedAt.toLocaleTimeString([], { hour12: false })}</td><td><span className="http-method">GET</span><span className="request-path">/api/health</span></td><td><StatusBadge state={check.data?.status === 'UP' ? 'up' : 'down'}>{check.data?.status === 'UP' ? '200 OK' : check.statusCode ? `${check.statusCode}${check.statusCode === 503 ? ' Unavailable' : ' Invalid response'}` : 'Unreachable'}</StatusBadge></td><td>{check.duration} ms</td></tr>)}</tbody></table></div>
        <p className="table-note"><Info size={11} />Last {checks.length} of up to 8 checks in this session.</p>
      </section>
      <div className="section-heading section"><span className="subtle-link"><Clock3 size={12} />{latest ? `Last checked at ${latest.checkedAt.toLocaleTimeString()}` : 'No completed checks'}</span><Link to="/api-details" className="subtle-link">Response details<ArrowRight size={12} /></Link></div>
    </div>
  )
}