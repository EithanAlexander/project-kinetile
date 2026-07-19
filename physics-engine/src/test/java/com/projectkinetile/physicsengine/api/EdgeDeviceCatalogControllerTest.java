package com.projectkinetile.physicsengine.api;

import java.util.List;
import java.util.Objects;

import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkinetile.physicsengine.service.EdgeDeviceCatalogService;

@WebMvcTest(EdgeDeviceCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Edge device catalog controller")
class EdgeDeviceCatalogControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private EdgeDeviceCatalogService edgeDeviceCatalogService;

  @Test
  @DisplayName("returns catalog from service")
  void getDevices_returnsCatalogFromService() throws Exception {
    List<EdgeDeviceDTO> catalog =
        new ObjectMapper()
            .readValue(
                Objects.requireNonNull(
                    getClass().getResourceAsStream("/data/edge-devices.json")),
                new TypeReference<>() {});
    when(edgeDeviceCatalogService.getCatalog()).thenReturn(catalog);

    mockMvc
        .perform(get(ApiPaths.DEVICES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(10))
        .andExpect(jsonPath("$[0].id").value("ble_beacon"))
        .andExpect(jsonPath("$[0].name").value("BLE Tracking Beacon"))
        .andExpect(jsonPath("$[0].dailyRequiredWh").value(0.01))
        .andExpect(jsonPath("$[9].id").value("wifi_ap"))
        .andExpect(jsonPath("$[9].dailyRequiredWh").value(180.0));
  }

  @Test
  @DisplayName("returns empty JSON array when catalog is empty")
  void getDevices_returnsEmptyJsonArrayWhenCatalogIsEmpty() throws Exception {
    when(edgeDeviceCatalogService.getCatalog()).thenReturn(List.of());

    mockMvc
        .perform(get(ApiPaths.DEVICES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("serializes device fields in response")
  void getDevices_serializesDeviceFields() throws Exception {
    when(edgeDeviceCatalogService.getCatalog())
        .thenReturn(List.of(new EdgeDeviceDTO("led_marker", "LED Pavement Marker", 6.0)));

    mockMvc
        .perform(get(ApiPaths.DEVICES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("led_marker"))
        .andExpect(jsonPath("$[0].name").value("LED Pavement Marker"))
        .andExpect(jsonPath("$[0].dailyRequiredWh").value(6.0));
  }

  @Test
  @DisplayName("preserves catalog order from service")
  void getDevices_preservesCatalogOrderFromService() throws Exception {
    when(edgeDeviceCatalogService.getCatalog())
        .thenReturn(
            List.of(
                new EdgeDeviceDTO("a", "Device A", 1.0),
                new EdgeDeviceDTO("b", "Device B", 2.0),
                new EdgeDeviceDTO("c", "Device C", 3.0)));

    mockMvc
        .perform(get(ApiPaths.DEVICES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].id").value("a"))
        .andExpect(jsonPath("$[1].id").value("b"))
        .andExpect(jsonPath("$[2].id").value("c"));
  }
}
