package com.projectkinetile.physicsengine.api;

import java.time.Instant;

/**
 * One calendar-day bucket of aggregated compression energy with compression counts.
 */
public record CompressionDailySeriesBucketDTO(
    Instant bucketStart,
    String city,
    String location,
    double totalJoules,
    double totalWattHours,
    long totalCompressions,
    long successfulActivations) {}
