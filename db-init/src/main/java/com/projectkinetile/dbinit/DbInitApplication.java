package com.projectkinetile.dbinit;

import com.projectkinetile.dbinit.bootstrap.CatalogBootstrapRunner;
import com.projectkinetile.dbinit.config.BootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * One-shot database initializer: Flyway migrations then idempotent catalog bootstrap.
 *
 * <p>Exits after completion; not a long-running service.
 */
@SpringBootApplication
@EnableConfigurationProperties(BootstrapProperties.class)
public class DbInitApplication implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DbInitApplication.class);

  private final CatalogBootstrapRunner catalogBootstrapRunner;

  public DbInitApplication(CatalogBootstrapRunner catalogBootstrapRunner) {
    this.catalogBootstrapRunner = catalogBootstrapRunner;
  }

  public static void main(String[] args) {
    int code = SpringApplication.exit(SpringApplication.run(DbInitApplication.class, args));
    System.exit(code);
  }

  @Override
  public void run(String... args) {
    log.info("Flyway migrations applied (if pending)");
    catalogBootstrapRunner.runBootstrap();
    log.info("db-init complete");
  }
}
