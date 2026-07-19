package com.projectkinetile.physicsengine.repository;

import java.time.Instant;

/**
 * Immutable set of optional filters applied to tile compression energy-ledger queries.
 *
 * <p>Each component is independent and {@code null}-tolerant: a {@code null} (or, for text fields,
 * blank) value means the corresponding constraint is omitted. When multiple filters are supplied
 * they are combined with logical {@code AND} semantics. Supplying no filters at all matches every
 * persisted row.
 *
 * <p>Instances are consumed by {@link TileCompressionEventSpecifications#withOptionalFilters} to
 * build the JPA {@code Specification} used for querying the ledger.
 *
 * @param locationContains case-insensitive substring matched against either the location or the
 *     city of an event; {@code null}/blank disables the filter. The fragment is escaped before use
 *     so SQL {@code LIKE} wildcards in user input are treated literally.
 * @param since inclusive lower bound on the event timestamp; {@code null} leaves the start open.
 * @param until inclusive upper bound on the event timestamp; {@code null} leaves the end open.
 * @param minEnergyJoules inclusive lower bound on calculated harvested energy in joules;
 *     {@code null} disables the lower bound.
 * @param maxEnergyJoules inclusive upper bound on calculated harvested energy in joules;
 *     {@code null} disables the upper bound.
 * @param minImpactMultiplier inclusive lower bound on the event impact multiplier; {@code null}
 *     disables the lower bound.
 * @param maxImpactMultiplier inclusive upper bound on the event impact multiplier; {@code null}
 *     disables the upper bound.
 * @param activationOnly when {@code Boolean.TRUE}, restricts results to events whose tile
 *     activation succeeded; {@code null} or {@code Boolean.FALSE} includes all events.
 * @param eventIdPrefix case-insensitive prefix matched against the event identifier; {@code null}/
 *     blank disables the filter. The fragment is escaped so {@code LIKE} wildcards are treated
 *     literally.
 * @param tileIdPrefix case-insensitive prefix matched against the tile UUID string; {@code null}/
 *     blank disables the filter.
 */
public record TileCompressionEventLedgerFilterCriteria(
    String locationContains,
    Instant since,
    Instant until,
    Double minEnergyJoules,
    Double maxEnergyJoules,
    Double minImpactMultiplier,
    Double maxImpactMultiplier,
    Boolean activationOnly,
    String eventIdPrefix,
    String tileIdPrefix) {}
