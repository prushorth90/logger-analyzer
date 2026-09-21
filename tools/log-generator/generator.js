#!/usr/bin/env node

import { randomBytes } from 'node:crypto'
import { readFile } from 'node:fs/promises'
import { pathToFileURL } from 'node:url'

const DEFAULT_SERVICES = [
  'payment-service',
  'order-service',
  'user-service',
  'inventory-service',
]

const DEFAULT_MESSAGES = {
  DEBUG: [
    'Cache lookup completed in {durationMs} ms',
    'Request validation completed for {operation}',
    'Connection returned to the database pool',
  ],
  INFO: [
    'Completed {operation} successfully in {durationMs} ms',
    'Processed request with status 200',
    'Published {operation} event to downstream consumers',
    'Health check completed successfully',
  ],
  WARN: [
    'Downstream request exceeded the {thresholdMs} ms warning threshold',
    'Retrying {operation} after a transient failure',
    'Database connection pool utilization is elevated',
    'Rate limit is approaching its configured threshold',
  ],
  ERROR: [
    'Failed to complete {operation} after 3 attempts',
    'Database query timed out after {thresholdMs} ms',
    'Downstream service returned status 503',
    'Unexpected error while processing request',
  ],
}

const SCENARIOS = {
  normal: { DEBUG: 8, INFO: 82, WARN: 8, ERROR: 2 },
  warnings: { DEBUG: 3, INFO: 37, WARN: 55, ERROR: 5 },
  'database-timeouts': { DEBUG: 0, INFO: 15, WARN: 25, ERROR: 60 },
  'payment-failures': { DEBUG: 0, INFO: 15, WARN: 20, ERROR: 65 },
  'error-spike': { DEBUG: 6, INFO: 78, WARN: 12, ERROR: 4 },
}

const TRACE_DEMO_ID = 'demo-checkout-trace-001'
const TRACE_DEMO_EVENTS = [
  {
    offsetMs: 0,
    serviceName: 'api-gateway',
    severity: 'INFO',
    message: 'Checkout request accepted',
    host: 'api-gateway-01',
    operation: 'checkout request',
  },
  {
    offsetMs: 250,
    serviceName: 'order-service',
    severity: 'INFO',
    message: 'Order created and payment requested',
    host: 'order-service-01',
    operation: 'order creation',
  },
  {
    offsetMs: 500,
    serviceName: 'payment-service',
    severity: 'ERROR',
    message: 'Payment authorization failed after 3 attempts',
    host: 'payment-service-01',
    operation: 'payment authorization',
  },
  {
    offsetMs: 750,
    serviceName: 'inventory-service',
    severity: 'INFO',
    message: 'Inventory reservation released after payment failure',
    host: 'inventory-service-01',
    operation: 'stock release',
  },
]

const OPERATIONS = {
  'payment-service': ['payment authorization', 'payment capture', 'refund'],
  'order-service': ['order creation', 'order fulfillment', 'order cancellation'],
  'user-service': ['user authentication', 'profile update', 'session refresh'],
  'inventory-service': ['stock reservation', 'inventory reconciliation', 'stock release'],
}

function pick(items) {
  return items[Math.floor(Math.random() * items.length)]
}

function weightedPick(weights) {
  const total = Object.values(weights).reduce((sum, weight) => sum + weight, 0)
  let value = Math.random() * total
  for (const [name, weight] of Object.entries(weights)) {
    value -= weight
    if (value < 0) return name
  }
  return 'INFO'
}

function scenarioWeights(scenario, elapsedSeconds) {
  if (scenario !== 'error-spike') return SCENARIOS[scenario]
  const spikeActive = elapsedSeconds % 20 >= 15
  return spikeActive
    ? { DEBUG: 0, INFO: 8, WARN: 12, ERROR: 80 }
    : SCENARIOS['error-spike']
}

function interpolate(message, values) {
  return message.replace(/\{(\w+)\}/g, (_, key) => values[key] ?? `{${key}}`)
}

