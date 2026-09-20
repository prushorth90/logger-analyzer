import { act, cleanup, renderHook } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { useLogStream } from './useLogStream'

class FakeEventSource {
  static instances: FakeEventSource[] = []
  readonly url: string
  onerror: (() => void) | null = null
  listeners = new Map<string, (event: MessageEvent) => void>()
  close = vi.fn()

  constructor(url: string) {
    this.url = url
    FakeEventSource.instances.push(this)
  }

  addEventListener(name: string, listener: EventListenerOrEventListenerObject) {
    this.listeners.set(name, listener as (event: MessageEvent) => void)
  }

  emit(name: string, data = '') {
    this.listeners.get(name)?.({ data } as MessageEvent)
  }
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.useRealTimers()
  FakeEventSource.instances = []
})

it('receives compact logs and reconnects with backoff after a dropped stream', () => {
  vi.useFakeTimers()
  vi.stubGlobal('EventSource', FakeEventSource)
  const onLog = vi.fn()
  const { result } = renderHook(() => useLogStream(onLog))
  const first = FakeEventSource.instances[0]

  act(() => first.emit('status'))
  expect(result.current.status).toBe('connected')

  act(() => first.emit('log', JSON.stringify({
    id: 'new-log', timestamp: '2026-09-19T12:00:00Z', serviceName: 'orders-api',
    environment: 'production', severity: 'INFO', message: 'Order accepted', traceId: 'trace-42',
  })))
  expect(onLog).toHaveBeenCalledWith(expect.objectContaining({ id: 'new-log' }))

  act(() => first.onerror?.())
  expect(result.current.status).toBe('reconnecting')
  expect(first.close).toHaveBeenCalled()

  act(() => vi.advanceTimersByTime(1_000))
  expect(FakeEventSource.instances).toHaveLength(2)
  act(() => FakeEventSource.instances[1].emit('status'))
  expect(result.current.status).toBe('connected')
})