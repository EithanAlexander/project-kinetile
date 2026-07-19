package com.projectkinetile.physicsengine.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import com.projectkinetile.physicsengine.api.TileDetailDTO;
import com.projectkinetile.physicsengine.config.TileMonitoringProperties;
import com.projectkinetile.physicsengine.domain.TileCompressionActivityEntity;
import com.projectkinetile.physicsengine.repository.ChokepointRepository;
import com.projectkinetile.physicsengine.repository.CityRepository;
import com.projectkinetile.physicsengine.repository.PlaceTypeRepository;
import com.projectkinetile.physicsengine.repository.TileCompressionActivityRepository;
import com.projectkinetile.physicsengine.repository.TileManufacturerRepository;
import com.projectkinetile.physicsengine.repository.TileRepository;
import com.projectkinetile.physicsengine.support.CatalogTestFixtures;
import com.projectkinetile.physicsengine.support.CatalogTestFixtures.CatalogSeed;

@DataJpaTest
@ActiveProfiles("test")
@EnableConfigurationProperties(TileMonitoringProperties.class)
@Import(InfrastructureCatalogService.class)
@DisplayName("Infrastructure catalog service")
class InfrastructureCatalogServiceTest {

  @Autowired private InfrastructureCatalogService catalogService;
  @Autowired private PlaceTypeRepository placeTypeRepository;
  @Autowired private TileManufacturerRepository manufacturerRepository;
  @Autowired private CityRepository cityRepository;
  @Autowired private ChokepointRepository chokepointRepository;
  @Autowired private TileRepository tileRepository;
  @Autowired private TileCompressionActivityRepository activityRepository;
  @Autowired private TileMonitoringProperties tileMonitoringProperties;

  private CatalogSeed seed;

  @BeforeEach
  void setUp() {
    seed =
        CatalogTestFixtures.seedCatalog(
            placeTypeRepository,
            manufacturerRepository,
            cityRepository,
            chokepointRepository,
            tileRepository);
    tileMonitoringProperties.setInactivityThresholdDays(5);
  }

  @Test
  @DisplayName("aggregates city chokepoint and active tile counts")
  void listCities_returnsAggregateCounts() {
    var cities = catalogService.listCities();
    assertThat(cities).hasSize(2);
    assertThat(cities.stream().filter(c -> c.name().equals("Tel Aviv-Yafo")).findFirst())
        .get()
        .satisfies(
            c -> {
              assertThat(c.chokepointCount()).isEqualTo(2);
              assertThat(c.activeTileCount()).isEqualTo(2);
            });
  }

  @Test
  @DisplayName("throws 404 when listing chokepoints for unknown city")
  void listChokepointsForCity_unknownCity_throws404() {
    assertThatThrownBy(() -> catalogService.listChokepointsForCity(999L))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  @DisplayName("includes compression activity on paginated tile rows")
  void listTilesForChokepoint_includesActivity() {
    UUID tileId = seed.tileA().getTileId();
    activityRepository.save(
        new TileCompressionActivityEntity(tileId, Instant.now(), Instant.now(), 3L));

    var page =
        catalogService.listTilesForChokepoint(
            seed.tileA().getChokepoint().getId(), PageRequest.of(0, 10));

    assertThat(page.getContent()).isNotEmpty();
    assertThat(page.getContent().get(0).totalCompressions()).isEqualTo(3L);
  }

  @Test
  @DisplayName("lists stale active tiles with no recent compression activity")
  void listStaleTiles_returnsInactiveTiles() {
    UUID staleId = seed.tileB().getTileId();
    activityRepository.save(
        new TileCompressionActivityEntity(
            staleId,
            Instant.now().minus(30, ChronoUnit.DAYS),
            Instant.now().minus(10, ChronoUnit.DAYS),
            1L));

    var stale = catalogService.listStaleTiles();
    assertThat(stale).extracting(TileDetailDTO::tileId).contains(staleId);
  }
}
