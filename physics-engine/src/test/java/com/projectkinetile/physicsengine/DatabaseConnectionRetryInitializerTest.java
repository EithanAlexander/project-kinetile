package com.projectkinetile.physicsengine;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("Database connection retry initializer")
class DatabaseConnectionRetryInitializerTest {

  @Test
  @DisplayName("rejects blank datasource password in environment")
  void initialize_blankPassword_throws() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/test");
    env.setProperty("spring.datasource.username", "user");
    env.setProperty("spring.datasource.password", "   ");

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.setEnvironment(env);
    DatabaseConnectionRetryInitializer initializer = new DatabaseConnectionRetryInitializer();

    assertThatThrownBy(() -> initializer.initialize(context))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("spring.datasource.password");
  }

  @Test
  @DisplayName("rejects invalid max-attempts configuration")
  void initialize_invalidMaxAttempts_throws() {
    MockEnvironment env = baseEnv();
    env.setProperty("app.db.connect.retry.max-attempts", "0");

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.setEnvironment(env);
    DatabaseConnectionRetryInitializer initializer = new DatabaseConnectionRetryInitializer();

    assertThatThrownBy(() -> initializer.initialize(context))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("max-attempts");
  }

  @Test
  @DisplayName("fails fast when Postgres is unreachable after configured retries")
  void initialize_unreachableDatabase_throwsAfterRetries() {
    MockEnvironment env = baseEnv();
    env.setProperty("app.db.connect.retry.max-attempts", "1");
    env.setProperty("app.db.connect.retry.backoff-ms", "0");

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.setEnvironment(env);
    DatabaseConnectionRetryInitializer initializer = new DatabaseConnectionRetryInitializer();

    assertThatThrownBy(() -> initializer.initialize(context))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to connect to Postgres");
  }

  private static MockEnvironment baseEnv() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:1/none");
    env.setProperty("spring.datasource.username", "user");
    env.setProperty("spring.datasource.password", "secret");
    return env;
  }
}
