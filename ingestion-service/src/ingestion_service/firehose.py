"""
Continuous simulated tile compression generator for Kafka pipeline testing.

Produces validated ``TileCompressionEvent`` JSON to ``raw-traffic-events``. Each event
references a random **active** tile UUID loaded from PostgreSQL, with ``mass_kg`` and
``impact_multiplier`` drawn from the simulation helpers. Timestamps span the past year
through now (UTC).

**Prerequisites:** PostgreSQL seeded via ``db-init``; ``POSTGRES_PASSWORD`` and Kafka broker
env vars set (see project ``.env``).

**Runtime tuning** (all optional):

- ``INGESTION_MAX_DURATION_SECONDS`` — stop after N seconds (0 = unlimited; default 1200)
- ``INGESTION_BATCH_MIN`` / ``INGESTION_BATCH_MAX`` — events per burst (default 200–500)
- ``INGESTION_BURST_SLEEP_MIN_SECONDS`` / ``INGESTION_BURST_SLEEP_MAX_SECONDS`` — pause
  between bursts (default 1–2 s)
"""

from __future__ import annotations

import os
import random
import time
import uuid
from pathlib import Path

from confluent_kafka import Producer
from dotenv import load_dotenv

from ingestion_service.kafka_settings import kafka_bootstrap_servers
from ingestion_service.models.tile_compression_event import (
    MAX_BATCH_SIZE,
    MIN_BURST_SLEEP_SECONDS,
    TileCompressionEventPayload,
)
from ingestion_service.simulation.compression import (
    get_tile_catalog,
    random_active_tile,
    random_event_timestamp,
    random_impact_multiplier,
    random_mass_kg,
)

_INGESTION_PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
_REPO_ROOT = _INGESTION_PROJECT_ROOT.parent
# Shared POSTGRES_* live in repo root .env (same as Docker Compose); ingestion-only vars in ingestion-service/.env.
load_dotenv(_REPO_ROOT / ".env", override=False)
load_dotenv(_INGESTION_PROJECT_ROOT / ".env", override=False)


def _create_producer() -> Producer:
    """Build a Confluent Kafka producer targeting the local broker."""
    return Producer(
        {
            "bootstrap.servers": kafka_bootstrap_servers(),
            "client.id": "ingestion-service-firehose",
            "enable.idempotence": True,
        }
    )


def _max_duration_seconds() -> float:
    """Return the firehose duration cap from ``INGESTION_MAX_DURATION_SECONDS`` (default 1200)."""
    raw = os.environ.get("INGESTION_MAX_DURATION_SECONDS", "1200").strip()
    try:
        v = float(raw)
    except ValueError:
        return 1200.0
    return v


def _batch_size_bounds() -> tuple[int, int]:
    """Return inclusive min/max burst sizes from env, clamped to ``MAX_BATCH_SIZE``."""
    default_lo, default_hi = 200, 500
    try:
        lo = int(os.environ.get("INGESTION_BATCH_MIN", str(default_lo)).strip())
        hi = int(os.environ.get("INGESTION_BATCH_MAX", str(default_hi)).strip())
    except ValueError:
        return (default_lo, default_hi)
    if lo < 1 or hi < 1:
        return (default_lo, default_hi)
    if lo > hi:
        lo, hi = hi, lo
    hi = min(hi, MAX_BATCH_SIZE)
    lo = min(lo, hi)
    return (lo, hi)


def _burst_sleep_range_seconds() -> tuple[float, float]:
    """Return inclusive min/max seconds to sleep between bursts (env-backed)."""
    default_lo, default_hi = 1.0, 2.0
    try:
        lo = float(os.environ.get("INGESTION_BURST_SLEEP_MIN_SECONDS", str(default_lo)).strip())
        hi = float(os.environ.get("INGESTION_BURST_SLEEP_MAX_SECONDS", str(default_hi)).strip())
    except ValueError:
        return (default_lo, default_hi)
    if lo < 0 or hi < 0:
        return (default_lo, default_hi)
    if lo > hi:
        lo, hi = hi, lo
    lo = max(lo, MIN_BURST_SLEEP_SECONDS)
    hi = max(hi, lo)
    return (lo, hi)


