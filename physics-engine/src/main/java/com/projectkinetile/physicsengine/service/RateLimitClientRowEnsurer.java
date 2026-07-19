package com.projectkinetile.physicsengine.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Ensures a per-IP rate-limit row exists before pessimistic locking.
 *
 * <p>PostgreSQL uses {@code INSERT ... ON CONFLICT DO NOTHING}. H2 tests insert and ignore duplicate
 * key races so concurrent first requests for the same IP remain safe.
 */
@Component
public class RateLimitClientRowEnsurer {

  private static final String INSERT_COLUMNS_SQL =
      """
      INSERT INTO api_rate_limit_clients (
        client_ip, window_start, request_count, violation_count,
        permanently_blocked, last_seen_at
      ) VALUES (?, ?, 0, 0, FALSE, ?)
      """;

  private static final String POSTGRES_INSERT_IF_ABSENT_SQL =
      INSERT_COLUMNS_SQL + " ON CONFLICT (client_ip) DO NOTHING";

  private final JdbcTemplate jdbcTemplate;
  private final boolean postgres;

  public RateLimitClientRowEnsurer(@NonNull DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.postgres = isPostgres(dataSource);
  }

  /**
   * Creates the client row when missing without overwriting existing penalty state.
   *
   * @param clientIp resolved client identifier
   * @param now timestamp used for {@code window_start} and {@code last_seen_at} on first insert
   */
  public void ensureExists(@NonNull String clientIp, @NonNull Instant now) {
    // JdbcTemplate cannot bind Instant for PostgreSQL; OffsetDateTime maps to TIMESTAMPTZ.
    OffsetDateTime atUtc = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
    if (postgres) {
      jdbcTemplate.update(POSTGRES_INSERT_IF_ABSENT_SQL, clientIp, atUtc, atUtc);
      return;
    }
    try {
      jdbcTemplate.update(INSERT_COLUMNS_SQL, clientIp, atUtc, atUtc);
    } catch (DuplicateKeyException ignored) {
      // Another transaction won the first-insert race.
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
