package com.projectkinetile.physicsengine.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.projectkinetile.physicsengine.api.ChokepointCatalogDTO;
import com.projectkinetile.physicsengine.api.CityCatalogDTO;
import com.projectkinetile.physicsengine.api.ManufacturerDTO;
import com.projectkinetile.physicsengine.api.PlaceTypeDTO;
import com.projectkinetile.physicsengine.api.TileCatalogDTO;
import com.projectkinetile.physicsengine.api.TileDetailDTO;
import com.projectkinetile.physicsengine.config.TileMonitoringProperties;
import com.projectkinetile.physicsengine.domain.ChokepointEntity;
import com.projectkinetile.physicsengine.domain.CityEntity;
import com.projectkinetile.physicsengine.domain.PlaceTypeEntity;
import com.projectkinetile.physicsengine.domain.TileCompressionActivityEntity;
import com.projectkinetile.physicsengine.domain.TileEntity;
import com.projectkinetile.physicsengine.repository.ChokepointRepository;
import com.projectkinetile.physicsengine.repository.CityRepository;
import com.projectkinetile.physicsengine.repository.PlaceTypeRepository;
import com.projectkinetile.physicsengine.repository.TileCompressionActivityRepository;
import com.projectkinetile.physicsengine.repository.TileManufacturerRepository;
import com.projectkinetile.physicsengine.repository.TileRepository;

/**
 * Read-only catalog queries for the city → chokepoint → tile hierarchy seeded by {@code db-init}.
 *
 * <p>Tile responses batch-load {@link TileCompressionActivityEntity} rows to avoid N+1 lookups.
 * Unknown city, chokepoint, or tile IDs raise {@link ResponseStatusException} with 404.
 */
@Service
@Transactional(readOnly = true)
public class InfrastructureCatalogService {

  private final PlaceTypeRepository placeTypeRepository;
  private final TileManufacturerRepository manufacturerRepository;
  private final CityRepository cityRepository;
  private final ChokepointRepository chokepointRepository;
  private final TileRepository tileRepository;
  private final TileCompressionActivityRepository activityRepository;
  private final TileMonitoringProperties tileMonitoringProperties;

  public InfrastructureCatalogService(
      PlaceTypeRepository placeTypeRepository,
      TileManufacturerRepository manufacturerRepository,
      CityRepository cityRepository,
      ChokepointRepository chokepointRepository,
      TileRepository tileRepository,
      TileCompressionActivityRepository activityRepository,
      TileMonitoringProperties tileMonitoringProperties) {
    this.placeTypeRepository = placeTypeRepository;
    this.manufacturerRepository = manufacturerRepository;
    this.cityRepository = cityRepository;
    this.chokepointRepository = chokepointRepository;
    this.tileRepository = tileRepository;
    this.activityRepository = activityRepository;
    this.tileMonitoringProperties = tileMonitoringProperties;
  }

  /** All place types, sorted by code. */
  public List<PlaceTypeDTO> listPlaceTypes() {
    return placeTypeRepository.findAll().stream()
        .sorted((a, b) -> a.getCode().compareTo(b.getCode()))
        .map(pt -> new PlaceTypeDTO(pt.getId(), pt.getCode(), pt.getLabel(), pt.getTrafficTier()))
        .toList();
  }

  /** All manufacturers with a count of their active tiles. */
  public List<ManufacturerDTO> listManufacturers() {
    return manufacturerRepository.findAllWithActiveTileCounts().stream()
        .map(
            row ->
                new ManufacturerDTO(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((Number) row[2]).longValue()))
        .toList();
  }

  /** All cities with per-city chokepoint and active-tile counts. */
  public List<CityCatalogDTO> listCities() {
    Map<Long, Long> chokepointCounts = toCountMap(chokepointRepository.countChokepointsByCityIdGrouped());
    Map<Long, Long> activeTileCounts = toCountMap(tileRepository.countActiveTilesByCityIdGrouped());
    return cityRepository.findAll().stream()
        .sorted((a, b) -> a.getName().compareTo(b.getName()))
        .map(
            city ->
                new CityCatalogDTO(
                    city.getId(),
                    city.getCode(),
                    city.getName(),
                    chokepointCounts.getOrDefault(city.getId(), 0L),
                    activeTileCounts.getOrDefault(city.getId(), 0L)))
        .toList();
  }

  /**
   * Chokepoints in the given city, sorted by name.
   *
   * @param cityId catalog city primary key
   * @throws ResponseStatusException 404 when the city does not exist
   */
  public List<ChokepointCatalogDTO> listChokepointsForCity(@NonNull Long cityId) {
    requireCity(cityId);
    return chokepointRepository.findByCityIdOrderByNameAsc(cityId).stream()
        .map(this::toChokepointDto)
        .toList();
  }

  /**
   * Paginated tile summaries for a chokepoint, including compression activity when recorded.
   *
   * @param chokepointId catalog chokepoint primary key
   * @throws ResponseStatusException 404 when the chokepoint does not exist
   */
  public Page<TileCatalogDTO> listTilesForChokepoint(
      @NonNull Long chokepointId, Pageable pageable) {
    requireChokepoint(chokepointId);
    Page<TileEntity> page =
        tileRepository.findByChokepointIdOrderByTileIdAsc(chokepointId, pageable);
    Map<UUID, TileCompressionActivityEntity> activities = loadActivities(page.getContent());
    return page.map(tile -> toTileCatalogDto(tile, activities));
  }

