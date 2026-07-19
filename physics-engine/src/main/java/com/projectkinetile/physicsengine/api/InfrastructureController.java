package com.projectkinetile.physicsengine.api;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.service.InfrastructureCatalogService;

/**
 * REST controller for the read-only infrastructure catalog seeded by {@code db-init}.
 *
 * <p>Exposes the city → chokepoint → tile hierarchy, reference data (place types, manufacturers),
 * and operational views (tile detail, stale active tiles). All labels are resolved from PostgreSQL
 * catalog joins — not from Kafka payloads.
 *
 * <p>Base path: {@link ApiPaths#INFRASTRUCTURE} ({@code /api/v1/infrastructure}).
 */
@RestController
@RequestMapping(ApiPaths.INFRASTRUCTURE)
public class InfrastructureController {

  private final InfrastructureCatalogService catalogService;
  private final AppSecurityProperties appSecurityProperties;

  /** Wires the catalog service and API security limits used for pagination clamping. */
  public InfrastructureController(
      InfrastructureCatalogService catalogService, AppSecurityProperties appSecurityProperties) {
    this.catalogService = catalogService;
    this.appSecurityProperties = appSecurityProperties;
  }

  /** Lists all place types (code, label, traffic tier). */
  @GetMapping("/place-types")
  public List<PlaceTypeDTO> listPlaceTypes() {
    return catalogService.listPlaceTypes();
  }

  /** Lists all tile manufacturers with active-tile counts. */
  @GetMapping("/manufacturers")
  public List<ManufacturerDTO> listManufacturers() {
    return catalogService.listManufacturers();
  }

  /** Lists all cities with chokepoint and active-tile counts. */
  @GetMapping("/cities")
  public List<CityCatalogDTO> listCities() {
    return catalogService.listCities();
  }

  /**
   * Lists chokepoints within a city.
   *
   * @param cityId catalog city primary key
   * @return chokepoints ordered by name; {@code 404} when the city is unknown
   */
  @GetMapping("/cities/{cityId}/chokepoints")
  public List<ChokepointCatalogDTO> listChokepoints(@PathVariable long cityId) {
    return catalogService.listChokepointsForCity(cityId);
  }

  /**
   * Paginated tiles installed at a chokepoint.
   *
   * @param chokepointId catalog chokepoint primary key
   * @param page zero-based page index (clamped to {@code >= 0})
   * @param size page size (clamped to {@code 1..ledgerMaxPageSize})
   * @return tile catalog rows with optional compression activity; {@code 404} when chokepoint unknown
   */
  @GetMapping("/chokepoints/{chokepointId}/tiles")
  public Page<TileCatalogDTO> listTiles(
      @PathVariable long chokepointId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.clamp(size, 1, appSecurityProperties.getApi().getLedgerMaxPageSize());
    Pageable pageable = PageRequest.of(safePage, safeSize);
    return catalogService.listTilesForChokepoint(chokepointId, pageable);
  }

  /**
   * Full detail for one physical tile by catalog UUID.
   *
   * @param tileId tile UUID from the registry
   * @return city, chokepoint, manufacturer, and activity fields; {@code 404} when not found
   */
  @GetMapping("/tiles/{tileId}")
  public TileDetailDTO getTile(@PathVariable UUID tileId) {
    return catalogService.getTile(tileId);
  }

  /** Active tiles with no compression since the configured inactivity threshold. */
  @GetMapping("/tiles/stale")
  public List<TileDetailDTO> listStaleTiles() {
    return catalogService.listStaleTiles();
  }
}
