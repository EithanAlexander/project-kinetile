"""Load active tile IDs from PostgreSQL for event simulation."""

from __future__ import annotations

import os
import random
from uuid import UUID

import psycopg


class TileCatalogError(RuntimeError):
    """Raised when the tile catalog cannot be loaded from PostgreSQL."""


def _require_env(name: str) -> str:
    """Return a non-empty environment variable or raise a catalog error."""
    value = os.environ.get(name, "").strip()
    if not value:
        raise TileCatalogError(f"Missing required environment variable: {name}")
    return value


def _connect_postgres() -> psycopg.Connection:
    """Open a PostgreSQL connection using env-backed credentials (no in-code secrets)."""
    return psycopg.connect(
        host=os.environ.get("POSTGRES_HOST", "localhost"),
        port=os.environ.get("POSTGRES_HOST_PORT", "15432"),
        dbname=os.environ.get("POSTGRES_DB", "projectkinetile"),
        user=os.environ.get("POSTGRES_USER", "projectkinetile"),
        password=_require_env("POSTGRES_PASSWORD"),
        connect_timeout=5,
    )


def load_active_tile_ids_from_db() -> list[UUID]:
    """
    Return all active tile UUIDs from PostgreSQL.

    Raises:
        TileCatalogError: if the database is unreachable or contains no active tiles.
    """
    query = "SELECT tile_id FROM tiles WHERE is_active = true"
    try:
        with _connect_postgres() as conn:
            with conn.cursor() as cur:
                cur.execute(query)
                rows = cur.fetchall()
    except psycopg.Error as exc:
        raise TileCatalogError(
            "Failed to connect to PostgreSQL for tile catalog. Run db-init first."
        ) from exc

    tile_ids = [UUID(str(row[0])) for row in rows]
    if not tile_ids:
        raise TileCatalogError(
            "No active tiles found in PostgreSQL. Run db-init (docker compose up) first."
        )
    return tile_ids


class ActiveTileCatalog:
    """In-memory cache of active tile IDs loaded once from PostgreSQL."""

    def __init__(self, tile_ids: list[UUID]) -> None:
        if not tile_ids:
            raise TileCatalogError("Active tile catalog cannot be empty")
        self._tile_ids = tile_ids

    @classmethod
    def from_database(cls) -> ActiveTileCatalog:
        """Load active tiles from PostgreSQL."""
        return cls(load_active_tile_ids_from_db())

    def random_active_tile(self) -> UUID:
        """Pick a random active tile UUID."""
        return random.choice(self._tile_ids)

    def __len__(self) -> int:
        return len(self._tile_ids)
