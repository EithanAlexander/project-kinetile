package com.projectkinetile.physicsengine.repository;

import java.time.LocalDate;

/**
 * Per-city daily compression aggregate returned by {@link TileCompressionEventRepository}.
 *
 * @param bucketDate UTC calendar day for the bucket
 * @param city catalog city name
 * @param totalJoules summed harvested energy in Joules
 * @param totalCompressions count of compression events
 * @param successfulActivations count of events that met the activation threshold
 */
public record DailyCompressionCityBucket(
    LocalDate bucketDate,
    String city,
    Double totalJoules,
    Long totalCompressions,
    Long successfulActivations) {}
