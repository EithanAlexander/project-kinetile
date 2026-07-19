package com.projectkinetile.physicsengine.api;

/**
 * Aggregated tile compression metrics for one city + chokepoint pair.
 *
 * @param city municipality or region label
 * @param location site / chokepoint name within the city
 * @param totalJoules summed generated energy in joules
 * @param totalWattHours {@code totalJoules / 3600}, rounded to three decimal places
 * @param totalCompressions count of all compression events in the group
 * @param successfulActivations count of threshold-met activations in the group
 */
public record LocationCompressionMetricsDTO(
    String city,
    String location,
    double totalJoules,
    double totalWattHours,
    long totalCompressions,
    long successfulActivations) {}
