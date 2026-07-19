package com.projectkinetile.physicsengine.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.service.InfrastructureCatalogService;

@WebMvcTest(InfrastructureController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppSecurityProperties.class)
@DisplayName("Infrastructure controller")
class InfrastructureControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private InfrastructureCatalogService catalogService;

  @Test
  @DisplayName("lists place types from the catalog service")
  void listPlaceTypes_returnsOk() throws Exception {
    when(catalogService.listPlaceTypes())
        .thenReturn(List.of(new PlaceTypeDTO(1L, "MALL", "Mall", "HIGH")));

    mockMvc
        .perform(get(ApiPaths.INFRASTRUCTURE + "/place-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("MALL"));
  }

  @Test
  @DisplayName("returns 404 when chokepoints requested for unknown city")
  void listChokepoints_unknownCity_returns404() throws Exception {
    when(catalogService.listChokepointsForCity(99L))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found"));

    mockMvc
        .perform(get(ApiPaths.INFRASTRUCTURE + "/cities/99/chokepoints"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("clamps tile page size to the configured ledger maximum")
  void listTiles_clampsPageSize() throws Exception {
    when(catalogService.listTilesForChokepoint(
            org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
        .thenReturn(Page.<TileCatalogDTO>empty());

    mockMvc
        .perform(get(ApiPaths.INFRASTRUCTURE + "/chokepoints/1/tiles").param("size", "9999"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("returns 404 when tile UUID is not in the catalog")
  void getTile_unknownUuid_returns404() throws Exception {
    UUID missing = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    when(catalogService.getTile(missing))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tile not found"));

    mockMvc
        .perform(get(ApiPaths.INFRASTRUCTURE + "/tiles/" + missing))
        .andExpect(status().isNotFound());
  }
}
