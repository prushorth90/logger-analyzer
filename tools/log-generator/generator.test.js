import assert from 'node:assert/strict'
import { createServer } from 'node:http'
import test from 'node:test'

import { createLog, run } from './generator.js'

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