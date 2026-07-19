# physics-engine

Java 21 / Spring Boot service for Project Kinetile: consumes compression events from Kafka, runs the piezoelectric feasibility model, persists results to PostgreSQL, and exposes analytics via REST (`/api/v1`).

## Prerequisites

- PostgreSQL with schema and catalog seeded by **`db-init`** (see root [README.md](../README.md))
- Kafka with topic `raw-traffic-events`

## Quick start

From the **repository root**:

```bash
mvn -pl physics-engine spring-boot:run
```

Defaults: Postgres `localhost:15432`, Kafka `localhost:9092`, API `http://localhost:8080`.

**Default database credentials** (`projectkinetile` / `projectkinetile`) are for local Docker Compose only. Override via `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_HOST_PORT`, and `POSTGRES_DB` (see root `.env.example`).

## Tests

```bash
mvn -pl physics-engine test
```

Docker image builds also run the test suite (`docker compose build physics-engine`).

## REST API

| Path | Description |
|------|-------------|
| `/api/v1/energy/**` | Compression analytics, ledger, time series |
| `/api/v1/config/hardware` | Tile physics constants for the dashboard calculator |
| `/api/v1/devices` | Edge-device catalog |
| `/api/v1/infrastructure/**` | Cities, chokepoints, tiles (catalog) |

**Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Key environment variables

| Variable | Purpose |
|----------|---------|
| `POSTGRES_*` | Database connection |
| `APP_CORS_ALLOWED_ORIGINS` | Browser CORS allowlist |
| `APP_RATE_LIMIT_ENABLED` | Enable per-IP rate limiting (default `true`) |
| `APP_RATE_LIMIT_RPM` | Requests per minute per IP (default `60`) |
| `APP_RATE_LIMIT_PENALTY_ESCALATION_MINUTES` | Ordered penalty-block durations in minutes (1st violation → 2nd → …). Overrides `app.rate-limit.penalty-escalation-minutes`; default ladder in `application.yml`: 5, 15, 60, 1440 |
| `APP_KAFKA_RAW_TRAFFIC_TOPIC` | Kafka ingestion topic |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers |

## Rate limiting

Per-IP limits are stored in PostgreSQL (`api_rate_limit_clients`) so they are **consistent across multiple pods** (e.g. Kubernetes). After exceeding the per-minute quota, clients receive escalating penalty blocks; repeated abuse within 24 hours can result in a permanent block (until cleared in the database).

Tune limits in `application.yml` under `app.rate-limit` (enabled, requests-per-minute, penalty-escalation-minutes list, permanent-after-violations, violation-window-hours, entry-ttl-minutes, cleanup-interval-minutes).

Optional ingress-level rate limiting can be added in production as an extra DDoS layer.
