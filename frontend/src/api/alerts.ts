export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED'

export interface AlertRule {
  id: string
  name: string
  serviceName: string
  thresholdCount: number
  windowMinutes: number
  cooldownMinutes: number
  active: boolean
  createdAt: string
}

export interface CreateAlertRule {
  name: string
  serviceName: string
  thresholdCount: number
  windowMinutes: number
  cooldownMinutes: number
}

export interface AlertRecord {
  id: string
  ruleId: string
  ruleName: string
  serviceName: string
  status: AlertStatus
  thresholdCount: number
  observedCount: number
  windowMinutes: number
  windowStartedAt: string
  windowEndedAt: string
  openedAt: string
  acknowledgedAt: string | null
  resolvedAt: string | null
  resolutionReason: string | null
}

export interface PagedAlerts {
  content: AlertRecord[]
  number: number
  size: number
  totalPages: number
  totalElements: number
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    cache: 'no-store',
    headers: { Accept: 'application/json', ...(init?.body ? { 'Content-Type': 'application/json' } : {}) },
    ...init,
  })
  if (!response.ok) throw new Error(`Alert request failed (HTTP ${response.status}).`)
  return response.json() as Promise<T>
}

export function fetchAlertRules(signal?: AbortSignal) {
  return request<AlertRule[]>('/api/alerts/rules', { signal })
}

export function createAlertRule(rule: CreateAlertRule) {
  return request<AlertRule>('/api/alerts/rules', { method: 'POST', body: JSON.stringify(rule) })
}

export function setAlertRuleActive(id: string, active: boolean) {
  return request<AlertRule>(`/api/alerts/rules/${encodeURIComponent(id)}/active?active=${active}`, { method: 'PATCH' })
}

export function fetchAlerts(status: AlertStatus | '', page: number, signal?: AbortSignal) {
  const params = new URLSearchParams({ page: String(page), size: '20' })
  if (status) params.set('status', status)
  return request<PagedAlerts>(`/api/alerts?${params}`, { signal })
}

export function acknowledgeAlert(id: string) {
  return request<AlertRecord>(`/api/alerts/${encodeURIComponent(id)}/acknowledge`, { method: 'POST' })
}

export function resolveAlert(id: string) {
  return request<AlertRecord>(`/api/alerts/${encodeURIComponent(id)}/resolve`, { method: 'POST' })
}