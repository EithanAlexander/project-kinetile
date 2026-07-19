"""Shared validated models for ingestion payloads."""

from ingestion_service.models.tile_compression_event import (
    MAX_BATCH_SIZE,
    TileCompressionEventPayload,
)

__all__ = ["MAX_BATCH_SIZE", "TileCompressionEventPayload"]
