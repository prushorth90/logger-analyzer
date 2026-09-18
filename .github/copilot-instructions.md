# Log Analyzer Copilot Instructions

This repository is a full-stack Log Analyzer.

## Stack
- Frontend: React + TypeScript + Vite
- Backend: Java 21 + Spring Boot
- Database: PostgreSQL
- Migrations: Flyway
- Messaging: Kafka
- Cache: Redis
- Search: OpenSearch
- Observability: OpenTelemetry, Prometheus, Grafana
- Containers: Docker / Docker Compose
- CI/CD: GitHub Actions

## Architecture rules
- Keep frontend and backend separated under `/frontend` and `/backend`.
- Backend should use controller, service, repository, DTO, and entity layers.
- Keep JPA entities separate from API DTOs.
- Use Flyway for schema changes.
- Do not rely on Hibernate auto schema creation.
- PostgreSQL is the durable source of truth.
- Kafka is used for asynchronous log ingestion.
- OpenSearch is used for full text log search.
- Redis is used only where caching or shared distributed state is justified.
- Do not introduce Spark unless explicitly requested.

## Development rules
- Do not invent placeholder production metrics.
- Do not add technologies that were not requested.
- Prefer simple implementations before introducing distributed components.
- Add tests for new backend behavior.
- Preserve existing working architecture unless a change is required.
- Update README when architecture or startup instructions change.