  /**
   * Full tile detail with city, chokepoint, and place-type context.
   *
   * @throws ResponseStatusException 404 when the tile does not exist
   */
  public TileDetailDTO getTile(UUID tileId) {
    TileEntity tile =
        tileRepository
            .findDetailedByTileId(tileId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tile not found"));
    Map<UUID, TileCompressionActivityEntity> activities =
        loadActivities(List.of(tile));
    return toTileDetailDto(tile, activities);
  }

  /**
   * Active tiles with no compression since {@link TileMonitoringProperties#getInactivityThresholdDays()}.
   */
  public List<TileDetailDTO> listStaleTiles() {
    Instant threshold =
        Instant.now().minus(tileMonitoringProperties.getInactivityThresholdDays(), ChronoUnit.DAYS);
    List<TileEntity> stale = tileRepository.findStaleActiveTiles(threshold);
    Map<UUID, TileCompressionActivityEntity> activities = loadActivities(stale);
    return stale.stream().map(tile -> toTileDetailDto(tile, activities)).toList();
  }

  private Map<Long, Long> toCountMap(List<Object[]> rows) {
    Map<Long, Long> counts = new HashMap<>();
    for (Object[] row : rows) {
      counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
    }
    return counts;
  }

  private Map<UUID, TileCompressionActivityEntity> loadActivities(List<TileEntity> tiles) {
    List<UUID> tileIds =
        tiles.stream().map(InfrastructureCatalogService::tileIdOf).filter(Objects::nonNull).distinct().toList();
    if (tileIds.isEmpty()) {
      return Map.of();
    }
    return activityRepository.findByTileIdIn(tileIds).stream()
        .collect(Collectors.toMap(InfrastructureCatalogService::activityTileId, activity -> activity));
  }

  private ChokepointCatalogDTO toChokepointDto(ChokepointEntity cp) {
    PlaceTypeEntity pt = cp.getPlaceType();
    return new ChokepointCatalogDTO(
        cp.getId(),
        cp.getCode(),
        cp.getName(),
        pt.getCode(),
        pt.getLabel(),
        pt.getTrafficTier(),
        tileRepository.countActiveByChokepointId(cp.getId()));
  }

  private TileCatalogDTO toTileCatalogDto(
      TileEntity tile, Map<UUID, TileCompressionActivityEntity> activities) {
    UUID tileId = Objects.requireNonNull(tile.getTileId());
    Optional<TileCompressionActivityEntity> activity = Optional.ofNullable(activities.get(tileId));
    return new TileCatalogDTO(
        tileId,
        tile.getManufacturer().getName(),
        tile.getSize(),
        tile.getColor(),
        tile.getInstallationDate(),
        tile.getRemovalDate(),
        tile.getLastInspectionDate(),
        tile.isActive(),
        activity.map(InfrastructureCatalogService::lastCompressionAt).orElse(null),
        activity.map(InfrastructureCatalogService::totalCompressions).orElse(0L));
  }

  private TileDetailDTO toTileDetailDto(
      TileEntity tile, Map<UUID, TileCompressionActivityEntity> activities) {
    UUID tileId = Objects.requireNonNull(tile.getTileId());
    var cp = tile.getChokepoint();
    var city = cp.getCity();
    var pt = cp.getPlaceType();
    Optional<TileCompressionActivityEntity> activity = Optional.ofNullable(activities.get(tileId));
    return new TileDetailDTO(
        tileId,
        city.getName(),
        city.getCode(),
        cp.getName(),
        cp.getCode(),
        pt.getCode(),
        pt.getTrafficTier(),
        tile.getManufacturer().getName(),
        tile.getSize(),
        tile.getColor(),
        tile.getInstallationDate(),
        tile.getRemovalDate(),
        tile.getLastInspectionDate(),
        tile.isActive(),
        activity.map(InfrastructureCatalogService::firstCompressionAt).orElse(null),
        activity.map(InfrastructureCatalogService::lastCompressionAt).orElse(null),
        activity.map(InfrastructureCatalogService::totalCompressions).orElse(0L));
  }

  private static UUID tileIdOf(@NonNull TileEntity tile) {
    return tile.getTileId();
  }

  private static UUID activityTileId(@NonNull TileCompressionActivityEntity activity) {
    return activity.getTileId();
  }

  private static Instant firstCompressionAt(@NonNull TileCompressionActivityEntity activity) {
    return activity.getFirstCompressionAt();
  }

  private static Instant lastCompressionAt(@NonNull TileCompressionActivityEntity activity) {
    return activity.getLastCompressionAt();
  }

  private static long totalCompressions(@NonNull TileCompressionActivityEntity activity) {
    return activity.getTotalCompressions();
  }

  private CityEntity requireCity(@NonNull Long cityId) {
    return cityRepository
        .findById(cityId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found"));
  }

  private ChokepointEntity requireChokepoint(@NonNull Long chokepointId) {
    return chokepointRepository
        .findById(chokepointId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chokepoint not found"));
  }
}
