import { useEffect, useState } from 'react'
import { fetchHealth } from '../api/health'
import type { HealthCheck } from '../api/health'

export function useHealth() {
  const [checks, setChecks] = useState<HealthCheck[]>([])
  const [completedRequestId, setCompletedRequestId] = useState(-1)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [requestId, setRequestId] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    void fetchHealth(controller.signal).then((check) => {
      if (controller.signal.aborted) return
      setChecks((previous) => [check, ...previous].slice(0, 8))
      setCompletedRequestId(requestId)
    })
    return () => controller.abort()
  }, [requestId])

  useEffect(() => {
    if (!autoRefresh) return
    const interval = window.setInterval(() => setRequestId((previous) => previous + 1), 30_000)
    return () => window.clearInterval(interval)
  }, [autoRefresh])

  return { checks, latest: checks[0], loading: requestId !== completedRequestId, autoRefresh, setAutoRefresh, refresh: () => setRequestId((previous) => previous + 1) }
}

export type HealthState = ReturnType<typeof useHealth>