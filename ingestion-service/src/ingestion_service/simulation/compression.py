"""Simulated tile compression parameters and event-field helpers."""

from __future__ import annotations

import random
from datetime import datetime, timedelta, timezone
from uuid import UUID

from ingestion_service.tile_catalog import ActiveTileCatalog

_EVENT_TS_LOOKBACK = timedelta(days=365)

# ~5% low-mass events to exercise sub-threshold activations in dev dashboards.
_SUB_THRESHOLD_FRACTION = 0.05


class _TileCatalogState:
    """Lazy holder for the process-wide active tile catalog."""

    _instance: ActiveTileCatalog | None = None

    @classmethod
    def get(cls) -> ActiveTileCatalog:
        if cls._instance is None:
            cls._instance = ActiveTileCatalog.from_database()
        return cls._instance

    @classmethod
    def set(cls, catalog: ActiveTileCatalog) -> None:
        cls._instance = catalog


def init_tile_catalog(catalog: ActiveTileCatalog) -> None:
    """Inject or replace the process-wide active tile catalog."""
    _TileCatalogState.set(catalog)


def get_tile_catalog() -> ActiveTileCatalog:
    """Return the initialized tile catalog."""
    return _TileCatalogState.get()


def random_event_timestamp() -> datetime:
    """Return a uniformly random instant from the past year through now (UTC)."""
    end = datetime.now(timezone.utc)
    start = end - _EVENT_TS_LOOKBACK
    span_seconds = (end - start).total_seconds()
    offset = random.uniform(0.0, span_seconds)
    return start + timedelta(seconds=offset)


def random_mass_kg() -> float:
    """
    Return a random load mass (kg) representing an anonymous compression source.

    Most draws are 20–200 kg (typical footfall / loaded rider). Occasionally draws
    5–15 kg so downstream analytics can show sub-threshold compressions.
    """
    if random.random() < _SUB_THRESHOLD_FRACTION:
        return random.uniform(5.0, 15.0)
    return random.uniform(20.0, 200.0)


def random_impact_multiplier() -> float:
    """Return a dynamic footfall multiplier in the commercial range 1.0×–1.5× body weight."""
    return random.uniform(1.0, 1.5)


def random_active_tile() -> UUID:
    """Pick a random active tile UUID from the PostgreSQL-backed catalog."""
    return get_tile_catalog().random_active_tile()
