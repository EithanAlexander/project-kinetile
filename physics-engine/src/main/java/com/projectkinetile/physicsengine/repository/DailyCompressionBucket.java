package com.projectkinetile.physicsengine.repository;

import java.time.LocalDate;

/**
 * Network-wide daily compression aggregate returned by {@link TileCompressionEventRepository}.
 *
 * @param bucketDate UTC calendar day for the bucket
 * @param totalJoules summed harvested energy in Joules
 * @param totalCompressions count of compression events
 * @param successfulActivations count of events that met the activation threshold
 */
public record DailyCompressionBucket(
    LocalDate bucketDate,
    Double totalJoules,
    Long totalCompressions,
    Long successfulActivations) {}
