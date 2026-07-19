"""Tests for simulated firehose env parsing and compression helpers."""

import time

import pytest

from ingestion_service.firehose import (
    _batch_size_bounds,
    _max_duration_seconds,
    _validate_runtime_config,
)
from ingestion_service.models.tile_compression_event import MAX_BATCH_SIZE
from ingestion_service.simulation.compression import random_event_timestamp, random_mass_kg


def test_random_event_timestamp_within_past_year() -> None:
    """Generated event timestamps fall within the trailing one-year window."""
    now = time.time()
    earliest = now - (365 * 86400 + 1)
    latest = now + 1
    for _ in range(100):
        ts = random_event_timestamp()
        assert earliest <= ts.timestamp() <= latest


def test_random_mass_kg_within_bounds() -> None:
    """Random pedestrian/light-mobility mass stays within the configured bounds."""
    for _ in range(200):
        mass = random_mass_kg()
        assert 5.0 <= mass <= 200.0


def test_max_duration_seconds_defaults(monkeypatch: pytest.MonkeyPatch) -> None:
    """Max duration falls back to its default when the env var is unset."""
    monkeypatch.delenv("INGESTION_MAX_DURATION_SECONDS", raising=False)
    assert _max_duration_seconds() == pytest.approx(1200.0)


def test_batch_size_bounds_defaults(monkeypatch: pytest.MonkeyPatch) -> None:
    """Batch-size bounds fall back to their defaults when env vars are unset."""
    monkeypatch.delenv("INGESTION_BATCH_MIN", raising=False)
    monkeypatch.delenv("INGESTION_BATCH_MAX", raising=False)
    assert _batch_size_bounds() == (200, 500)


def test_validate_runtime_config_rejects_oversized_batch() -> None:
    """Runtime validation rejects a batch maximum above the allowed ceiling."""
    with pytest.raises(ValueError, match="INGESTION_BATCH_MAX"):
        _validate_runtime_config(200, MAX_BATCH_SIZE + 1, 1.0, 2.0)


def test_validate_runtime_config_rejects_nonpositive_batch_min() -> None:
    """Runtime validation rejects a batch minimum below 1."""
    with pytest.raises(ValueError, match="INGESTION_BATCH_MIN"):
        _validate_runtime_config(0, 500, 1.0, 2.0)
