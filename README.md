# Log Analyzer

A developer workspace built with React, TypeScript, Vite, Java 21, Spring Boot, and PostgreSQL. It provides an end-to-end health check and synchronous REST log ingestion backed by PostgreSQL.

## Start with Docker Compose

Prerequisites: Docker Desktop (or Docker Engine with Compose v2.20+) running, available ports 3000, 8080, and 5432, and internet access for the first image/dependency download. Host Node.js, Maven, and Java are not required for the Docker workflow.

From the repository root:

```sh
docker compose up --build -d --wait --wait-timeout 180
```

The first build may take several minutes. Both application image builds run their tests. PostgreSQL must become healthy before the backend starts, and the backend must become healthy before the frontend starts.

| Service | Address |
| --- | --- |
| Frontend | http://localhost:3000 |
| Backend health | http://localhost:8080/api/health |
| Health through the frontend proxy | http://localhost:3000/api/health |
| PostgreSQL | localhost:5432 |

Open the frontend to see the backend connection status, database availability, request duration, and recent health checks. The Health API route shows the actual JSON response. Checks refresh every 30 seconds and can also be triggered manually or paused. The latest eight checks are kept only in browser memory. Frontend status means the page has loaded; it is not an independent server probe.

```sh
docker compose ps
curl --fail http://localhost:3000/api/health
docker compose logs -f backend
```

Stop the stack while preserving database data:

```sh
docker compose down
```

To deliberately delete all local database data as well, run `docker compose down -v`. Do not use `-v` when data must be retained.

## Configuration

Defaults work without an environment file. To override them, create a root `.env` using `.env.example` as a reference. Compose reads this file automatically; it is excluded from git.

| Variable | Default |
| --- | --- |
| `FRONTEND_PORT` | `3000` |
| `BACKEND_PORT` | `8080` |
| `POSTGRES_PORT` | `5432` |
| `POSTGRES_DB` | `log_analyzer` |
| `POSTGRES_USER` | `log_analyzer` |
| `POSTGRES_PASSWORD` | `local_dev_password` |

For a port conflict, choose a free host port, for example:

```sh
FRONTEND_PORT=3001 docker compose up --build -d --wait
```

Internal service ports and the frontend API URL do not change. All published ports bind to localhost. Credentials are local development defaults, not production secrets. There is no authentication or TLS in this initial local scaffold. PostgreSQL initialization variables apply only to an empty volume; changing credentials later requires updating the database itself or explicitly resetting disposable data.

## Repository Layout

```text
frontend/
  src/
    api/          Typed HTTP client and response validation
    components/   Shared status and refresh controls
    hooks/        Health polling and session history
    pages/        Overview and API response routes
    App.tsx       React Router and workspace layout
  Dockerfile      Vite build and unprivileged Nginx runtime
  nginx.conf      Same-origin /api proxy and SPA fallback
backend/
  src/main/java/dev/loganalyzer/
    controller/   HTTP routing and response codes
    service/      Health behavior
    repository/   JDBC health probe and JPA repository
    entity/       Initial LogEntry persistence model
    dto/          Public health response contract
  src/main/resources/db/migration/
                  Versioned PostgreSQL schema (Flyway)
  src/test/       Health endpoint tests
  Dockerfile      Maven build and non-root Java 21 runtime
docker-compose.yml
```

The `LogEntry` entity and repository persist logs submitted through `POST /api/logs`. Search and analysis features are not implemented. Flyway owns schema changes; Hibernate validates the schema at startup.

## Ingest Logs

`POST /api/logs` accepts JSON and returns HTTP 201 with the saved log and a `Location` header. Required fields are `timestamp`, `serviceName`, `environment`, `severity`, `message`, and `host`. `severity` must be `DEBUG`, `INFO`, `WARN`, or `ERROR`; `traceId` and `metadata` are optional.

Send an INFO log:

