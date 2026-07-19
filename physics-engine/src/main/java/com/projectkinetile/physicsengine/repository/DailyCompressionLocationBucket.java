package com.projectkinetile.physicsengine.repository;

import java.time.LocalDate;

/**
 * Per-location daily compression aggregate returned by {@link TileCompressionEventRepository}.
 *
 * @param bucketDate UTC calendar day for the bucket
 * @param city catalog city name
 * @param location chokepoint name
 * @param totalJoules summed harvested energy in Joules
 * @param totalCompressions count of compression events
 * @param successfulActivations count of events that met the activation threshold
 */
public record DailyCompressionLocationBucket(
    LocalDate bucketDate,
    String city,
    String location,
    Double totalJoules,
    Long totalCompressions,
    Long successfulActivations) {}
