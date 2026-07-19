package com.projectkinetile.physicsengine.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.support.CatalogTestFixtures;
import com.projectkinetile.physicsengine.support.CatalogTestFixtures.CatalogSeed;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tile compression event repository")
class TileCompressionEventRepositoryTest {

  @Autowired private TileCompressionEventRepository repository;
  @Autowired private PlaceTypeRepository placeTypeRepository;
  @Autowired private TileManufacturerRepository manufacturerRepository;
  @Autowired private CityRepository cityRepository;
  @Autowired private ChokepointRepository chokepointRepository;
  @Autowired private TileRepository tileRepository;

  private static final Instant T0 = Instant.parse("2024-06-01T12:00:00Z");
  private static final Instant T1 = Instant.parse("2024-06-02T12:00:00Z");
  private static final Instant AUDIT = Instant.parse("2024-06-01T12:05:00Z");

  private CatalogSeed catalog;

  @BeforeEach
  void resetAndSeed() {
    repository.deleteAll();
    tileRepository.deleteAll();
    chokepointRepository.deleteAll();
    cityRepository.deleteAll();
    manufacturerRepository.deleteAll();
    placeTypeRepository.deleteAll();
    catalog =
        CatalogTestFixtures.seedCatalog(
            placeTypeRepository,
            manufacturerRepository,
            cityRepository,
            chokepointRepository,
            tileRepository);
    repository.save(
        CatalogTestFixtures.compressionEvent(
            "evt-a", catalog.tileA(), 80.0, 1.0, 784.8, 5.0, true, T0, AUDIT));
    repository.save(
        CatalogTestFixtures.compressionEvent(
            "evt-b", catalog.tileB(), 8.0, 1.0, 78.48, 0.0, false, T1, AUDIT));
    repository.save(
        CatalogTestFixtures.compressionEvent(
            "evt-c", catalog.tileC(), 70.0, 1.2, 823.0, 5.0, true, T1, AUDIT));
    repository.flush();
  }

  @Test
  @DisplayName("aggregateByLocation sums compressions and activations")
  void aggregateByLocation_sumsCompressionsAndActivations() {
    List<Map<String, Object>> rows = repository.aggregateByLocation();
    assertThat(rows).hasSize(3);
    Map<String, Object> haifa =
        rows.stream().filter(r -> "Haifa".equals(r.get("city"))).findFirst().orElseThrow();
    assertThat(haifa)
        .containsEntry("totalCompressions", 1L)
        .containsEntry("successfulActivations", 0L);
  }

  @Test
  @DisplayName("aggregateNetworkSummary returns totals")
  void aggregateNetworkSummary_returnsTotals() {
    Map<String, Object> summary = repository.aggregateNetworkSummary();
    assertThat(summary)
        .containsEntry("totalCompressions", 3L)
        .containsEntry("successfulActivations", 2L);
    assertThat(((Number) summary.get("totalJoules")).doubleValue()).isEqualTo(10.0);
  }

  @Test
  @DisplayName("specification filters activationOnly")
  void specification_activationOnly() {
    Specification<TileCompressionEventEntity> spec =
        TileCompressionEventSpecifications.withOptionalFilters(
            new TileCompressionEventLedgerFilterCriteria(
                null, null, null, null, null, null, null, true, null, null));
    var page = repository.findAll(spec, PageRequest.of(0, 10, Sort.by("eventTimestamp").descending()));
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  @DisplayName("findAll sorts by joined chokepoint name")
  void findAll_sortsByLocationAsc() {
    var page =
        repository.findAll(
            allRowsSpec(),
            PageRequest.of(0, 10, Sort.by("tile.chokepoint.name").ascending()));
    assertThat(page.getContent())
        .extracting(e -> e.getTile().getChokepoint().getName())
        .containsExactly("Site A", "Site B", "Site C");
  }

  @Test
  @DisplayName("findAll sorts by event id")
  void findAll_sortsByEventIdAsc() {
    var page =
        repository.findAll(
            allRowsSpec(),
            PageRequest.of(0, 10, Sort.by("eventId").ascending()));
    assertThat(page.getContent())
        .extracting(e -> e.getEventId())
        .containsExactly("evt-a", "evt-b", "evt-c");
  }

  @Test
  @DisplayName("findAll sorts by activation outcome")
  void findAll_sortsByActivationAsc() {
    var page =
        repository.findAll(
            allRowsSpec(),
            PageRequest.of(0, 10, Sort.by("activationSuccessful").ascending()));
    assertThat(page.getContent().getFirst().isActivationSuccessful()).isFalse();
    assertThat(page.getContent().getLast().isActivationSuccessful()).isTrue();
  }

  @Test
  @DisplayName("sumJoulesGroupedByDay buckets by calendar day")
  void sumJoulesGroupedByDay_bucketsByDay() {
    Instant since = Instant.parse("2024-06-01T00:00:00Z");
    Instant until = Instant.parse("2024-06-03T00:00:00Z");
    List<DailyCompressionBucket> rows = repository.sumJoulesGroupedByDay(since, until);
    assertThat(rows).hasSize(2);
    assertThat(rows.getFirst().bucketDate()).isEqualTo(LocalDate.parse("2024-06-01"));
    assertThat(rows.getFirst().totalCompressions()).isEqualTo(1L);
    assertThat(rows.getFirst().successfulActivations()).isEqualTo(1L);
  }

  @NonNull
  private static Specification<TileCompressionEventEntity> allRowsSpec() {
    return (root, query, cb) -> cb.conjunction();
  }
}
