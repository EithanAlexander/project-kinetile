package com.projectkinetile.physicsengine.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Tile inventory row for catalog APIs. */
public record TileCatalogDTO(
    UUID tileId,
    String manufacturerName,
    String size,
    String color,
    LocalDate installationDate,
    LocalDate removalDate,
    LocalDate lastInspectionDate,
    boolean active,
    Instant lastCompressionAt,
    long totalCompressions) {}
