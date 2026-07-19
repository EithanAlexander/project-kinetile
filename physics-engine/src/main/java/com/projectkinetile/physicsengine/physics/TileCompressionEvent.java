package com.projectkinetile.physicsengine.physics;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.projectkinetile.physicsengine.domain.TileCompressionEventType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Single hardware-level tile compression deserialized from Kafka JSON (snake_case keys).
 *
 * @param eventId unique identifier for this compression (UUID from ingestion)
 * @param eventType discriminant; must be {@link TileCompressionEventType#TILE_COMPRESSION}
 * @param tileId physical tile UUID registered in the infrastructure catalog
 * @param massKg load mass in kilograms used for force calculation
 * @param impactMultiplier dynamic footfall multiplier (1.0–1.5× body weight)
 * @param timestamp instant when the compression occurred (ISO-8601 UTC)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TileCompressionEvent(
    @NotBlank @Size(max = 128) String eventId,
    @NotNull TileCompressionEventType eventType,
    @NotNull UUID tileId,
    @Positive @Max(500) double massKg,
    @DecimalMin("1.0") @DecimalMax("1.5") double impactMultiplier,
    @NotNull @PastOrPresent Instant timestamp) {}
