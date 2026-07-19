package com.projectkinetile.physicsengine.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Per-client REST rate-limit state */
@Entity
@Table(name = "api_rate_limit_clients")
public class ApiRateLimitClientEntity {

  @Id
  @Column(name = "client_ip", nullable = false, length = 45)
  private String clientIp;

  @Column(name = "window_start", nullable = false)
  private Instant windowStart;

  @Column(name = "request_count", nullable = false)
  private int requestCount;

  @Column(name = "violation_count", nullable = false)
  private int violationCount;

  @Column(name = "first_violation_at")
  private Instant firstViolationAt;

  @Column(name = "penalty_until")
  private Instant penaltyUntil;

  @Column(name = "permanently_blocked", nullable = false)
  private boolean permanentlyBlocked;

  @Column(name = "last_seen_at", nullable = false)
  private Instant lastSeenAt;

  protected ApiRateLimitClientEntity() {}

  public ApiRateLimitClientEntity(String clientIp, Instant now) {
    this.clientIp = clientIp;
    this.windowStart = now;
    this.requestCount = 0;
    this.violationCount = 0;
    this.lastSeenAt = now;
    this.permanentlyBlocked = false;
  }

  public String getClientIp() {
    return clientIp;
  }

  public Instant getWindowStart() {
    return windowStart;
  }

  public void setWindowStart(Instant windowStart) {
    this.windowStart = windowStart;
  }

  public int getRequestCount() {
    return requestCount;
  }

  public void setRequestCount(int requestCount) {
    this.requestCount = requestCount;
  }

  public int getViolationCount() {
    return violationCount;
  }

  public void setViolationCount(int violationCount) {
    this.violationCount = violationCount;
  }

  public Instant getFirstViolationAt() {
    return firstViolationAt;
  }

  public void setFirstViolationAt(Instant firstViolationAt) {
    this.firstViolationAt = firstViolationAt;
  }

  public Instant getPenaltyUntil() {
    return penaltyUntil;
  }

  public void setPenaltyUntil(Instant penaltyUntil) {
    this.penaltyUntil = penaltyUntil;
  }

  public boolean isPermanentlyBlocked() {
    return permanentlyBlocked;
  }

  public void setPermanentlyBlocked(boolean permanentlyBlocked) {
    this.permanentlyBlocked = permanentlyBlocked;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(Instant lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }
}
