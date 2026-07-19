"""Shared Kafka client defaults for ingestion (aligned with physics-engine bootstrap-servers)."""

from __future__ import annotations

import os


def kafka_bootstrap_servers() -> str:
    """Broker list from ``KAFKA_BOOTSTRAP_SERVERS``, default ``localhost:9092``."""
    return os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092").strip()
