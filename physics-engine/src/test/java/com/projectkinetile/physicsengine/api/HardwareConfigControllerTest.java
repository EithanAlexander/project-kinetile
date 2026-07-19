package com.projectkinetile.physicsengine.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projectkinetile.physicsengine.config.HardwareProperties;
import com.projectkinetile.physicsengine.config.PhysicsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer (MockMvc) tests for {@link HardwareConfigController}.
 *
 * <p>These tests isolate the controller via {@link WebMvcTest} and stub the configuration property
 * beans with Mockito so the assertions verify the HTTP contract (status, content type, and JSON
 * field mapping) rather than the actual {@code application.yml} values. Keeping the source values
 * mocked guarantees the controller faithfully projects {@link HardwareProperties} and {@link
 * PhysicsProperties} onto {@link HardwareConfigDTO} without hardcoded defaults leaking through.
 */
@WebMvcTest(HardwareConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@EnableConfigurationProperties({HardwareProperties.class, PhysicsProperties.class})
@DisplayName("Hardware config controller")
class HardwareConfigControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HardwareProperties hardwareProperties;
  @MockitoBean private PhysicsProperties physicsProperties;

  /**
   * Verifies the happy path: every configured hardware/physics value is serialized onto the
   * corresponding JSON field with a {@code 200 OK} status.
   */
  @Test
  @DisplayName("returns hardware tile configuration")
  void getHardwareConfig_returnsThresholdAndRatedOutput() throws Exception {
    when(hardwareProperties.getActivationThresholdNewtons()).thenReturn(100.0);
    when(hardwareProperties.getMinRatedOutputJoules()).thenReturn(2.0);
    when(hardwareProperties.getMaxRatedOutputJoules()).thenReturn(5.0);
    when(hardwareProperties.getMaxScaleMassKg()).thenReturn(90.0);
    when(hardwareProperties.getMaxDisplacementMeters()).thenReturn(0.0001);
    when(physicsProperties.getGravity()).thenReturn(9.81);

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

  /**
   * Verifies the endpoint advertises a JSON response so dashboard clients can safely deserialize the
   * payload regardless of the underlying property values.
   */
  @Test
  @DisplayName("responds with JSON content type")
  void getHardwareConfig_respondsWithJsonContentType() throws Exception {
    when(hardwareProperties.getActivationThresholdNewtons()).thenReturn(100.0);
    when(hardwareProperties.getMinRatedOutputJoules()).thenReturn(2.0);
    when(hardwareProperties.getMaxRatedOutputJoules()).thenReturn(5.0);
    when(hardwareProperties.getMaxScaleMassKg()).thenReturn(90.0);
    when(hardwareProperties.getMaxDisplacementMeters()).thenReturn(0.0001);
    when(physicsProperties.getGravity()).thenReturn(9.81);

    mockMvc
        .perform(get(ApiPaths.CONFIG_HARDWARE))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));
  }
}
