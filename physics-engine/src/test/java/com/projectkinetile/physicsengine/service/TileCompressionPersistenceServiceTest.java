package com.projectkinetile.physicsengine.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import com.projectkinetile.physicsengine.domain.TileCompressionActivityEntity;
import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.domain.TileCompressionEventType;
import com.projectkinetile.physicsengine.domain.TileEntity;
import com.projectkinetile.physicsengine.physics.ActivationResult;
import com.projectkinetile.physicsengine.physics.TileCompressionEvent;
import com.projectkinetile.physicsengine.repository.ChokepointRepository;
import com.projectkinetile.physicsengine.repository.CityRepository;
import com.projectkinetile.physicsengine.repository.PlaceTypeRepository;
import com.projectkinetile.physicsengine.repository.TileCompressionActivityRepository;
import com.projectkinetile.physicsengine.repository.TileCompressionEventRepository;
import com.projectkinetile.physicsengine.repository.TileManufacturerRepository;
import com.projectkinetile.physicsengine.repository.TileRepository;
import com.projectkinetile.physicsengine.support.CatalogTestFixtures;
import com.projectkinetile.physicsengine.support.CatalogTestFixtures.CatalogSeed;

@DataJpaTest
@Import({TileCompressionPersistenceService.class, TileCompressionActivityIncrementer.class})
@ActiveProfiles("test")
@DisplayName("Tile compression persistence service")
class TileCompressionPersistenceServiceTest {

  private static @NonNull UUID tileId() {
    return Objects.requireNonNull(UUID.fromString("11111111-1111-4111-8111-111111111111"));
  }

  @Autowired private TileCompressionPersistenceService persistenceService;
  @Autowired private TileCompressionEventRepository eventRepository;
  @Autowired private TileCompressionActivityRepository activityRepository;
  @Autowired private PlaceTypeRepository placeTypeRepository;
  @Autowired private TileManufacturerRepository manufacturerRepository;
  @Autowired private CityRepository cityRepository;
  @Autowired private ChokepointRepository chokepointRepository;
  @Autowired private TileRepository tileRepository;

  private TileEntity tile;

  @BeforeEach
  void seedCatalog() {
    CatalogSeed catalog =
        CatalogTestFixtures.seedCatalog(
            placeTypeRepository,
            manufacturerRepository,
            cityRepository,
            chokepointRepository,
            tileRepository);
    tile = catalog.tileA();
  }

  @Test
  @DisplayName("persist saves evaluated physics and increments activity on first insert")
  void persist_firstInsert_savesEntityAndIncrementsActivity() {
    UUID tileId = tileId();
    Instant ts = utcInstantOn(tile.getInstallationDate().plusDays(30));
    TileCompressionEvent event = compressionEvent("evt-first", tileId, ts);
    ActivationResult result = new ActivationResult(784.8, 4.0, true);

    TileCompressionEventEntity saved = persistenceService.persist(event, tile, result);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getEventId()).isEqualTo("evt-first");
    assertThat(saved.getCalculatedEnergyJoules()).isEqualTo(4.0);
    assertThat(saved.getCalculatedForceNewtons()).isEqualTo(784.8);
    assertThat(saved.isActivationSuccessful()).isTrue();
    assertThat(saved.getEventTimestamp()).isEqualTo(ts);

    TileCompressionActivityEntity activity = activityRepository.findById(tileId).orElseThrow();
    assertThat(activity.getTotalCompressions()).isEqualTo(1L);
    assertThat(activity.getFirstCompressionAt()).isEqualTo(ts);
    assertThat(activity.getLastCompressionAt()).isEqualTo(ts);
  }

  @Test
  @DisplayName("duplicate event_id is idempotent and does not double-count activity")
  void persist_duplicateEventId_isIdempotent() {
    UUID tileId = tileId();
    Instant ts = utcInstantOn(tile.getInstallationDate().plusDays(30));
    TileCompressionEvent event = compressionEvent("evt-dup-1", tileId, ts);
    ActivationResult result = new ActivationResult(784.8, 4.0, true);

    persistenceService.persist(event, tile, result);
    persistenceService.persist(event, tile, result);

    assertThat(eventRepository.findByEventId("evt-dup-1")).isPresent();
    assertThat(eventRepository.count()).isEqualTo(1);

    TileCompressionActivityEntity activity = activityRepository.findById(tileId).orElseThrow();
    assertThat(activity.getTotalCompressions()).isEqualTo(1L);
  }

  @Test
  @DisplayName("resolveActiveTile returns empty for unknown tile UUID")
  void resolveActiveTile_unknownTile_returnsEmpty() {
    TileCompressionEvent event =
        compressionEvent(
            "evt-unknown",
            UUID.fromString("99999999-9999-4999-8999-999999999999"),
            utcInstantOn(tile.getInstallationDate().plusDays(1)));

    assertThat(persistenceService.resolveActiveTile(event)).isEmpty();
  }

  @Test
  @DisplayName("resolveActiveTile returns empty when tile is inactive")
  void resolveActiveTile_inactiveTile_returnsEmpty() {
    tile.setActive(false);
    tileRepository.save(tile);

    TileCompressionEvent event =
        compressionEvent("evt-inactive", tileId(), utcInstantOn(tile.getInstallationDate().plusDays(1)));

    assertThat(persistenceService.resolveActiveTile(event)).isEmpty();
  }

  @Test
  @DisplayName("resolveActiveTile returns empty when event predates installation")
  void resolveActiveTile_beforeInstallation_returnsEmpty() {
    Instant beforeInstall = utcInstantOn(tile.getInstallationDate().minusDays(1));
    TileCompressionEvent event = compressionEvent("evt-early", tileId(), beforeInstall);

    assertThat(persistenceService.resolveActiveTile(event)).isEmpty();
  }

  @Test
  @DisplayName("resolveActiveTile returns empty when event is after removal date")
  void resolveActiveTile_afterRemoval_returnsEmpty() {
    LocalDate removal = tile.getInstallationDate().plusDays(10);
    tile.setRemovalDate(removal);
    tileRepository.save(tile);

    Instant afterRemoval = utcInstantOn(removal.plusDays(1));
    TileCompressionEvent event = compressionEvent("evt-late", tileId(), afterRemoval);

    assertThat(persistenceService.resolveActiveTile(event)).isEmpty();
  }

  @Test
  @DisplayName("resolveActiveTile returns tile when active and within install/removal window")
  void resolveActiveTile_withinLifecycle_returnsTile() {
    Instant inWindow = utcInstantOn(tile.getInstallationDate().plusDays(5));
    TileCompressionEvent event = compressionEvent("evt-ok", tileId(), inWindow);

    assertThat(persistenceService.resolveActiveTile(event))
        .hasValueSatisfying(resolved -> assertThat(resolved.getTileId()).isEqualTo(tileId()));
  }

  private static TileCompressionEvent compressionEvent(String eventId, UUID id, Instant ts) {
    return new TileCompressionEvent(
        eventId, TileCompressionEventType.TILE_COMPRESSION, id, 80.0, 1.0, ts);
  }

  private static Instant utcInstantOn(LocalDate date) {
    return date.atStartOfDay(ZoneOffset.UTC).toInstant();
  }
}
