package com.projectkinetile.physicsengine.api;

/** Chokepoint catalog row with place type and active tile count. */
public record ChokepointCatalogDTO(
    Long id,
    String code,
    String name,
    String placeTypeCode,
    String placeTypeLabel,
    String trafficTier,
    long activeTileCount) {}
