package com.projectkinetile.physicsengine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Ensures the app exits when Postgres is unreachable, with retries/backoff.
 *
 * <p>This runs before Spring fully refreshes the context, so Hibernate schema updates
 * won't fail immediately without giving Postgres a chance to come up.
 */
public final class DatabaseConnectionRetryInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionRetryInitializer.class);

  private static final String URL_PROPERTY = "spring.datasource.url";
  private static final String USERNAME_PROPERTY = "spring.datasource.username";
  private static final String PASSWORD_PROPERTY = "spring.datasource.password";

  // Optional knobs to avoid recompiling when adjusting retry behavior.
  private static final String MAX_ATTEMPTS_PROPERTY = "app.db.connect.retry.max-attempts";
  private static final String BACKOFF_MS_PROPERTY = "app.db.connect.retry.backoff-ms";

  @Override
  public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
    Environment env = applicationContext.getEnvironment();

    DbConnectConfig config = DbConnectConfig.from(env);

    DataSource dataSource = createDataSource(config.url(), config.username(), config.password());
    RetryTemplate retryTemplate =
        createRetryTemplate(config.maxAttempts(), config.backoffMs());

    try {
      retryTemplate.execute(context -> {
        int attempt = context.getRetryCount() + 1;
        log.info("Checking Postgres connectivity (attempt {}/{})...", attempt,
            config.maxAttempts());

        testConnection(dataSource);
        log.info("Postgres connectivity check succeeded.");
        return null;
      });
    } catch (RuntimeException e) {
      throw new IllegalStateException(
          "Failed to connect to Postgres after " + config.maxAttempts() + " attempts. Exiting.", e);
    }
  }

  private static DataSource createDataSource(String url, String username, String password) {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    return dataSource;
  }

  private static void testConnection(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
      statement.execute();
    } catch (Exception e) {
      // Wrap checked exceptions so RetryTemplate can treat it as a retryable failure.
      throw new IllegalStateException("Postgres connectivity check failed.", e);
    }
  }

  private static RetryTemplate createRetryTemplate(int maxAttempts, long backoffMs) {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts);

    FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(backoffMs);

    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    return retryTemplate;
  }

  /**
   * Small immutable config snapshot sourced from Spring environment.
   *
   * <p>We keep this separate so the initializer remains readable.
   */
  private record DbConnectConfig(
      String url,
      String username,
      String password,
      int maxAttempts,
      long backoffMs) {

    static DbConnectConfig from(Environment env) {
      String url = env.getRequiredProperty(URL_PROPERTY);
      if (url.isBlank()) {
        throw new IllegalStateException("Missing required configuration property: " + URL_PROPERTY);
      }

      String username = env.getRequiredProperty(USERNAME_PROPERTY);
      if (username.isBlank()) {
        throw new IllegalStateException(
            "Missing required configuration property: " + USERNAME_PROPERTY);
      }

      String password = env.getRequiredProperty(PASSWORD_PROPERTY);
      if (password.isBlank()) {
        throw new IllegalStateException(
            "Missing required configuration property: " + PASSWORD_PROPERTY);
      }

      int maxAttempts = env.getProperty(MAX_ATTEMPTS_PROPERTY, Integer.class, 6);
      if (maxAttempts < 1) {
        throw new IllegalStateException(
            "Invalid retry configuration: " + MAX_ATTEMPTS_PROPERTY + " must be >= 1.");
      }

      long backoffMs = env.getProperty(BACKOFF_MS_PROPERTY, Long.class, 2000L);
      if (backoffMs < 0) {
        throw new IllegalStateException(
            "Invalid retry configuration: " + BACKOFF_MS_PROPERTY + " must be >= 0.");
      }

      return new DbConnectConfig(url, username, password, maxAttempts, backoffMs);
    }
  }

}

