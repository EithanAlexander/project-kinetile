"""Tests for tile compression payload validation."""

import json
from datetime import datetime, timezone
from uuid import UUID

import pytest
from pydantic import ValidationError

from ingestion_service.models.tile_compression_event import TileCompressionEventPayload

TILE_ID = UUID("11111111-1111-4111-8111-111111111111")


def test_valid_payload_serializes() -> None:
    """A valid payload serializes to JSON with the expected canonical fields."""
    event = TileCompressionEventPayload(
        event_id="evt-1",
        event_type="tile_compression",
        mass_kg=80.0,
        impact_multiplier=1.2,
        tile_id=TILE_ID,
        timestamp=datetime.now(timezone.utc),
    )
    data = event.model_dump(mode="json")
    assert data["event_type"] == "tile_compression"
    assert data["tile_id"] == str(TILE_ID)
    assert "city" not in data
    assert "location" not in data


def test_rejects_invalid_event_type() -> None:
    """An unsupported event_type raises a Pydantic ValidationError."""

    with pytest.raises(ValidationError):
        TileCompressionEventPayload(
            event_id="evt-1",
            event_type="pedestrian",  # type: ignore[arg-type]
            mass_kg=80.0,
            impact_multiplier=1.2,
            tile_id=TILE_ID,
            timestamp=datetime.now(timezone.utc),
        )


@pytest.mark.parametrize(
    ("field", "value"),
    [
        ("mass_kg", 0.0),
        ("mass_kg", 600.0),
        ("impact_multiplier", 0.9),
        ("impact_multiplier", 1.6),
    ],
)
def test_rejects_out_of_range_physical_values(field: str, value: float) -> None:
    """Mass and impact multiplier outside their physical bounds are rejected."""
    kwargs = {
        "event_id": "evt-1",
        "event_type": "tile_compression",
        "mass_kg": 80.0,
        "impact_multiplier": 1.2,
        "tile_id": TILE_ID,
        "timestamp": datetime.now(timezone.utc),
    }
    kwargs[field] = value
    with pytest.raises(ValidationError):
        TileCompressionEventPayload(**kwargs)


def test_serialization_produces_key_and_snake_case_json() -> None:
    """to_json_bytes emits UTF-8 snake_case JSON; partition key is tile_id."""
    event = TileCompressionEventPayload(
        event_id="evt-42",
        event_type="tile_compression",
        mass_kg=80.0,
        impact_multiplier=1.2,
        tile_id=TILE_ID,
        timestamp=datetime.now(timezone.utc),
    )
    assert event.message_key() == str(TILE_ID).encode("utf-8")
    payload = json.loads(event.to_json_bytes().decode("utf-8"))
    assert payload["event_id"] == "evt-42"
    assert payload["event_type"] == "tile_compression"
    assert payload["tile_id"] == str(TILE_ID)
