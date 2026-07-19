package com.projectkinetile.physicsengine.api.validation;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Bounds time-series query windows to prevent unbounded aggregation DoS.
 */
public final class TimeseriesBoundsValidator {

  private TimeseriesBoundsValidator() {}

  /**
   * Normalizes and validates an inclusive time range for daily aggregation endpoints.
   *
   * @param since optional lower bound
   * @param until optional upper bound
   * @param maxMonths maximum allowed span in calendar months
   * @return resolved inclusive bounds
   */
  public static TimeseriesBounds resolve(Instant since, Instant until, int maxMonths) {
    int months = Math.max(1, maxMonths);
    Instant untilBound = until != null ? until : Instant.now();
    long maxSpanDays = months * 31L;
    Instant sinceBound =
        since != null ? since : untilBound.minus(maxSpanDays, ChronoUnit.DAYS);

    if (sinceBound.isAfter(untilBound)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "time range is invalid");
    }

    if (Duration.between(sinceBound, untilBound).toDays() > maxSpanDays) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "time range exceeds maximum of " + months + " months allowed");
    }

    return new TimeseriesBounds(sinceBound, untilBound);
  }

  /** Inclusive lower and upper instants for repository queries. */
  public record TimeseriesBounds(Instant since, Instant until) {}
}
