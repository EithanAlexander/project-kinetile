package com.projectkinetile.physicsengine.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Detailed tile view with resolved city and chokepoint labels. */
public record TileDetailDTO(
    UUID tileId,
    String cityName,
    String cityCode,
    String chokepointName,
    String chokepointCode,
    String placeTypeCode,
    String trafficTier,
    String manufacturerName,
    String size,
    String color,
    LocalDate installationDate,
    LocalDate removalDate,
    LocalDate lastInspectionDate,
    boolean active,
    Instant firstCompressionAt,
    Instant lastCompressionAt,
    long totalCompressions) {}
