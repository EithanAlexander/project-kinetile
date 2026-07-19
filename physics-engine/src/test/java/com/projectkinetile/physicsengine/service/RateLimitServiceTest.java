package com.projectkinetile.physicsengine.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.domain.ApiRateLimitClientEntity;
import com.projectkinetile.physicsengine.repository.ApiRateLimitClientRepository;
import com.projectkinetile.physicsengine.support.MutableClock;

@DataJpaTest
@ActiveProfiles("test")
@EnableConfigurationProperties(AppSecurityProperties.class)
@Import({RateLimitService.class, RateLimitClientRowEnsurer.class, RateLimitServiceTest.TestClockConfig.class})
@DisplayName("Rate limit service")
class RateLimitServiceTest {

  @Autowired private RateLimitService rateLimitService;
  @Autowired private ApiRateLimitClientRepository repository;
  @Autowired private AppSecurityProperties appSecurityProperties;
  @Autowired private MutableClock mutableClock;

  private static final Instant TEST_EPOCH = Instant.parse("2024-06-01T12:00:00Z");

  @BeforeEach
  void setUp() {
    mutableClock.setInstant(TEST_EPOCH);
    appSecurityProperties.getRateLimit().setRequestsPerMinute(2);
    appSecurityProperties.getRateLimit().setPenaltyEscalationMinutes(List.of(5, 15));
    appSecurityProperties.getRateLimit().setPermanentAfterViolations(3);
    appSecurityProperties.getRateLimit().setViolationWindowHours(24);
    appSecurityProperties.getRateLimit().setEntryTtlMinutes(120);
    repository.deleteAll();
  }

  @Test
  @DisplayName("allows requests up to the per-minute quota")
  void tryConsume_withinQuota_allowsRequests() {
    assertThat(rateLimitService.tryConsume("10.0.0.1")).isTrue();
    assertThat(rateLimitService.tryConsume("10.0.0.1")).isTrue();
  }

  @Test
  @DisplayName("resets request window after one minute when not penalized")
  void tryConsume_afterWindowResets_allowsAgain() {
    assertThat(rateLimitService.tryConsume("10.0.0.2")).isTrue();
    assertThat(rateLimitService.tryConsume("10.0.0.2")).isTrue();

    mutableClock.advanceSeconds(61);
    assertThat(rateLimitService.tryConsume("10.0.0.2")).isTrue();
  }

  @Test
  @DisplayName("applies escalating penalty that blocks into the next window")
  void tryConsume_quotaExceeded_appliesPenalty() {
    assertThat(rateLimitService.tryConsume("10.0.0.3")).isTrue();
    assertThat(rateLimitService.tryConsume("10.0.0.3")).isTrue();
    assertThat(rateLimitService.tryConsume("10.0.0.3")).isFalse();

    mutableClock.advanceMinutes(1);
    assertThat(rateLimitService.tryConsume("10.0.0.3")).isFalse();

    ApiRateLimitClientEntity state = repository.findById("10.0.0.3").orElseThrow();
    assertThat(state.getPenaltyUntil()).isAfter(mutableClock.instant());
  }

  @Test
  @DisplayName("permanently blocks after repeated violations in the rolling window")
  void tryConsume_repeatedViolations_permanentlyBlocks() {
    for (int cycle = 0; cycle < 3; cycle++) {
      rateLimitService.tryConsume("10.0.0.4");
      rateLimitService.tryConsume("10.0.0.4");
      rateLimitService.tryConsume("10.0.0.4");
      mutableClock.advanceMinutes(20);
    }

    ApiRateLimitClientEntity state = repository.findById("10.0.0.4").orElseThrow();
    assertThat(state.isPermanentlyBlocked()).isTrue();
    assertThat(rateLimitService.tryConsume("10.0.0.4")).isFalse();
  }

  @Test
  @DisplayName("purges idle benign client rows past TTL")
  void purgeIdleClients_removesExpiredBenignRows() {
    rateLimitService.tryConsume("10.0.0.5");
    mutableClock.advanceMinutes(180);
    int removed = rateLimitService.purgeIdleClients();
    assertThat(removed).isEqualTo(1);
    assertThat(repository.findById("10.0.0.5")).isEmpty();
  }

  @Test
  @DisplayName("concurrent first requests for one IP respect the per-minute quota")
  void tryConsume_concurrentFirstRequests_respectsQuota() throws Exception {
    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<Boolean>> results = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  return rateLimitService.tryConsume("10.0.0.99");
                }));
      }
      start.countDown();

      long allowed = 0;
      for (Future<Boolean> result : results) {
        if (result.get()) {
          allowed++;
        }
      }

      assertThat(allowed).isEqualTo(2);
      assertThat(repository.findById("10.0.0.99")).isPresent();
    } finally {
      pool.shutdownNow();
    }
  }

  @TestConfiguration
  static class TestClockConfig {
    @Bean
    @SuppressWarnings("unused") // Registered by Spring when building the test context
    MutableClock mutableClock() {
      return new MutableClock(TEST_EPOCH);
    }
  }
}
