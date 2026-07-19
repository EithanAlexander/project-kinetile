package com.projectkinetile.physicsengine.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.domain.TileEntity;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/** JPA specifications for filtering persisted tile compression rows (energy ledger). */
public final class TileCompressionEventSpecifications {

  private TileCompressionEventSpecifications() {}

  /**
   * Composes optional-filter specification from {@code criteria}. Omitted fields are ignored.
   */
  public static Specification<TileCompressionEventEntity> withOptionalFilters(
      TileCompressionEventLedgerFilterCriteria criteria) {
    return (root, query, cb) -> {
      if (query != null && Long.class != query.getResultType()) {
        root.fetch("tile", JoinType.INNER).fetch("chokepoint", JoinType.INNER).fetch("city", JoinType.INNER);
        root.fetch("tile", JoinType.INNER).fetch("manufacturer", JoinType.INNER);
        query.distinct(true);
      }
      List<Predicate> predicates = new ArrayList<>();
      addLocationContainsIfPresent(predicates, cb, root, criteria.locationContains());
      addTimestampRangeIfPresent(
          predicates, cb, root, criteria.since(), criteria.until());
      addEnergyRangeIfPresent(
          predicates, cb, root, criteria.minEnergyJoules(), criteria.maxEnergyJoules());
      addImpactMultiplierRangeIfPresent(
          predicates, cb, root, criteria.minImpactMultiplier(), criteria.maxImpactMultiplier());
      addActivationOnlyIfPresent(predicates, cb, root, criteria.activationOnly());
      addEventIdPrefixIfPresent(predicates, cb, root, criteria.eventIdPrefix());
      addTileIdPrefixIfPresent(predicates, cb, root, criteria.tileIdPrefix());

      if (predicates.isEmpty()) {
        return cb.conjunction();
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  /** Adds a case-insensitive substring match over chokepoint or city name, if supplied. */
  private static void addLocationContainsIfPresent(
      List<Predicate> predicates,
      CriteriaBuilder cb,
      Root<TileCompressionEventEntity> root,
      String locationContains) {
    if (locationContains == null || locationContains.isBlank()) {
      return;
    }
    String pattern =
        "%" + quoteSqlLikeUserFragment(locationContains.trim()).toLowerCase() + "%";
    Join<TileCompressionEventEntity, TileEntity> tile = root.join("tile", JoinType.INNER);
    var chokepoint = tile.join("chokepoint", JoinType.INNER);
    var city = chokepoint.join("city", JoinType.INNER);
    Expression<String> locLower = cb.lower(chokepoint.get("name"));
    Expression<String> cityLower = cb.lower(city.get("name"));
    predicates.add(cb.or(cb.like(locLower, pattern, '\\'), cb.like(cityLower, pattern, '\\')));
  }

  /** Adds inclusive lower/upper bounds on {@code eventTimestamp} for any non-null endpoint. */
  private static void addTimestampRangeIfPresent(
      List<Predicate> predicates,
      CriteriaBuilder cb,
      Root<TileCompressionEventEntity> root,
      Instant since,
      Instant until) {
    if (since != null) {
      predicates.add(cb.greaterThanOrEqualTo(root.get("eventTimestamp"), since));
    }
    if (until != null) {
      predicates.add(cb.lessThanOrEqualTo(root.get("eventTimestamp"), until));
    }
  }

  /** Adds inclusive lower/upper bounds on {@code calculatedEnergyJoules} for any non-null bound. */
  private static void addEnergyRangeIfPresent(
      List<Predicate> predicates,
      CriteriaBuilder cb,
      Root<TileCompressionEventEntity> root,
      Double minEnergyJoules,
      Double maxEnergyJoules) {
    if (minEnergyJoules != null) {
      predicates.add(cb.ge(root.get("calculatedEnergyJoules"), minEnergyJoules));
    }
    if (maxEnergyJoules != null) {
      predicates.add(cb.le(root.get("calculatedEnergyJoules"), maxEnergyJoules));
    }
  }

  /** Adds inclusive lower/upper bounds on {@code impactMultiplier} for any non-null bound. */
  private static void addImpactMultiplierRangeIfPresent(
      List<Predicate> predicates,
      CriteriaBuilder cb,
      Root<TileCompressionEventEntity> root,
      Double minImpactMultiplier,
      Double maxImpactMultiplier) {
    if (minImpactMultiplier != null) {
      predicates.add(cb.ge(root.get("impactMultiplier"), minImpactMultiplier));
    }
    if (maxImpactMultiplier != null) {
      predicates.add(cb.le(root.get("impactMultiplier"), maxImpactMultiplier));
    }
  }

  /** Restricts to events with a successful activation when {@code activationOnly} is true. */
  private static void addActivationOnlyIfPresent(
      List<Predicate> predicates,
      CriteriaBuilder cb,
      Root<TileCompressionEventEntity> root,
      Boolean activationOnly) {
    if (activationOnly == null || !activationOnly) {
      return;
    }
    predicates.add(cb.isTrue(root.get("activationSuccessful")));
  }

  /** Adds a case-insensitive prefix match over {@code eventId}, if supplied. */
  private static void addEventIdPrefixIfPresent(
      List<Predicate> predicates,
      CriteriaBuilder cb,
      Root<TileCompressionEventEntity> root,
      String eventIdPrefix) {
    if (eventIdPrefix == null || eventIdPrefix.isBlank()) {
      return;
    }
    String pattern = quoteSqlLikeUserFragment(eventIdPrefix.trim()).toLowerCase() + "%";
    predicates.add(cb.like(cb.lower(root.get("eventId")), pattern, '\\'));
  }

  /** Adds a case-insensitive prefix match over {@code tileId}, if supplied. */
  private static void addTileIdPrefixIfPresent(
      List<Predicate> predicates,
      CriteriaBuilder cb,
      Root<TileCompressionEventEntity> root,
      String tileIdPrefix) {
    if (tileIdPrefix == null || tileIdPrefix.isBlank()) {
      return;
    }
    String pattern = quoteSqlLikeUserFragment(tileIdPrefix.trim()).toLowerCase() + "%";
    predicates.add(cb.like(cb.lower(root.get("tileId").as(String.class)), pattern, '\\'));
  }

  /** Escapes {@code \}, {@code %} and {@code _} so user input is treated literally in LIKE. */
  private static String quoteSqlLikeUserFragment(String raw) {
    return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
