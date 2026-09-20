import { useEffect, useState, type FormEvent } from 'react'
import { AlertCircle, BellRing, Check, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { acknowledgeAlert, createAlertRule, fetchAlerts, fetchAlertRules, resolveAlert, setAlertRuleActive, type AlertRecord, type AlertRule, type AlertStatus, type PagedAlerts } from '../api/alerts'

const EMPTY_RULE = { name: '', serviceName: '', thresholdCount: 20, windowMinutes: 5, cooldownMinutes: 30 }

export function AlertsPage() {
  const [rules, setRules] = useState<AlertRule[]>([])
  const [alerts, setAlerts] = useState<PagedAlerts | null>(null)
  const [ruleForm, setRuleForm] = useState(EMPTY_RULE)
  const [status, setStatus] = useState<AlertStatus | ''>('')
  const [page, setPage] = useState(0)
  const [requestVersion, setRequestVersion] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([fetchAlertRules(controller.signal), fetchAlerts(status, page, controller.signal)])
      .then(([nextRules, nextAlerts]) => { setRules(nextRules); setAlerts(nextAlerts) })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Unable to load alerts.')
      })
    return () => controller.abort()
  }, [status, page, requestVersion])

  function updateRule(name: keyof typeof EMPTY_RULE, value: string) {
    setRuleForm(current => ({ ...current, [name]: name === 'name' || name === 'serviceName' ? value : Number(value) }))
  }

  async function saveRule(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const created = await createAlertRule(ruleForm)
      setRules(current => [...current, created].sort((left, right) => left.name.localeCompare(right.name)))
      setRuleForm(EMPTY_RULE)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to create alert rule.')
    } finally {
      setSaving(false)
    }
  }

  async function toggleRule(rule: AlertRule) {
    try {
      const updated = await setAlertRuleActive(rule.id, !rule.active)
      setRules(current => current.map(item => item.id === updated.id ? updated : item))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to update alert rule.')
    }
  }

  async function transition(alert: AlertRecord, action: 'acknowledge' | 'resolve') {
    setError(null)
    try {
      const updated = action === 'acknowledge' ? await acknowledgeAlert(alert.id) : await resolveAlert(alert.id)
      setAlerts(current => {
        if (!current) return current
        if (status && updated.status !== status) {
          return { ...current, content: current.content.filter(item => item.id !== updated.id), totalElements: current.totalElements - 1 }
        }
        return { ...current, content: current.content.map(item => item.id === updated.id ? updated : item) }
      })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to update alert.')
    }
  }

  return <section className="page-content alerts-page">
    <div className="page-heading logs-heading">
      <div><p className="eyebrow">OPERATIONS / ALERTS</p><h1>Alert rules</h1><p>Evaluate recent PostgreSQL logs and track actionable conditions.</p></div>
      <button className="button" onClick={() => setRequestVersion(value => value + 1)}><RefreshCw size={14} />Refresh</button>
    </div>

    {error && <div className="analytics-error" role="alert"><AlertCircle size={20} /><div><strong>Alert request failed</strong><p>{error}</p></div></div>}

    <form className="alert-rule-form" onSubmit={saveRule}>
      <label><span>Rule name</span><input required maxLength={120} value={ruleForm.name} onChange={event => updateRule('name', event.target.value)} placeholder="Payment errors" /></label>
      <label><span>Service</span><input required maxLength={255} value={ruleForm.serviceName} onChange={event => updateRule('serviceName', event.target.value)} placeholder="payment-service" /></label>
      <label><span>Error count over</span><input required type="number" min="1" value={ruleForm.thresholdCount} onChange={event => updateRule('thresholdCount', event.target.value)} /></label>
      <label><span>Window (minutes)</span><input required type="number" min="1" max="1440" value={ruleForm.windowMinutes} onChange={event => updateRule('windowMinutes', event.target.value)} /></label>
      <label><span>Cooldown (minutes)</span><input required type="number" min="1" max="10080" value={ruleForm.cooldownMinutes} onChange={event => updateRule('cooldownMinutes', event.target.value)} /></label>
      <button className="button primary" disabled={saving}><BellRing size={14} />Create rule</button>
    </form>

    <section className="alert-rules-strip" aria-label="Configured alert rules">
      {rules.map(rule => <article key={rule.id} className="alert-rule-item"><div><strong>{rule.name}</strong><p>ERROR count for <code>{rule.serviceName}</code> &gt; {rule.thresholdCount} during {rule.windowMinutes} minutes</p></div><button className={`alert-rule-toggle ${rule.active ? 'active' : ''}`} onClick={() => toggleRule(rule)}>{rule.active ? 'Active' : 'Paused'}</button></article>)}
      {rules.length === 0 && <p className="alert-empty">No alert rules configured.</p>}
    </section>

    <div className="alert-list-heading"><div><h2>Alert history</h2><p>{alerts?.totalElements ?? 0} alert records</p></div><label><span>Status</span><select value={status} onChange={event => { setStatus(event.target.value as AlertStatus | ''); setPage(0) }}><option value="">All</option><option>OPEN</option><option>ACKNOWLEDGED</option><option>RESOLVED</option></select></label></div>
    <div className="logs-table-wrap">
      <div className="table-scroll"><table className="alerts-table"><thead><tr><th>Opened</th><th>Rule</th><th>Service</th><th>Condition</th><th>Observed</th><th>Status</th><th>Actions</th></tr></thead><tbody>
        {alerts?.content.map(alert => <tr key={alert.id} className={`alert-row-${alert.status.toLowerCase()}`}><td>{new Date(alert.openedAt).toLocaleString()}</td><td>{alert.ruleName}</td><td><code>{alert.serviceName}</code></td><td>ERROR &gt; {alert.thresholdCount} / {alert.windowMinutes}m</td><td>{alert.observedCount}</td><td><span className={`alert-status alert-status-${alert.status.toLowerCase()}`}>{alert.status}</span></td><td><div className="alert-actions">{alert.status === 'OPEN' && <button className="button" onClick={() => transition(alert, 'acknowledge')}><Check size={13} />Acknowledge</button>}{alert.status !== 'RESOLVED' && <button className="button" onClick={() => transition(alert, 'resolve')}>Resolve</button>}</div></td></tr>)}
      </tbody></table></div>
      {alerts?.content.length === 0 && <div className="table-state"><BellRing size={24} /><strong>No alerts</strong><p>No alert records match this status.</p></div>}
    </div>
    <div className="pagination"><span>Page {(alerts?.number ?? 0) + 1} of {Math.max(alerts?.totalPages ?? 1, 1)}</span><div><button className="icon-button" aria-label="Previous page" disabled={page === 0} onClick={() => setPage(value => value - 1)}><ChevronLeft size={15} /></button><button className="icon-button" aria-label="Next page" disabled={!alerts || page + 1 >= alerts.totalPages} onClick={() => setPage(value => value + 1)}><ChevronRight size={15} /></button></div></div>
  </section>
}