export function createLog(options, elapsedSeconds = 0) {
  let serviceName = pick(options.services)
  const severity = weightedPick(scenarioWeights(options.scenario, elapsedSeconds))

  if (options.scenario === 'payment-failures' && Math.random() < 0.8) {
    serviceName = options.services.includes('payment-service') ? 'payment-service' : serviceName
  }

  const operation = pick(OPERATIONS[serviceName] ?? ['request processing'])
  const durationMs = Math.floor(20 + Math.random() * (severity === 'ERROR' ? 5000 : 900))
  const thresholdMs = pick([1000, 1500, 2000, 3000])
  let message = interpolate(pick(options.messages[severity]), { durationMs, operation, thresholdMs })

  if (options.scenario === 'database-timeouts' && severity === 'ERROR') {
    message = `Database query timed out after ${thresholdMs} ms while processing ${operation}`
  } else if (options.scenario === 'payment-failures' && severity === 'ERROR') {
    message = pick([
      'Payment authorization declined by provider',
      'Payment capture failed after 3 attempts',
      'Payment provider returned status 502',
    ])
  } else if (options.scenario === 'error-spike' && severity === 'ERROR') {
    message = `Unexpected error spike while processing ${operation}`
  }

  return {
    timestamp: new Date().toISOString(),
    serviceName,
    environment: options.environment,
    severity,
    message,
    traceId: randomBytes(16).toString('hex'),
    host: `${serviceName}-${String(1 + Math.floor(Math.random() * 4)).padStart(2, '0')}`,
    metadata: {
      scenario: options.scenario,
      operation,
      durationMs,
      region: pick(['us-east-1', 'us-west-2', 'eu-west-1']),
    },
  }
}

export function createTraceDemoLogs(options, startedAt = new Date()) {
  return TRACE_DEMO_EVENTS.map((event, index) => ({
    timestamp: new Date(startedAt.getTime() + event.offsetMs).toISOString(),
    serviceName: event.serviceName,
    environment: options.environment,
    severity: event.severity,
    message: event.message,
    traceId: options.traceId ?? TRACE_DEMO_ID,
    host: event.host,
    metadata: {
      scenario: 'trace-demo',
      operation: event.operation,
      sequence: index + 1,
    },
  }))
}

function parseArgs(args) {
  const values = {}
  for (let index = 0; index < args.length; index += 1) {
    const argument = args[index]
    if (argument === '--help' || argument === '-h') values.help = true
    else if (!argument.startsWith('--')) throw new Error(`Unexpected argument: ${argument}`)
    else {
      const [name, inlineValue] = argument.slice(2).split('=', 2)
      const value = inlineValue ?? args[++index]
      if (!value || value.startsWith('--')) throw new Error(`Missing value for --${name}`)
      values[name] = value
    }
  }
  return values
}

async function loadOptions(args, environment = process.env) {
  const input = parseArgs(args)
  const scenario = input.scenario ?? environment.LOG_SCENARIO ?? 'normal'
  if (!SCENARIOS[scenario] && scenario !== 'trace-demo') {
    throw new Error(`Unknown scenario '${scenario}'. Choose: ${[...Object.keys(SCENARIOS), 'trace-demo'].join(', ')}`)
  }

  const requestsPerSecond = Number(input.rps ?? environment.LOG_RPS ?? 5)
  const durationSeconds = Number(input.duration ?? environment.LOG_DURATION ?? 0)
  if (!Number.isFinite(requestsPerSecond) || requestsPerSecond <= 0) throw new Error('RPS must be greater than zero')
  if (!Number.isFinite(durationSeconds) || durationSeconds < 0) throw new Error('Duration must be zero or greater')

  const services = (input.services ?? environment.LOG_SERVICES ?? DEFAULT_SERVICES.join(','))
    .split(',').map(value => value.trim()).filter(Boolean)
  if (services.length === 0) throw new Error('At least one service is required')

  let messages = structuredClone(DEFAULT_MESSAGES)
  const messagesFile = input['messages-file'] ?? environment.LOG_MESSAGES_FILE
  if (messagesFile) {
    const configured = JSON.parse(await readFile(messagesFile, 'utf8'))
    for (const severity of Object.keys(DEFAULT_MESSAGES)) {
      if (configured[severity] !== undefined) {
        if (!Array.isArray(configured[severity]) || configured[severity].length === 0) {
          throw new Error(`${severity} messages must be a non-empty array`)
        }
        messages[severity] = configured[severity]
      }
    }
  }

  return {
    apiUrl: input.url ?? environment.LOG_API_URL ?? 'http://127.0.0.1:8080/api/logs',
    durationSeconds,
    environment: input.environment ?? environment.LOG_ENVIRONMENT ?? 'development',
    help: input.help ?? false,
    messages,
    requestsPerSecond,
    scenario,
    services,
    traceId: input['trace-id'] ?? environment.LOG_TRACE_ID ?? TRACE_DEMO_ID,
  }
}

