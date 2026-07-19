package com.projectkinetile.physicsengine.api;

/**
 * Network-wide compression activation totals.
 *
 * @param totalCompressions all persisted compression events
 * @param successfulActivations compressions where force met the hardware threshold
 * @param totalJoules summed generated energy in joules
 * @param totalWattHours {@code totalJoules / 3600}, rounded to three decimal places
 */
public record CompressionSummaryDTO(
    long totalCompressions,
    long successfulActivations,
    double totalJoules,
    double totalWattHours) {}
