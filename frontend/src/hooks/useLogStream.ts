import { useEffect, useEffectEvent, useState } from 'react'
import type { LogEntry } from '../api/logs'

export type LogStreamStatus = 'connecting' | 'connected' | 'reconnecting' | 'disconnected'

export type LiveLogEntry = Omit<LogEntry, 'host' | 'metadata'>

function isLiveLogEntry(value: unknown): value is LiveLogEntry {
  if (!value || typeof value !== 'object') return false
  const data = value as Record<string, unknown>
  return typeof data.id === 'string'
    && typeof data.timestamp === 'string'
    && typeof data.serviceName === 'string'
    && typeof data.environment === 'string'
    && typeof data.severity === 'string'
    && typeof data.message === 'string'
}

export function useLogStream(onLog: (log: LiveLogEntry) => void) {
  const [status, setStatus] = useState<LogStreamStatus>(() => typeof EventSource === 'undefined' ? 'disconnected' : 'connecting')
  const onLiveLog = useEffectEvent(onLog)

  useEffect(() => {
    if (typeof EventSource === 'undefined') {
      return
    }

    let source: EventSource | null = null
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null
    let stopped = false
    let retryCount = 0

    function connect() {
      if (stopped) return
      source = new EventSource('/api/logs/stream')
      source.addEventListener('status', () => {
        retryCount = 0
        setStatus('connected')
      })
      source.addEventListener('log', event => {
        try {
          const value: unknown = JSON.parse((event as MessageEvent).data)
          if (isLiveLogEntry(value)) onLiveLog(value)
        } catch {
          // Ignore malformed stream events and keep the connection alive.
        }
      })
      source.onerror = () => {
        source?.close()
        if (stopped) return
        retryCount += 1
        setStatus('reconnecting')
        reconnectTimer = setTimeout(connect, Math.min(1_000 * 2 ** (retryCount - 1), 15_000))
      }
    }

    connect()
    return () => {
      stopped = true
      source?.close()
      if (reconnectTimer) clearTimeout(reconnectTimer)
    }
  }, [])

  return { status }
}