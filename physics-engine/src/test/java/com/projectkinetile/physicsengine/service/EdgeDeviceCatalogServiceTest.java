package com.projectkinetile.physicsengine.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkinetile.physicsengine.api.EdgeDeviceDTO;
import com.projectkinetile.physicsengine.config.EdgeDeviceCatalogProperties;

@DisplayName("Edge device catalog service")
class EdgeDeviceCatalogServiceTest {

  @Test
  @DisplayName("loads edge devices from classpath JSON")
  void loadsEdgeDevicesFromClasspathJson() {
    EdgeDeviceCatalogService service =
        new EdgeDeviceCatalogService(new ObjectMapper(), new EdgeDeviceCatalogProperties());
    List<EdgeDeviceDTO> catalog = service.getCatalog();
    assertThat(catalog).hasSize(10);
    assertThat(catalog.getFirst().id()).isEqualTo("ble_beacon");
    assertThat(catalog.getLast().id()).isEqualTo("wifi_ap");
    assertThat(catalog.getLast().dailyRequiredWh()).isEqualTo(180.0);
  }

  @Test
  @DisplayName("returns unmodifiable catalog snapshot")
  void getCatalog_returnsUnmodifiableList() {
    EdgeDeviceCatalogService service =
        new EdgeDeviceCatalogService(new ObjectMapper(), new EdgeDeviceCatalogProperties());

    assertThat(service.getCatalog()).isUnmodifiable();
  }
}