```sh
curl --fail-with-body -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{"timestamp":"2026-09-17T12:00:00Z","serviceName":"catalog-api","environment":"development","severity":"INFO","message":"Catalog refresh completed","host":"catalog-01","metadata":{"itemCount":128}}'
```

Send a WARN log:

```sh
curl --fail-with-body -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{"timestamp":"2026-09-17T12:01:00Z","serviceName":"checkout-api","environment":"staging","severity":"WARN","message":"Payment provider response was slow","traceId":"trace-456","host":"checkout-02","metadata":{"durationMs":2400}}'
```

Send an ERROR log:

```sh
curl --fail-with-body -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{"timestamp":"2026-09-17T12:02:00Z","serviceName":"billing-api","environment":"production","severity":"ERROR","message":"Payment capture failed","traceId":"trace-789","host":"billing-01","metadata":{"provider":"example-pay","retryable":true}}'
```

Invalid JSON or validation failures return HTTP 400 with field details where available. Unexpected failures return HTTP 500 with a generic message; server details remain in backend logs.

## Health Contract

`GET /api/health` performs a bounded PostgreSQL `SELECT 1` through controller, service, and repository layers. A successful check returns HTTP 200:

```json
{
  "status": "UP",
  "service": "log-analyzer",
  "database": "UP",
  "timestamp": "2026-09-17T12:00:00Z"
}
```

A database failure returns HTTP 503 with `status` and `database` set to `DOWN`, without exposing connection details. This is a readiness check, not a liveness endpoint. If PostgreSQL is unavailable at startup, Flyway prevents the backend from starting. The frontend distinguishes a database failure from an unreachable backend and times out requests after six seconds.

In Docker, Nginx forwards `/api/*` to `backend:8080` using Docker DNS. In local development, Vite forwards the same path to `localhost:8080`. The browser never resolves Docker service names and no cross-origin configuration is needed. Nginx also supports direct navigation to React Router paths such as `/api-details`.

## Develop Without Application Containers

Use Node.js 22.12+ (or a compatible newer LTS), Java 21, and Maven 3.9+. Start only the database:

```sh
docker compose up -d --wait postgres
```

In one terminal:

```sh
cd backend
mvn spring-boot:run
```

In another:

```sh
cd frontend
npm ci
npm run dev
```

Vite prints its URL, normally http://localhost:5173. Stop the Compose frontend/backend first if they are already running (`docker compose stop frontend backend`). The backend defaults match the default Compose database. With custom database settings, explicitly export `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` before running Maven; the backend does not read the root `.env` itself. For a different backend port, set Spring's `SERVER_PORT` and pass `API_PROXY_TARGET=http://localhost:<port>` to `npm run dev`.

## Checks

```sh
cd frontend
npm ci
npm test
npm run lint
npm run build
```

```sh
cd backend
mvn test
```

Backend unit tests do not require a database. Controller integration tests use Testcontainers and require a running Docker daemon. `docker compose build` executes both projects' tests in their specified build environments. The live Compose health check validates real PostgreSQL connectivity and migration startup.

To check outage handling on this disposable development stack, stop PostgreSQL with `docker compose stop postgres`, refresh the UI, and expect HTTP 503 with database status `DOWN`. Restore it with `docker compose up -d --wait postgres backend frontend`. The backend reconnects without a rebuild.

## Scope

Only frontend, backend, and PostgreSQL are configured. Redis, Kafka, OpenSearch, Prometheus, and Grafana are intentionally not installed or configured. The root Compose file and its default network can be extended in later increments; there are no placeholder containers or observability dependencies.

## Troubleshooting

- If Docker cannot connect, start Docker Desktop and retry.
- For startup failures, inspect `docker compose logs backend postgres` and `docker compose ps`.
- For a disconnected UI, check both the direct and proxied health URLs above. A 502 indicates the proxy cannot reach the backend; a JSON 503 indicates a database problem.
- To apply source changes to Docker images, rerun `docker compose up --build -d --wait`.
- UI fonts are loaded from Google Fonts, with local sans-serif/monospace fallbacks when offline. Application behavior does not depend on that font request.