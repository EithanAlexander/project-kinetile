package com.projectkinetile.physicsengine.repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;

/**
 * Spring Data JPA repository for persisted {@link TileCompressionEventEntity} rows.
 *
 * <p>Each event references a catalog {@code tile} by foreign key; city, chokepoint (location), and
 * manufacturer labels are resolved through joins — they are not denormalized on the event row.
 *
 * <p>Supports three access patterns:
 * <ul>
 *   <li>Ledger pagination with optional {@link Specification} filters and eager catalog fetches
 *   <li>Idempotent ingestion lookups by Kafka {@code event_id}
 *   <li>Analytics aggregates and daily time-series buckets for the compression dashboard API
 * </ul>
 */
public interface TileCompressionEventRepository
    extends JpaRepository<TileCompressionEventEntity, Long>,
        JpaSpecificationExecutor<TileCompressionEventEntity> {

  /**
   * Finds a persisted event by its upstream idempotency key.
   *
   * @param eventId unique event identifier from the Kafka payload
   * @return the matching row, if already stored
   */
  Optional<TileCompressionEventEntity> findByEventId(String eventId);

  /**
   * Paginated ledger query with eager fetches for tile catalog associations.
   *
   * <p>Overrides the default {@link JpaSpecificationExecutor#findAll(Specification, Pageable)}
   * implementation so each page load includes {@code tile}, chokepoint, city, place type, and
   * manufacturer without N+1 queries. Used by {@code TileCompressionLedgerQueryService}.
   *
   * @param spec optional filter specification; {@code null} returns all events
   * @param pageable page index, size, and sort
   * @return matching events with catalog graph initialized
   */
  @Override
  @EntityGraph(
      attributePaths = {
        "tile",
        "tile.chokepoint",
        "tile.chokepoint.city",
        "tile.chokepoint.placeType",
        "tile.manufacturer"
      })
  @NonNull
  Page<TileCompressionEventEntity> findAll(
      @Nullable Specification<TileCompressionEventEntity> spec, @NonNull Pageable pageable);

  /**
   * Aggregates harvested energy and activation counts grouped by city and chokepoint.
   *
   * @return one map per city/location pair with keys {@code city}, {@code location},
   *     {@code totalJoules}, {@code totalCompressions}, and {@code successfulActivations}
   */
  @Query(
      """
      SELECT new map(c.name as city, cp.name as location,
          SUM(t.calculatedEnergyJoules) as totalJoules,
          COUNT(t.id) as totalCompressions,
          SUM(CASE WHEN t.activationSuccessful = true THEN 1 ELSE 0 END) as successfulActivations)
      FROM TileCompressionEventEntity t
      JOIN t.tile tile
      JOIN tile.chokepoint cp
      JOIN cp.city c
      GROUP BY c.name, cp.name
      """)
  List<Map<String, Object>> aggregateByLocation();

  /**
   * Returns network-wide totals across all stored compression events.
   *
   * @return a single map with keys {@code totalJoules}, {@code totalCompressions}, and
   *     {@code successfulActivations}
   */
  @Query(
      """
      SELECT SUM(t.calculatedEnergyJoules) as totalJoules,
          COUNT(t.id) as totalCompressions,
          SUM(CASE WHEN t.activationSuccessful = true THEN 1 ELSE 0 END) as successfulActivations
      FROM TileCompressionEventEntity t
      """)
  Map<String, Object> aggregateNetworkSummary();

  /**
   * Daily time-series buckets for the entire network within an inclusive timestamp window.
   *
   * @param since inclusive lower bound on {@code eventTimestamp}
   * @param until inclusive upper bound on {@code eventTimestamp}
   * @return rows ordered by calendar day
   */
  @Query(
      """
      SELECT new com.projectkinetile.physicsengine.repository.DailyCompressionBucket(
          CAST(t.eventTimestamp AS localdate),
          SUM(t.calculatedEnergyJoules),
          COUNT(t.id),
          SUM(CASE WHEN t.activationSuccessful = true THEN 1 ELSE 0 END))
      FROM TileCompressionEventEntity t
      WHERE t.eventTimestamp >= :since AND t.eventTimestamp <= :until
      GROUP BY CAST(t.eventTimestamp AS localdate)
      ORDER BY CAST(t.eventTimestamp AS localdate)
      """)
  List<DailyCompressionBucket> sumJoulesGroupedByDay(
      @Param("since") Instant since, @Param("until") Instant until);

  /**
   * Daily time-series buckets grouped by city within an inclusive timestamp window.
   *
   * @param since inclusive lower bound on {@code eventTimestamp}
   * @param until inclusive upper bound on {@code eventTimestamp}
   * @return rows ordered by day then city
   */
  @Query(
      """
      SELECT new com.projectkinetile.physicsengine.repository.DailyCompressionCityBucket(
          CAST(t.eventTimestamp AS localdate),
          c.name,
          SUM(t.calculatedEnergyJoules),
          COUNT(t.id),
          SUM(CASE WHEN t.activationSuccessful = true THEN 1 ELSE 0 END))
      FROM TileCompressionEventEntity t
      JOIN t.tile tile
      JOIN tile.chokepoint cp
      JOIN cp.city c
      WHERE t.eventTimestamp >= :since AND t.eventTimestamp <= :until
      GROUP BY CAST(t.eventTimestamp AS localdate), c.name
      ORDER BY CAST(t.eventTimestamp AS localdate), c.name
      """)
  List<DailyCompressionCityBucket> sumJoulesGroupedByDayAndCity(
      @Param("since") Instant since, @Param("until") Instant until);

  /**
   * Daily time-series buckets for chokepoints within a single city.
   *
   * <p>City matching is case-insensitive and trims surrounding whitespace.
   *
   * @param since inclusive lower bound on {@code eventTimestamp}
   * @param until inclusive upper bound on {@code eventTimestamp}
   * @param city city name filter (case-insensitive)
   * @return rows ordered by day then location
   */
  @Query(
      """
      SELECT new com.projectkinetile.physicsengine.repository.DailyCompressionLocationBucket(
          CAST(t.eventTimestamp AS localdate),
          c.name,
          cp.name,
          SUM(t.calculatedEnergyJoules),
          COUNT(t.id),
          SUM(CASE WHEN t.activationSuccessful = true THEN 1 ELSE 0 END))
      FROM TileCompressionEventEntity t
      JOIN t.tile tile
      JOIN tile.chokepoint cp
      JOIN cp.city c
      WHERE t.eventTimestamp >= :since AND t.eventTimestamp <= :until
      AND LOWER(TRIM(c.name)) = LOWER(TRIM(:city))
      GROUP BY CAST(t.eventTimestamp AS localdate), c.name, cp.name
      ORDER BY CAST(t.eventTimestamp AS localdate), cp.name
      """)
  List<DailyCompressionLocationBucket> sumJoulesGroupedByDayCityAndLocation(
      @Param("since") Instant since, @Param("until") Instant until, @Param("city") String city);
}
