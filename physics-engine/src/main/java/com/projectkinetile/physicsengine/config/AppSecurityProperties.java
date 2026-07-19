package com.projectkinetile.physicsengine.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

/**
 * Application security and operational limits (CORS, rate limiting, Kafka bounds, REST validation).
 *
 * <p>Defaults and overrides live in {@code application.yml} under the {@code app.*} prefix. Most
 * values can also be set via environment variables (see field Javadoc on nested classes).
 */
@ConfigurationProperties(prefix = "app")
public class AppSecurityProperties {

  private final Cors cors = new Cors();
  private final RateLimit rateLimit = new RateLimit();
  private final Kafka kafka = new Kafka();
  private final Api api = new Api();

  public Cors getCors() {
    return cors;
  }

  public RateLimit getRateLimit() {
    return rateLimit;
  }

  public Kafka getKafka() {
    return kafka;
  }

  public Api getApi() {
    return api;
  }

  /** Allowed browser origins for cross-origin API access. */
  public static class Cors {
    /**
     * Comma-separated allowed origins ({@code app.cors.allowed-origins} /
     * {@code APP_CORS_ALLOWED_ORIGINS}).
     */
    private String allowedOrigins;

    public String getAllowedOrigins() {
      return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
      this.allowedOrigins = allowedOrigins;
    }

    public List<String> allowedOriginList() {
      if (allowedOrigins == null || allowedOrigins.isBlank()) {
        return List.of();
      }
      return Arrays.stream(allowedOrigins.split(","))
          .map(Cors::trimOrigin)
          .filter(origin -> !origin.isEmpty())
          .toList();
    }

    private static String trimOrigin(@NonNull String origin) {
      return origin.trim();
    }
  }

  /** Postgres-backed per-IP REST rate limiting (shared across replicas). */
  public static class RateLimit {
    /** {@code app.rate-limit.enabled} / {@code APP_RATE_LIMIT_ENABLED}. */
    private boolean enabled;

    /** {@code app.rate-limit.requests-per-minute} / {@code APP_RATE_LIMIT_RPM}. */
    private int requestsPerMinute;

    /**
     * Escalating penalty durations in minutes ({@code app.rate-limit.penalty-escalation-minutes}).
     *
     * <p>Configure as a YAML list; override via env as comma-separated values
     * ({@code APP_RATE_LIMIT_PENALTY_ESCALATION_MINUTES=5,15,60,1440}).
     */
    private List<Integer> penaltyEscalationMinutes = List.of(5, 15, 60, 1440);

    /** Violations within the window before a permanent block ({@code permanent-after-violations}). */
    private int permanentAfterViolations = 5;

    /** Rolling window for violation counting in hours ({@code violation-window-hours}). */
    private int violationWindowHours = 24;

    /** Idle benign row TTL in minutes ({@code entry-ttl-minutes}). */
    private int entryTtlMinutes = 120;

    /** Purge scheduler interval in minutes ({@code cleanup-interval-minutes}). */
    private int cleanupIntervalMinutes = 30;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getRequestsPerMinute() {
      return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
      this.requestsPerMinute = requestsPerMinute;
    }

    public List<Integer> getPenaltyEscalationMinutes() {
      if (penaltyEscalationMinutes == null || penaltyEscalationMinutes.isEmpty()) {
        return List.of(5, 15, 60, 1440);
      }
      return penaltyEscalationMinutes;
    }

    public void setPenaltyEscalationMinutes(List<Integer> penaltyEscalationMinutes) {
      this.penaltyEscalationMinutes = penaltyEscalationMinutes;
    }

    public int getPermanentAfterViolations() {
      return permanentAfterViolations;
    }

    public void setPermanentAfterViolations(int permanentAfterViolations) {
      this.permanentAfterViolations = permanentAfterViolations;
    }

    public int getViolationWindowHours() {
      return violationWindowHours;
    }

    public void setViolationWindowHours(int violationWindowHours) {
      this.violationWindowHours = violationWindowHours;
    }

