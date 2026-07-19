package com.projectkinetile.physicsengine.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.projectkinetile.physicsengine.domain.TileCompressionActivityEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;

/**
 * Atomically records per-tile compression activity.
 *
 * <p>PostgreSQL uses a single {@code INSERT ... ON CONFLICT DO UPDATE} statement. The H2 test
 * profile falls back to a pessimistic row lock so integration tests stay portable.
 */
@Component
public class TileCompressionActivityIncrementer {

  private static final String POSTGRES_INCREMENT_SQL =
      """
      INSERT INTO tile_compression_activity (
        tile_id, first_compression_at, last_compression_at, total_compressions
      ) VALUES (?, ?, ?, 1)
      ON CONFLICT (tile_id) DO UPDATE SET
        last_compression_at = GREATEST(
          tile_compression_activity.last_compression_at, EXCLUDED.last_compression_at),
        total_compressions = tile_compression_activity.total_compressions + 1
      """;

  private final JdbcTemplate jdbcTemplate;
  private final EntityManager entityManager;
  private final boolean postgres;

  public TileCompressionActivityIncrementer(
      @NonNull DataSource dataSource, @NonNull EntityManager entityManager) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.entityManager = entityManager;
    this.postgres = isPostgres(dataSource);
  }

  /** Inserts or increments activity for one tile in a concurrency-safe way. */
  @Transactional
  public void increment(@NonNull UUID tileId, Instant eventTimestamp) {
    if (postgres) {
      OffsetDateTime atUtc = OffsetDateTime.ofInstant(eventTimestamp, ZoneOffset.UTC);
      jdbcTemplate.update(POSTGRES_INCREMENT_SQL, tileId, atUtc, atUtc);
      return;
    }
    incrementWithPessimisticLock(tileId, eventTimestamp);
  }

  private void incrementWithPessimisticLock(UUID tileId, Instant eventTimestamp) {
    try {
      TileCompressionActivityEntity activity =
          entityManager
              .createQuery(
                  """
                  SELECT a FROM TileCompressionActivityEntity a
                  WHERE a.tileId = :tileId
                  """,
                  TileCompressionActivityEntity.class)
              .setParameter("tileId", tileId)
              .setLockMode(LockModeType.PESSIMISTIC_WRITE)
              .getSingleResult();
      activity.setLastCompressionAt(
          activity.getLastCompressionAt().isAfter(eventTimestamp)
              ? activity.getLastCompressionAt()
              : eventTimestamp);
      activity.setTotalCompressions(activity.getTotalCompressions() + 1);
      entityManager.merge(activity);
    } catch (NoResultException ex) {
      entityManager.persist(
          new TileCompressionActivityEntity(tileId, eventTimestamp, eventTimestamp, 1L));
    }
  }

  private static boolean isPostgres(@NonNull DataSource dataSource) {
    try (var connection = dataSource.getConnection()) {
      String url = connection.getMetaData().getURL();
      return url != null && url.toLowerCase().contains("postgresql");
    } catch (Exception ex) {
      return false;
    }
  }
}
