package com.projectkinetile.physicsengine.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the real {@link ApiSecurityConfig} filter chain end-to-end and pins the intended
 * security posture of the open, read-only analytics API.
 *
 * <p>Unlike the controller slice tests (which run with {@code addFilters = false}), this boots the
 * full application context with Spring Security active, so it is the single place that verifies the
 * authorization rules and response-hardening headers actually take effect. It guards four distinct
 * decisions in {@link ApiSecurityConfig}:
 *
 * <ul>
 *   <li>the {@code /api/v1/**} surface is reachable without any credentials (no authentication);
 *   <li>the {@code /actuator/health} probe is publicly reachable;
 *   <li>every other route is rejected by the catch-all {@code denyAll};
 *   <li>security-hardening headers are written on responses.
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityConfigIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("v1 API is reachable without any credentials (open read-only API)")
  void v1Api_noCredentials_returnsOk() throws Exception {
    mockMvc.perform(get("/api/v1/energy/ping")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("actuator health probe is publicly reachable")
  void actuatorHealth_isPubliclyReachable() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("actuator info endpoint is publicly reachable")
  void actuatorInfo_isPubliclyReachable() throws Exception {
    mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("OpenAPI documentation endpoints are publicly reachable")
  void swaggerEndpoints_arePubliclyReachable() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("routes outside the v1 API and health probe are denied by the catch-all")
  void unmappedRoute_isDeniedByCatchAll() throws Exception {
    mockMvc.perform(get("/internal/metrics")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("responses carry the configured security-hardening headers")
  void responses_includeHardeningHeaders() throws Exception {
    mockMvc
        .perform(get("/api/v1/energy/ping"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
  }
}
