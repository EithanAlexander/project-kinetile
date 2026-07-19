package com.projectkinetile.physicsengine.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projectkinetile.physicsengine.config.TileMonitoringProperties;
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

@WebMvcTest(TileMonitoringConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@EnableConfigurationProperties(TileMonitoringProperties.class)
@DisplayName("Tile monitoring config controller")
class TileMonitoringConfigControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TileMonitoringProperties tileMonitoringProperties;

  @Test
  @DisplayName("returns tile monitoring configuration")
  void getTileMonitoringConfig_returnsThreshold() throws Exception {
    when(tileMonitoringProperties.getInactivityThresholdDays()).thenReturn(5);

    mockMvc
        .perform(get(ApiPaths.CONFIG_TILE_MONITORING))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inactivityThresholdDays").value(5));
  }

  @Test
  @DisplayName("responds with JSON content type")
  void getTileMonitoringConfig_respondsWithJsonContentType() throws Exception {
    when(tileMonitoringProperties.getInactivityThresholdDays()).thenReturn(5);

    mockMvc
        .perform(get(ApiPaths.CONFIG_TILE_MONITORING))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));
  }
}
