"""Validated outbound tile compression event schema (matches Java {@code TileCompressionEvent})."""

from __future__ import annotations

import json
from datetime import datetime
from typing import Annotated, Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

TileCompressionEventType = Literal["tile_compression"]

MAX_PAYLOAD_BYTES = 8192
MAX_STRING_LENGTH = 128
MAX_BATCH_SIZE = 5000
MIN_BURST_SLEEP_SECONDS = 0.1


class TileCompressionEventPayload(BaseModel):
    """Single hardware-level tile compression published to Kafka."""

    model_config = ConfigDict(populate_by_name=True)
    event_id: Annotated[str, Field(min_length=1, max_length=MAX_STRING_LENGTH)]
    event_type: TileCompressionEventType
    tile_id: UUID
    mass_kg: Annotated[float, Field(gt=0, le=500)]
    impact_multiplier: Annotated[float, Field(ge=1.0, le=1.5)]
    timestamp: datetime

    def message_key(self) -> bytes:
        """UTF-8 partition key from ``tile_id`` so all events for one tile stay ordered."""
        return str(self.tile_id).encode("utf-8")

    def to_json_bytes(self) -> bytes:
        """Serialize to snake_case JSON bytes with a size guard."""
        data = self.model_dump(mode="json", by_alias=True)
        encoded = json.dumps(data).encode("utf-8")
        if len(encoded) > MAX_PAYLOAD_BYTES:
            raise ValueError(f"payload exceeds {MAX_PAYLOAD_BYTES} bytes")
        return encoded
