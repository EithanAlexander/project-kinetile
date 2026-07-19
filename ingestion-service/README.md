# ingestion-service

Python 3.12 ingestion for Project Kinetile: synthetic **firehose** that emits `TileCompressionEvent` JSON to Kafka (`raw-traffic-events`).

## Prerequisites

- Kafka reachable at `KAFKA_BOOTSTRAP_SERVERS`
- PostgreSQL seeded by **`db-init`** — firehose loads active `tile_id` values from the database (does not mint UUIDs)

## Quick start

```bash
poetry install
cp .env.example .env   # optional; loaded on firehose startup
poetry run ingestion-firehose
```

## Environment

- `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`)
- `INGESTION_MAX_DURATION_SECONDS` — default `1200` (20 minutes); non-positive = no limit
- `INGESTION_BATCH_MIN` / `INGESTION_BATCH_MAX` — burst size (defaults `200` / `500`)
- `INGESTION_BURST_SLEEP_MIN_SECONDS` / `INGESTION_BURST_SLEEP_MAX_SECONDS` — pause between bursts (defaults `1.0` / `2.0`)

See the root [README.md](../README.md) and [docker-compose.yml](../docker-compose.yml).
