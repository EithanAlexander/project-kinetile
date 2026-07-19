package com.projectkinetile.physicsengine.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Injectable UTC {@link Clock} so {@code RateLimitService} time logic is testable
 * with {@link com.projectkinetile.physicsengine.support.MutableClock} */
@Configuration
public class ClockConfig {

  @Bean
  public Clock systemClock() {
    return Clock.systemUTC();
  }
}
