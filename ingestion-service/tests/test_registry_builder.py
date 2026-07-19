"""Tests for infrastructure registry builder output and legacy catalog constants."""

from ingestion_service.registry_builder import (
    CITY_CODES,
    _LEGACY_CHOKEPOINTS,
    build_registry,
    classify_place_type,
)


def test_legacy_catalog_has_88_chokepoints() -> None:
    """Legacy seed catalog contains 15 cities and 88 chokepoints total."""
    total = sum(len(sites) for sites in _LEGACY_CHOKEPOINTS.values())
    assert len(_LEGACY_CHOKEPOINTS) == 15
    assert total == 88


def test_build_registry_shape() -> None:
    """Built registry lists manufacturers, city codes, place types, and active tiles."""
    registry = build_registry(seed=42, seed_date=__import__("datetime").date(2026, 6, 14))
    assert registry["manufacturers"] == ["Aslan", "Æ Inc", "GFS"]
    assert len(registry["cities"]) == 15
    assert all(city["code"] == CITY_CODES[city["name"]] for city in registry["cities"])
    for city in registry["cities"]:
        for cp in city["chokepoints"]:
            assert cp["tileInstances"]
            assert classify_place_type(cp["name"]) == cp["placeType"]
            for tile in cp["tileInstances"]:
                assert tile["isActive"] is True
                assert tile["tileId"]
