package com.projectkinetile.physicsengine.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.domain.ChokepointEntity;
import com.projectkinetile.physicsengine.domain.CityEntity;
import com.projectkinetile.physicsengine.domain.PlaceTypeEntity;
import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.domain.TileCompressionEventType;
import com.projectkinetile.physicsengine.domain.TileEntity;
import com.projectkinetile.physicsengine.domain.TileManufacturerEntity;
import com.projectkinetile.physicsengine.repository.DailyCompressionBucket;
import com.projectkinetile.physicsengine.repository.DailyCompressionCityBucket;
import com.projectkinetile.physicsengine.repository.TileCompressionEventRepository;
import com.projectkinetile.physicsengine.service.TileCompressionLedgerQueryService;

@WebMvcTest(CompressionAnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppSecurityProperties.class)
@DisplayName("Compression analytics controller")
class CompressionAnalyticsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TileCompressionEventRepository repository;
  @MockitoBean private TileCompressionLedgerQueryService ledgerQueryService;

  @Test
  @DisplayName("ping returns plain text message")
  void ping_returnsPlainTextMessage() throws Exception {
    mockMvc
        .perform(get(ApiPaths.ENERGY + "/ping"))
        .andExpect(status().isOk())
        .andExpect(content().string("Project Kinetile API is live!"));
  }

  @Test
  @DisplayName("summary returns network totals")
  void summary_returnsNetworkTotals() throws Exception {
    Map<String, Object> row = new HashMap<>();
    row.put("totalJoules", 10_000.0);
    row.put("totalCompressions", 100L);
    row.put("successfulActivations", 80L);
    when(repository.aggregateNetworkSummary()).thenReturn(row);

    mockMvc
        .perform(get(ApiPaths.ENERGY + "/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCompressions").value(100))
        .andExpect(jsonPath("$.successfulActivations").value(80))
        .andExpect(jsonPath("$.totalJoules").value(10_000.0))
        .andExpect(jsonPath("$.totalWattHours").value(2.778));
  }

  @Test
  @DisplayName("locations returns compression metrics per site")
  void locations_returnsMetricsPerSite() throws Exception {
    Map<String, Object> row = new HashMap<>();
    row.put("city", "Haifa");
    row.put("location", "Grand Canyon Mall Entrance");
    row.put("totalJoules", 3600.0);
    row.put("totalCompressions", 10L);
    row.put("successfulActivations", 8L);
    when(repository.aggregateByLocation()).thenReturn(List.of(row));

    mockMvc
        .perform(get(ApiPaths.ENERGY + "/locations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].city").value("Haifa"))
        .andExpect(jsonPath("$[0].totalCompressions").value(10))
        .andExpect(jsonPath("$[0].successfulActivations").value(8))
        .andExpect(jsonPath("$[0].totalWattHours").value(1.0));
  }

  @Test
  @DisplayName("ledger returns paged compression rows")
  @SuppressWarnings("null")
  void ledger_returnsPagedRows() throws Exception {
    Instant ts = Instant.parse("2024-01-01T00:00:00Z");
    UUID tileId = UUID.fromString("11111111-1111-4111-8111-111111111111");
    CityEntity city = new CityEntity("HFA", "Haifa");
    city.setId(1L);
    PlaceTypeEntity placeType = new PlaceTypeEntity("MALL", "Mall", "HIGH");
    ChokepointEntity chokepoint = new ChokepointEntity(city, placeType, "GCM", "Site");
    TileManufacturerEntity manufacturer = new TileManufacturerEntity("Aslan");
    TileEntity tile =
        new TileEntity(tileId, chokepoint, manufacturer, "600x600", "Slate Gray", LocalDate.now(), LocalDate.now(), true);
    TileCompressionEventEntity entity =
        new TileCompressionEventEntity(
            "evt-1",
            TileCompressionEventType.TILE_COMPRESSION,
            tile,
            80.0,
            1.0,
            784.8,
            5.0,
            true,
            ts,
            ts,
            ts);
    entity.setId(1L);
    when(ledgerQueryService.findLedgerPage(
            ArgumentMatchers.<Specification<TileCompressionEventEntity>>any(),
            ArgumentMatchers.any()))
        .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 50), 1));

    mockMvc
        .perform(get(ApiPaths.ENERGY + "/ledger"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].eventId").value("evt-1"))
        .andExpect(jsonPath("$.content[0].tileId").value(tileId.toString()))
        .andExpect(jsonPath("$.content[0].city").value("Haifa"))
        .andExpect(jsonPath("$.content[0].manufacturerName").value("Aslan"))
        .andExpect(jsonPath("$.content[0].activationSuccessful").value(true))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("daily total timeseries returns bucket rows")
  void dailyTotal_returnsBuckets() throws Exception {
    DailyCompressionBucket row =
        new DailyCompressionBucket(LocalDate.parse("2024-06-01"), 3600.0, 10L, 8L);
    when(repository.sumJoulesGroupedByDay(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(java.util.Collections.singletonList(row));

    mockMvc
        .perform(get(ApiPaths.ENERGY + "/timeseries/daily/total"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].totalJoules").value(3600.0))
        .andExpect(jsonPath("$[0].totalCompressions").value(10));
  }

  @Test
  @DisplayName("daily by-city timeseries returns city buckets")
  void dailyByCity_returnsCityBuckets() throws Exception {
    DailyCompressionCityBucket row =
        new DailyCompressionCityBucket(LocalDate.parse("2024-06-01"), "Haifa", 1800.0, 5L, 4L);
    when(repository.sumJoulesGroupedByDayAndCity(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(java.util.Collections.singletonList(row));

    mockMvc
        .perform(get(ApiPaths.ENERGY + "/timeseries/daily/by-city"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].city").value("Haifa"));
  }

  @Test
  @DisplayName("daily by-location requires city parameter")
  void dailyByLocation_missingCity_returns400() throws Exception {
    mockMvc
        .perform(get(ApiPaths.ENERGY + "/timeseries/daily/by-location"))
        .andExpect(status().isBadRequest());
  }
}
