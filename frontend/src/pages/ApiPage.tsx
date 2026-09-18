import { Check, Copy } from 'lucide-react'
import { useState } from 'react'
import { RefreshButton } from '../components/RefreshButton'
import type { HealthState } from '../hooks/useHealth'

export function ApiPage({ health }: { health: HealthState }) {
  const [copied, setCopied] = useState(false)
  const [copyError, setCopyError] = useState(false)
  const { latest, loading, refresh } = health

  async function copyEndpoint() {
    try {
      await navigator.clipboard.writeText(`${window.location.origin}/api/health`)
      setCopied(true)
      setCopyError(false)
    } catch {
      setCopyError(true)
    }
  }

  return <div className="page-content">
    <div className="page-heading"><div><div className="eyebrow">SYSTEM / API</div><h1>Health API</h1><p>Backend and database availability.</p></div><RefreshButton loading={loading} onRefresh={refresh} /></div>
    <div className="endpoint-toolbar"><code><span className="http-method">GET</span>/api/health</code><button className="button" onClick={() => void copyEndpoint()} title="Copy endpoint URL">{copied ? <Check size={14} /> : <Copy size={14} />}{copied ? 'Copied' : 'Copy URL'}</button></div>
    {copyError && <p role="alert">Clipboard access is unavailable.</p>}
    <div className="response-heading"><h2>Response</h2><span>{latest?.statusCode ? `HTTP ${latest.statusCode}` : 'No response'}{latest && ` / ${latest.duration} ms`}</span></div>
    <pre className="response-body" aria-live="polite">{latest?.data ? JSON.stringify(latest.data, null, 2) : latest?.error ?? 'Waiting for response...'}</pre>
    <dl className="api-facts"><div><dt>CONTENT TYPE</dt><dd>application/json</dd></div><div><dt>HEALTHY</dt><dd>200 OK</dd></div><div><dt>DATABASE UNAVAILABLE</dt><dd>503 Service Unavailable</dd></div></dl>
  </div>
}