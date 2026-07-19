package com.projectkinetile.physicsengine.api.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.projectkinetile.physicsengine.api.CompressionDailySeriesBucketDTO;
import com.projectkinetile.physicsengine.repository.DailyCompressionBucket;
import com.projectkinetile.physicsengine.repository.DailyCompressionCityBucket;
import com.projectkinetile.physicsengine.repository.DailyCompressionLocationBucket;

/** Maps repository daily aggregate projections into compression analytics DTOs. */
public final class CompressionAnalyticsRowMapper {

  private CompressionAnalyticsRowMapper() {}

  /** Maps a network-total daily aggregate row into a series bucket DTO. */
  public static CompressionDailySeriesBucketDTO mapDailyTotalRow(DailyCompressionBucket row) {
    return new CompressionDailySeriesBucketDTO(
        startOfUtcDay(row.bucketDate()),
        null,
        null,
        doubleValueOrZero(row.totalJoules()),
        EnergyUnitConverter.wattHoursFromJoules(doubleValueOrZero(row.totalJoules())),
        longValueOrZero(row.totalCompressions()),
        longValueOrZero(row.successfulActivations()));
  }

  /** Maps a per-city daily aggregate row into a series bucket DTO. */
  public static CompressionDailySeriesBucketDTO mapDailyCityRow(DailyCompressionCityBucket row) {
    double totalJoules = doubleValueOrZero(row.totalJoules());
    return new CompressionDailySeriesBucketDTO(
        startOfUtcDay(row.bucketDate()),
        normalizeLabel(row.city()),
        null,
        totalJoules,
        EnergyUnitConverter.wattHoursFromJoules(totalJoules),
        longValueOrZero(row.totalCompressions()),
        longValueOrZero(row.successfulActivations()));
  }

  /** Maps a per-location daily aggregate row into a series bucket DTO. */
  public static CompressionDailySeriesBucketDTO mapDailyLocationRow(
      DailyCompressionLocationBucket row) {
    double totalJoules = doubleValueOrZero(row.totalJoules());
    return new CompressionDailySeriesBucketDTO(
        startOfUtcDay(row.bucketDate()),
        normalizeLabel(row.city()),
        normalizeLabel(row.location()),
        totalJoules,
        EnergyUnitConverter.wattHoursFromJoules(totalJoules),
        longValueOrZero(row.totalCompressions()),
        longValueOrZero(row.successfulActivations()));
  }

  /** Normalizes a possibly null or blank label to a safe display value. */
  public static String normalizeLabel(String value) {
    if (value == null || value.isBlank()) {
      return "Unknown";
    }
    return value;
  }

  /** Safely extracts a double from a possibly null number. */
  public static double doubleValueOrZero(Number n) {
    return n != null ? n.doubleValue() : 0.0;
  }

  /** Safely extracts a double from a possibly null, non-numeric value. */
  public static double doubleValueOrZero(Object n) {
    if (n instanceof Number number) {
      return number.doubleValue();
    }
    return 0.0;
  }

  /** Safely extracts a long from a possibly null number. */
  public static long longValueOrZero(Number n) {
    return n != null ? n.longValue() : 0L;
  }

  private static Instant startOfUtcDay(LocalDate bucketDate) {
    if (bucketDate == null) {
      return Instant.EPOCH;
    }
    return bucketDate.atStartOfDay(ZoneOffset.UTC).toInstant();
  }
}