def _validate_runtime_config(batch_lo: int, batch_hi: int, sleep_lo: float, sleep_hi: float) -> None:
    """Raise ``ValueError`` when parsed batch or sleep bounds violate hard limits."""
    if batch_lo < 1:
        raise ValueError("INGESTION_BATCH_MIN must be at least 1")
    if batch_hi > MAX_BATCH_SIZE:
        raise ValueError(f"INGESTION_BATCH_MAX must not exceed {MAX_BATCH_SIZE}")
    if sleep_lo < MIN_BURST_SLEEP_SECONDS:
        raise ValueError(
            f"INGESTION_BURST_SLEEP_MIN_SECONDS must be at least {MIN_BURST_SLEEP_SECONDS}"
        )
    if sleep_hi < MIN_BURST_SLEEP_SECONDS:
        raise ValueError(
            f"INGESTION_BURST_SLEEP_MAX_SECONDS must be at least {MIN_BURST_SLEEP_SECONDS}"
        )


def _produce_burst(
    producer: Producer,
    delivery_report,
    batch_size: int,
    deadline: float | None,
) -> bool:
    """
    Produce one burst of simulated compression events.

    Returns:
        ``True`` when the duration deadline was hit mid-batch (caller should stop).
    """
    for _ in range(batch_size):
        if deadline is not None and time.monotonic() >= deadline:
            print("Duration cap reached mid-batch; flushing and exiting.")
            return True

        tile_id = random_active_tile()
        event = TileCompressionEventPayload(
            event_id=str(uuid.uuid4()),
            event_type="tile_compression",
            mass_kg=round(random_mass_kg(), 2),
            impact_multiplier=round(random_impact_multiplier(), 3),
            tile_id=tile_id,
            timestamp=random_event_timestamp(),
        )
        value = event.to_json_bytes()
        producer.produce(
            topic="raw-traffic-events",
            key=event.message_key(),
            value=value,
            callback=delivery_report,
        )
        producer.poll(0)
    return False


def run_firehose() -> None:
    """
    Emit simulated tile compression events in burst batches until stopped.

    Runs until ``INGESTION_MAX_DURATION_SECONDS`` elapses, or the process receives
    ``KeyboardInterrupt``. Flushes the Kafka producer on exit.
    """
    producer = _create_producer()
    delivery_counter: list[int] = [0]

    def delivery_report(err, msg) -> None:
        delivery_counter[0] += 1
        n = delivery_counter[0]
        if err is not None:
            print(f"Delivery failed for record {msg.key()!r}: {err}")
            return
        if n % 50 == 0:
            print(
                f"[{n}] produced to topic={msg.topic()} "
                f"partition={msg.partition()} offset={msg.offset()}"
            )

    duration = _max_duration_seconds()
    deadline = time.monotonic() + duration if duration > 0 else None
    batch_lo, batch_hi = _batch_size_bounds()
    sleep_lo, sleep_hi = _burst_sleep_range_seconds()
    _validate_runtime_config(batch_lo, batch_hi, sleep_lo, sleep_hi)
    catalog = get_tile_catalog()
    print(
        "Firehose started. Topic: raw-traffic-events (tile_compression burst mode). "
        f"Active tiles loaded from DB: {len(catalog)}. "
        f"Duration cap: {'unlimited' if deadline is None else f'{duration}s'}. "
        f"Batch size: {batch_lo}..{batch_hi}. Inter-burst sleep: {sleep_lo}s..{sleep_hi}s."
    )

    try:
        while True:
            if deadline is not None and time.monotonic() >= deadline:
                print("Duration cap reached; flushing and exiting.")
                break

            batch_size = random.randint(batch_lo, batch_hi)
            if _produce_burst(producer, delivery_report, batch_size, deadline):
                break

            time.sleep(random.uniform(sleep_lo, sleep_hi))
    except KeyboardInterrupt:
        print("\nShutting down: flushing outstanding messages...")
    finally:
        producer.flush()


if __name__ == "__main__":
    run_firehose()