async function sendPayload(apiUrl, payload) {
  const response = await fetch(apiUrl, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!response.ok) throw new Error(`HTTP ${response.status}: ${await response.text()}`)
}

async function sendLog(options, elapsedSeconds) {
  return sendPayload(options.apiUrl, createLog(options, elapsedSeconds))
}

async function runTraceDemo(options) {
  const logs = createTraceDemoLogs(options)
  console.log(`Sending deterministic trace demo ${logs[0].traceId} -> ${options.apiUrl}`)
  let sent = 0
  for (const log of logs) {
    await sendPayload(options.apiUrl, log)
    sent += 1
    console.log(`${sent}/${logs.length} ${log.serviceName} ${log.severity}: ${log.message}`)
  }
  console.log(`Trace demo complete: ${sent} events, traceId=${logs[0].traceId}`)
  return { sent, failed: 0 }
}

export async function run(options) {
  if (options.scenario === 'trace-demo') return runTraceDemo(options)

  const startedAt = Date.now()
  const pending = new Set()
  let sent = 0
  let failed = 0
  let budget = 0
  let previousTick = Date.now()
  let stopping = false

  const stop = () => { stopping = true }
  process.once('SIGINT', stop)
  process.once('SIGTERM', stop)

  console.log(`Generating ${options.scenario} traffic at ${options.requestsPerSecond} req/s -> ${options.apiUrl}`)
  while (!stopping) {
    const now = Date.now()
    const elapsedSeconds = (now - startedAt) / 1000
    if (options.durationSeconds > 0 && elapsedSeconds >= options.durationSeconds) break

    budget += options.requestsPerSecond * (now - previousTick) / 1000
    previousTick = now
    const requestCount = Math.min(Math.floor(budget), Math.ceil(options.requestsPerSecond))
    budget -= requestCount

    for (let index = 0; index < requestCount; index += 1) {
      const request = sendLog(options, elapsedSeconds)
        .then(() => { sent += 1 })
        .catch(error => {
          failed += 1
          console.error(`Request failed: ${error.message}`)
        })
        .finally(() => pending.delete(request))
      pending.add(request)
    }
    await new Promise(resolve => setTimeout(resolve, 100))
  }

  await Promise.allSettled(pending)
  process.removeListener('SIGINT', stop)
  process.removeListener('SIGTERM', stop)
  console.log(`Stopped after ${sent + failed} requests: ${sent} succeeded, ${failed} failed`)
  if (failed > 0) process.exitCode = 1
  return { sent, failed }
}

function printHelp() {
  console.log(`Usage: node generator.js [options]

Options:
  --url URL                 API endpoint (default: http://127.0.0.1:8080/api/logs)
  --scenario NAME           normal, warnings, database-timeouts, payment-failures, error-spike, trace-demo
  --trace-id ID             Shared trace ID for trace-demo (default: demo-checkout-trace-001)
  --services NAMES          Comma-separated service names
  --messages-file PATH      JSON file with DEBUG, INFO, WARN, and ERROR message arrays
  --environment NAME        Log environment (default: development)
  --rps NUMBER              Requests per second (default: 5)
  --duration SECONDS        Stop after this duration; 0 runs until interrupted (default: 0)
  --help                    Show this help

Equivalent environment variables use the LOG_ prefix, for example LOG_RPS and LOG_SCENARIO.`)
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  loadOptions(process.argv.slice(2))
    .then(options => options.help ? printHelp() : run(options))
    .catch(error => {
      console.error(`log-generator: ${error.message}`)
      process.exitCode = 1
    })
}