package com.projectkinetile.physicsengine.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Mutable {@link Clock} for time-dependent unit tests. */
public final class MutableClock extends Clock {

  private Instant instant;
  private final ZoneOffset zone = ZoneOffset.UTC;

  public MutableClock(Instant initial) {
    this.instant = initial;
  }

  public void setInstant(Instant instant) {
    this.instant = instant;
  }

  public void advanceSeconds(long seconds) {
    instant = instant.plusSeconds(seconds);
  }

  public void advanceMinutes(long minutes) {
    instant = instant.plusSeconds(minutes * 60);
  }

  @Override
  public ZoneOffset getZone() {
    return zone;
  }

  @Override
  public Clock withZone(java.time.ZoneId zone) {
    return this;
  }

  @Override
  public Instant instant() {
    return instant;
  }
}
