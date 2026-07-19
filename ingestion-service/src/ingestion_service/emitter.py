"""Kafka producer helpers for emitting validated tile compression events."""

from __future__ import annotations

import uuid
from datetime import datetime, timezone

from confluent_kafka import Producer

from ingestion_service.kafka_settings import kafka_bootstrap_servers
from ingestion_service.models.tile_compression_event import TileCompressionEventPayload
from ingestion_service.simulation.compression import get_tile_catalog, random_active_tile


def _delivery_report(err, msg) -> None:
    """Log the outcome of an asynchronous Kafka delivery attempt."""
    if err is not None:
        print(f"Delivery failed for record {msg.key()!r}: {err}")
    else:
        print(
            f"Record successfully produced to "
            f"topic={msg.topic()} partition={msg.partition()} offset={msg.offset()}"
        )


def _create_producer() -> Producer:
    """Build an idempotent Kafka producer configured for this service."""
    return Producer(
        {
            "bootstrap.servers": kafka_bootstrap_servers(),
            "client.id": "ingestion-service-emitter",
            "enable.idempotence": True,
        }
    )


def send_compression_demo() -> None:
    """Produce a single validated tile compression event to raw-traffic-events."""
    producer = _create_producer()
    get_tile_catalog()

    event = TileCompressionEventPayload(
        event_id=str(uuid.uuid4()),
        event_type="tile_compression",
        mass_kg=80.0,
        impact_multiplier=1.15,
        tile_id=random_active_tile(),
        timestamp=datetime.now(timezone.utc),
    )

    producer.produce(
        topic="raw-traffic-events",
        key=event.message_key(),
        value=event.to_json_bytes(),
        callback=_delivery_report,
    )
    producer.flush()


if __name__ == "__main__":
    send_compression_demo()
