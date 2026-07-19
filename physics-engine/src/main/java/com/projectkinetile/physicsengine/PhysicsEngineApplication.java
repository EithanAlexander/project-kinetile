package com.projectkinetile.physicsengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.config.EdgeDeviceCatalogProperties;
import com.projectkinetile.physicsengine.config.HardwareProperties;
import com.projectkinetile.physicsengine.config.PhysicsProperties;
import com.projectkinetile.physicsengine.config.TileMonitoringProperties;

/**
 * Project Kinetile physics engine: consumes tile compression events from Kafka, evaluates the
 * piezoelectric feasibility model, persists outcomes to PostgreSQL, and exposes read-only analytics
 * via REST ({@code /api/v1}).
 */
@EnableScheduling
@EnableKafka
@EnableConfigurationProperties({
  PhysicsProperties.class,
  HardwareProperties.class,
  AppSecurityProperties.class,
  EdgeDeviceCatalogProperties.class,
  TileMonitoringProperties.class
})
@SpringBootApplication
public class PhysicsEngineApplication {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(PhysicsEngineApplication.class);
    application.addInitializers(new DatabaseConnectionRetryInitializer());
    application.run(args);
  }
}
