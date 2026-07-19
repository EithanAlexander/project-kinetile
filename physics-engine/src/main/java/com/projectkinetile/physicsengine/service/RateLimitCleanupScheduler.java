package com.projectkinetile.physicsengine.service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically purges expired benign rate-limit rows from PostgreSQL. */
@Component
public class RateLimitCleanupScheduler {

  private static final Logger log = LoggerFactory.getLogger(RateLimitCleanupScheduler.class);

  private final RateLimitService rateLimitService;

  public RateLimitCleanupScheduler(RateLimitService rateLimitService) {
    this.rateLimitService = rateLimitService;
  }

  /**
   * Removes idle client rows past the configured TTL.
   *
   * <p>Interval: {@code app.rate-limit.cleanup-interval-minutes} (default 30).
   */
  @Scheduled(
      fixedRateString = "${app.rate-limit.cleanup-interval-minutes:30}",
      timeUnit = TimeUnit.MINUTES)
  public void purgeIdleClients() {
    int removed = rateLimitService.purgeIdleClients();
    if (removed > 0) {
      log.info("Purged {} idle rate-limit client row(s)", removed);
    }
  }
}
