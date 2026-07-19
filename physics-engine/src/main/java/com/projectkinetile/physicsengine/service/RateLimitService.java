package com.projectkinetile.physicsengine.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.domain.ApiRateLimitClientEntity;
import com.projectkinetile.physicsengine.repository.ApiRateLimitClientRepository;

/**
 * Postgres-backed per-IP rate limiting with escalating penalties and permanent blocks.
 *
 * <p>State is shared across replicas so limits are consistent in multi-pod deployments. Each
 * {@link #tryConsume} call atomically ensures the client row exists, loads it with a pessimistic
 * write lock, then applies quota and penalty rules in the same transaction.
 */
@Service
public class RateLimitService {

  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final ApiRateLimitClientRepository repository;
  private final RateLimitClientRowEnsurer rowEnsurer;
  private final AppSecurityProperties appSecurityProperties;
  private final Clock clock;

  public RateLimitService(
      ApiRateLimitClientRepository repository,
      RateLimitClientRowEnsurer rowEnsurer,
      AppSecurityProperties appSecurityProperties,
      Clock clock) {
    this.repository = repository;
    this.rowEnsurer = rowEnsurer;
    this.appSecurityProperties = appSecurityProperties;
    this.clock = clock;
  }

  /**
   * Attempts to consume one request quota for the given client IP.
   *
   * @param clientIp resolved client identifier
   * @return {@code true} when the request is allowed; {@code false} when rate limited
   */
  @Transactional
  public boolean tryConsume(@NonNull String clientIp) {
    Instant now = Objects.requireNonNull(clock.instant(), "clock");
    rowEnsurer.ensureExists(clientIp, now);
    ApiRateLimitClientEntity state =
        repository
            .findByClientIp(clientIp)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Rate-limit row missing after ensure for client: " + clientIp));

    state.setLastSeenAt(now);

    if (state.isPermanentlyBlocked()) {
      repository.save(state);
      return false;
    }

    if (state.getPenaltyUntil() != null && now.isBefore(state.getPenaltyUntil())) {
      repository.save(state);
      return false;
    }

    resetViolationWindowIfExpired(state, now);
    resetRequestWindowIfExpired(state, now);

    int limit = Math.max(1, appSecurityProperties.getRateLimit().getRequestsPerMinute());
    int nextCount = state.getRequestCount() + 1;
    state.setRequestCount(nextCount);

    if (nextCount > limit) {
      applyPenalty(state, now);
      repository.save(state);
      return false;
    }

    repository.save(state);
    return true;
  }

  /** Removes idle benign client rows older than the configured TTL. */
  @Transactional
  public int purgeIdleClients() {
    Instant now = clock.instant();
    Instant cutoff =
        now.minus(appSecurityProperties.getRateLimit().getEntryTtlMinutes(), ChronoUnit.MINUTES);
    return repository.deleteIdleBenignClients(cutoff, now);
  }

  private void resetViolationWindowIfExpired(ApiRateLimitClientEntity state, Instant now) {
    Instant firstViolation = state.getFirstViolationAt();
    if (firstViolation == null) {
      return;
    }
    long violationWindowHours = appSecurityProperties.getRateLimit().getViolationWindowHours();
    if (Duration.between(firstViolation, now).toHours() >= violationWindowHours) {
      state.setViolationCount(0);
      state.setFirstViolationAt(null);
    }
  }

  private void resetRequestWindowIfExpired(ApiRateLimitClientEntity state, Instant now) {
    if (Duration.between(state.getWindowStart(), now).compareTo(WINDOW) >= 0) {
      state.setWindowStart(now);
      state.setRequestCount(0);
    }
  }

  private void applyPenalty(ApiRateLimitClientEntity state, Instant now) {
    if (state.getFirstViolationAt() == null) {
      state.setFirstViolationAt(now);
    }
    int violations = state.getViolationCount() + 1;
    state.setViolationCount(violations);

    List<Integer> escalation = appSecurityProperties.getRateLimit().getPenaltyEscalationMinutes();
    int maxLadderIndex = Math.max(0, (int) (escalation.size() - 1L));
    int ladderIndex = Math.clamp((int) (violations - 1L), 0, maxLadderIndex);
    int penaltyMinutes = escalation.get(ladderIndex);
    state.setPenaltyUntil(now.plus(penaltyMinutes, ChronoUnit.MINUTES));

    if (violations >= appSecurityProperties.getRateLimit().getPermanentAfterViolations()) {
      state.setPermanentlyBlocked(true);
    }
  }
}
