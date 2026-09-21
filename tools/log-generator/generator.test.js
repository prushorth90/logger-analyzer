import assert from 'node:assert/strict'
import { createServer } from 'node:http'
import test from 'node:test'

import { createDlqDemoLog, createLog, createTraceDemoLogs, run } from './generator.js'

const messages = {
  DEBUG: ['debug {operation}'],
  INFO: ['info {operation}'],
  WARN: ['warn {operation}'],
  ERROR: ['error {operation}'],
}

test('createLog creates a valid API payload', () => {
  const log = createLog({
    environment: 'test',
    messages,
    scenario: 'normal',
    services: ['order-service'],
  })

  assert.equal(log.serviceName, 'order-service')
  assert.equal(log.environment, 'test')
  assert.match(log.traceId, /^[a-f0-9]{32}$/)
  assert.match(log.host, /^order-service-0[1-4]$/)
  assert.ok(['DEBUG', 'INFO', 'WARN', 'ERROR'].includes(log.severity))
  assert.equal(log.metadata.scenario, 'normal')
  assert.ok(!Number.isNaN(Date.parse(log.timestamp)))
})

test('run posts generated logs at the configured rate', async () => {
  const requests = []
  const server = createServer((request, response) => {
    let body = ''
    request.setEncoding('utf8')
    request.on('data', chunk => { body += chunk })
    request.on('end', () => {
      requests.push({ method: request.method, body: JSON.parse(body) })
      response.writeHead(201).end()
    })
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))

  try {
    const address = server.address()
    const result = await run({
      apiUrl: `http://127.0.0.1:${address.port}/api/logs`,
      durationSeconds: 0.35,
      environment: 'test',
      messages,
      requestsPerSecond: 10,
      scenario: 'normal',
      services: ['user-service'],
    })

    assert.equal(result.failed, 0)
    assert.ok(result.sent >= 2)
    assert.equal(requests.length, result.sent)
    assert.ok(requests.every(request => request.method === 'POST'))
    assert.ok(requests.every(request => request.body.serviceName === 'user-service'))
  } finally {
    await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()))
  }
})

test('createTraceDemoLogs creates an ordered shared trace across four services', () => {
  const startedAt = new Date('2026-09-20T12:00:00.000Z')
  const logs = createTraceDemoLogs({ environment: 'demo', traceId: 'trace-recording-001' }, startedAt)

  assert.deepEqual(logs.map(log => log.serviceName), [
    'api-gateway',
    'order-service',
    'payment-service',
    'inventory-service',
  ])
  assert.deepEqual(logs.map(log => log.timestamp), [
    '2026-09-20T12:00:00.000Z',
    '2026-09-20T12:00:00.250Z',
    '2026-09-20T12:00:00.500Z',
    '2026-09-20T12:00:00.750Z',
  ])
  assert.ok(logs.every(log => log.traceId === 'trace-recording-001'))
  assert.equal(logs[2].severity, 'ERROR')
  assert.equal(logs[2].serviceName, 'payment-service')
  assert.deepEqual(logs.map(log => log.metadata.sequence), [1, 2, 3, 4])
})

test('trace-demo posts exactly four deterministic events and stops', async () => {
  const requests = []
  const server = createServer((request, response) => {
    let body = ''
    request.setEncoding('utf8')
    request.on('data', chunk => { body += chunk })
    request.on('end', () => {
      requests.push({ method: request.method, body: JSON.parse(body) })
      response.writeHead(202).end()
    })
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))

  try {
    const address = server.address()
    const result = await run({
      apiUrl: `http://127.0.0.1:${address.port}/api/logs`,
      environment: 'demo',
      scenario: 'trace-demo',
      traceId: 'trace-recording-001',
    })

    assert.deepEqual(result, { sent: 4, failed: 0 })
    assert.equal(requests.length, 4)
    assert.ok(requests.every(request => request.method === 'POST'))
    assert.ok(requests.every(request => request.body.traceId === 'trace-recording-001'))
    assert.deepEqual(requests.map(request => request.body.serviceName), [
      'api-gateway', 'order-service', 'payment-service', 'inventory-service',
    ])
  } finally {
    await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()))
  }
})

test('createDlqDemoLog creates one isolated retryable failure marker', () => {
  const log = createDlqDemoLog(
    { environment: 'demo', traceId: 'demo-dlq-test-001' },
    new Date('2026-09-20T12:00:00.000Z'),
  )

  assert.equal(log.timestamp, '2026-09-20T12:00:00.000Z')
  assert.equal(log.serviceName, 'dlq-demo-service')
  assert.equal(log.severity, 'ERROR')
  assert.equal(log.traceId, 'demo-dlq-test-001')
  assert.equal(log.metadata.scenario, 'dlq-demo')
  assert.equal(log.metadata.demoFailure, 'retry-to-dlq')
})

test('dlq-demo posts exactly one event and prints the accepted event ID', async () => {
  const requests = []
  const server = createServer((request, response) => {
    let body = ''
    request.setEncoding('utf8')
    request.on('data', chunk => { body += chunk })
    request.on('end', () => {
      requests.push(JSON.parse(body))
      response.writeHead(202, { 'content-type': 'application/json' })
        .end(JSON.stringify({ eventId: 'demo-event-id', status: 'accepted' }))
    })
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))

  try {
    const address = server.address()
    const result = await run({
      apiUrl: `http://127.0.0.1:${address.port}/api/logs`,
      environment: 'demo',
      scenario: 'dlq-demo',
      traceId: 'demo-dlq-test-001',
    })

    assert.deepEqual(result, { sent: 1, failed: 0, eventId: 'demo-event-id' })
    assert.equal(requests.length, 1)
    assert.equal(requests[0].metadata.demoFailure, 'retry-to-dlq')
  } finally {
    await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()))
  }
})