    public int getEntryTtlMinutes() {
      return entryTtlMinutes;
    }

    public void setEntryTtlMinutes(int entryTtlMinutes) {
      this.entryTtlMinutes = entryTtlMinutes;
    }

    public int getCleanupIntervalMinutes() {
      return cleanupIntervalMinutes;
    }

    public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
      this.cleanupIntervalMinutes = cleanupIntervalMinutes;
    }
  }

  /** Kafka consumer hardening. */
  public static class Kafka {
    private final Consumer consumer = new Consumer();
    private final Topics topics = new Topics();

    public Consumer getConsumer() {
      return consumer;
    }

    public Topics getTopics() {
      return topics;
    }

    public static class Consumer {
      /** {@code app.kafka.consumer.max-payload-bytes} / {@code APP_KAFKA_MAX_PAYLOAD_BYTES}. */
      private int maxPayloadBytes;

      /** Max listener retries before DLQ ({@code app.kafka.consumer.error-max-retries}). */
      private int errorMaxRetries = 2;

      /** Backoff between listener retries in ms ({@code app.kafka.consumer.error-backoff-ms}). */
      private long errorBackoffMs = 500L;

      public int getMaxPayloadBytes() {
        return maxPayloadBytes;
      }

      public void setMaxPayloadBytes(int maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
      }

      public int getErrorMaxRetries() {
        return errorMaxRetries;
      }

      public void setErrorMaxRetries(int errorMaxRetries) {
        this.errorMaxRetries = errorMaxRetries;
      }

      public long getErrorBackoffMs() {
        return errorBackoffMs;
      }

      public void setErrorBackoffMs(long errorBackoffMs) {
        this.errorBackoffMs = errorBackoffMs;
      }
    }

    public static class Topics {
      /** {@code app.kafka.topics.raw-traffic} / {@code APP_KAFKA_RAW_TRAFFIC_TOPIC}. */
      private String rawTraffic;

      /** {@code app.kafka.topics.raw-traffic-dlq} / {@code APP_KAFKA_RAW_TRAFFIC_DLQ_TOPIC}. */
      private String rawTrafficDlq;

      public String getRawTraffic() {
        return rawTraffic;
      }

      public void setRawTraffic(String rawTraffic) {
        this.rawTraffic = rawTraffic;
      }

      public String getRawTrafficDlq() {
        return rawTrafficDlq;
      }

      public void setRawTrafficDlq(String rawTrafficDlq) {
        this.rawTrafficDlq = rawTrafficDlq;
      }
    }
  }

  /** REST query validation limits. */
  public static class Api {
    /** {@code app.api.max-query-string-length} / {@code APP_API_MAX_QUERY_STRING_LENGTH}. */
    private int maxQueryStringLength;

    /** {@code app.api.timeseries-max-months} / {@code APP_API_TIMESERIES_MAX_MONTHS}. */
    private int timeseriesMaxMonths;

    /**
     * Maximum page size accepted by paginated ledger endpoints; requested sizes are clamped to this
     * upper bound to limit per-request resource usage ({@code app.api.ledger-max-page-size} /
     * {@code APP_API_LEDGER_MAX_PAGE_SIZE}).
     */
    private int ledgerMaxPageSize;

    public int getMaxQueryStringLength() {
      return maxQueryStringLength;
    }

    public void setMaxQueryStringLength(int maxQueryStringLength) {
      this.maxQueryStringLength = maxQueryStringLength;
    }

    public int getTimeseriesMaxMonths() {
      return timeseriesMaxMonths;
    }

    public void setTimeseriesMaxMonths(int timeseriesMaxMonths) {
      this.timeseriesMaxMonths = timeseriesMaxMonths;
    }

    public int getLedgerMaxPageSize() {
      return ledgerMaxPageSize;
    }

    public void setLedgerMaxPageSize(int ledgerMaxPageSize) {
      this.ledgerMaxPageSize = ledgerMaxPageSize;
    }
  }
}
