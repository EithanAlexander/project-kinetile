package com.projectkinetile.physicsengine.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projectkinetile.physicsengine.physics.ActivationResult;
import com.projectkinetile.physicsengine.physics.PiezoelectricCalculator;

/**
 * End-to-end configuration-binding tests for the {@code /api/v1/config/hardware} endpoint.
 *
 * <p>Unlike {@link HardwareConfigControllerTest}, these tests do <em>not</em> mock the property
 * beans. They boot the real Spring context so that values flow through the full chain that ships
 * to production: YAML keys ({@code hardware.tile.*}, {@code app.physics.gravity}) &rarr;
 * {@code @ConfigurationProperties} binding ({@link
 * com.projectkinetile.physicsengine.config.HardwareProperties} / {@link
 * com.projectkinetile.physicsengine.config.PhysicsProperties}) &rarr; {@link HardwareConfigDTO}
 * &rarr; JSON response (and downstream beans such as {@link PiezoelectricCalculator}).
 *
 * <p>Nested classes use separate contexts so each scenario can assert a distinct binding source
 * without property leakage between tests. The {@code test} profile isolates persistence onto
 * in-memory H2 and disables security/rate-limiting.
 */
@DisplayName("Hardware config binding")
class HardwareConfigBindingTest {

  private static void configureInMemoryDatabase(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () -> "jdbc:h2:mem:hardware_config_binding;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
    registry.add("spring.datasource.username", () -> "sa");
    registry.add("spring.datasource.password", () -> "");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
  }

  @Nested
  @SpringBootTest(
      properties = {
        "hardware.tile.activation-threshold-newtons=250.5",
        "hardware.tile.min-rated-output-joules=1.5",
        "hardware.tile.max-rated-output-joules=7.25",
        "hardware.tile.max-scale-mass-kg=85",
        "hardware.tile.max-displacement-meters=0.00025",
        "app.physics.gravity=9.78"
      })
  @AutoConfigureMockMvc(addFilters = false)
  @ActiveProfiles("test")
  @DisplayName("explicit property overrides")
  class ExplicitPropertyOverrides {

    @DynamicPropertySource
    static void inMemoryDatabase(DynamicPropertyRegistry registry) {
      configureInMemoryDatabase(registry);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private PiezoelectricCalculator calculator;

    @Test
    @DisplayName("binds hardware.tile and app.physics properties into the response")
    void getHardwareConfig_bindsConfiguredPropertyValues() throws Exception {
      mockMvc
          .perform(get(ApiPaths.CONFIG_HARDWARE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.activationThresholdNewtons").value(250.5))
          .andExpect(jsonPath("$.minRatedOutputJoules").value(1.5))
          .andExpect(jsonPath("$.maxRatedOutputJoules").value(7.25))
          .andExpect(jsonPath("$.maxScaleMassKg").value(85))
          .andExpect(jsonPath("$.maxDisplacementMeters").value(0.00025))
          .andExpect(jsonPath("$.gravity").value(9.78));
    }

    @Test
    @DisplayName("bound activation threshold drives PiezoelectricCalculator gate")
    void piezoelectricCalculator_usesBoundActivationThreshold() {
      ActivationResult belowThreshold = calculator.evaluate(20.0, 1.0);
      ActivationResult aboveThreshold = calculator.evaluate(30.0, 1.0);

      assertThat(belowThreshold.activationSuccessful()).isFalse();
      assertThat(aboveThreshold.activationSuccessful()).isTrue();
    }
  }

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc(addFilters = false)
  @ActiveProfiles("test")
  @DisplayName("test profile YAML defaults")
  class TestProfileYamlDefaults {

    @DynamicPropertySource
    static void inMemoryDatabase(DynamicPropertyRegistry registry) {
      configureInMemoryDatabase(registry);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private PiezoelectricCalculator calculator;

    @Test
    @DisplayName("exposes values from application-test.yml")
    void getHardwareConfig_exposesApplicationTestYamlDefaults() throws Exception {
      mockMvc
          .perform(get(ApiPaths.CONFIG_HARDWARE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.activationThresholdNewtons").value(100.0))
          .andExpect(jsonPath("$.minRatedOutputJoules").value(2.0))
          .andExpect(jsonPath("$.maxRatedOutputJoules").value(5.0))
          .andExpect(jsonPath("$.maxScaleMassKg").value(90.0))
          .andExpect(jsonPath("$.maxDisplacementMeters").value(0.0001))
          .andExpect(jsonPath("$.gravity").value(9.81));
    }

    @Test
    @DisplayName("default YAML values drive calculator floor J at threshold mass")
    void piezoelectricCalculator_usesDefaultYamlConstants() {
      ActivationResult atThreshold = calculator.evaluate(10.2, 1.0);

      assertThat(atThreshold.forceNewtons()).isGreaterThanOrEqualTo(100.0);
      assertThat(atThreshold.activationSuccessful()).isTrue();
      assertThat(atThreshold.energyJoules()).isCloseTo(2.0, within(0.05));
    }
  }

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc(addFilters = false)
  @ActiveProfiles("test")
  @DisplayName("relaxed binding aliases")
  class RelaxedBindingAliases {

    @DynamicPropertySource
    static void camelCaseOverrides(DynamicPropertyRegistry registry) {
      configureInMemoryDatabase(registry);
      registry.add("hardware.tile.maxRatedOutputJoules", () -> "6.5");
    }

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("binds camelCase hardware.tile keys into the response")
    void getHardwareConfig_bindsCamelCaseMaxRatedOutput() throws Exception {
      mockMvc
          .perform(get(ApiPaths.CONFIG_HARDWARE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.maxRatedOutputJoules").value(6.5))
          .andExpect(jsonPath("$.minRatedOutputJoules").value(2.0))
          .andExpect(jsonPath("$.activationThresholdNewtons").value(100.0));
    }
  }
}
