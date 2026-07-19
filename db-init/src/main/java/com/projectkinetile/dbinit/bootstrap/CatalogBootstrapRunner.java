package com.projectkinetile.dbinit.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.projectkinetile.dbinit.config.BootstrapProperties;
import com.projectkinetile.dbinit.registry.InfrastructureRegistry;
import com.projectkinetile.dbinit.registry.RegistryChokepoint;
import com.projectkinetile.dbinit.registry.RegistryCity;
import com.projectkinetile.dbinit.registry.RegistryTileInstance;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent catalog bootstrap: seeds place types, manufacturers, cities, chokepoints, and tiles
 * from infrastructure-registry.json when the catalog is empty (or when {@code force} is set).
 */
@Component
public class CatalogBootstrapRunner {

  private static final Logger log = LoggerFactory.getLogger(CatalogBootstrapRunner.class);

  private static final List<PlaceTypeSeed> PLACE_TYPES =
      List.of(
          new PlaceTypeSeed("MARKET", "Market", "HIGH"),
          new PlaceTypeSeed("BUS_STATION", "Bus Station", "HIGH"),
          new PlaceTypeSeed("TRAIN_STATION", "Train Station", "HIGH"),
          new PlaceTypeSeed("TURNSTILE", "Turnstile", "HIGH"),
          new PlaceTypeSeed("MALL", "Mall", "HIGH"),
          new PlaceTypeSeed("SQUARE", "Square", "MEDIUM"),
          new PlaceTypeSeed("PROMENADE", "Promenade", "MEDIUM"),
          new PlaceTypeSeed("WALKWAY", "Walkway", "MEDIUM"),
          new PlaceTypeSeed("BIKE_LANE", "Bike Lane", "MEDIUM"),
          new PlaceTypeSeed("BEACH", "Beach", "MEDIUM"),
          new PlaceTypeSeed("PARK", "Park", "MEDIUM"),
          new PlaceTypeSeed("BRIDGE", "Bridge", "MEDIUM"),
          new PlaceTypeSeed("STREET", "Street", "LOW"));

  private final JdbcTemplate jdbcTemplate;
  private final BootstrapProperties bootstrapProperties;
  private final ResourceLoader resourceLoader;
  private final ObjectMapper objectMapper;

  public CatalogBootstrapRunner(
      JdbcTemplate jdbcTemplate,
      BootstrapProperties bootstrapProperties,
      ResourceLoader resourceLoader) {
    this.jdbcTemplate = jdbcTemplate;
    this.bootstrapProperties = bootstrapProperties;
    this.resourceLoader = resourceLoader;
    this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  /** Runs catalog bootstrap when needed. */
  @Transactional
  public void runBootstrap() {
    boolean catalogEmpty = isCatalogEmpty();
    if (!catalogEmpty && !bootstrapProperties.isForce()) {
      log.info("Catalog already initialized; skipping bootstrap (use DB_INIT_FORCE=true to re-seed)");
      return;
    }
    if (!catalogEmpty && bootstrapProperties.isForce()) {
      log.warn("DB_INIT_FORCE=true: truncating catalog tables before re-bootstrap");
      truncateCatalog();
    }

    seedPlaceTypes();
    InfrastructureRegistry registry = loadRegistry();
    seedManufacturers(registry.manufacturers());
    seedInfrastructure(registry.cities());
    log.info(
        "Catalog bootstrap complete: {} cities, {} tiles",
        countCities(),
        countTiles());
  }

  private boolean isCatalogEmpty() {
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cities", Integer.class);
    return count == null || count == 0;
  }

  private void truncateCatalog() {
    jdbcTemplate.execute(
        """
        TRUNCATE tile_compression_activity, tile_compression_events, tiles,
                 chokepoints, cities, tile_manufacturers, place_types
        RESTART IDENTITY CASCADE
        """);
  }

  private void seedPlaceTypes() {
    for (PlaceTypeSeed seed : PLACE_TYPES) {
      jdbcTemplate.update(
          """
          INSERT INTO place_types (code, label, traffic_tier)
          VALUES (?, ?, ?)
          ON CONFLICT (code) DO NOTHING
          """,
          seed.code(),
          seed.label(),
          seed.trafficTier());
    }
  }

  private InfrastructureRegistry loadRegistry() {
    String location = bootstrapProperties.getRegistryResource();
    Resource resource = resourceLoader.getResource(location);
    if (!resource.exists()) {
      throw new IllegalStateException("Registry resource not found: " + location);
    }
    try (InputStream in = resource.getInputStream()) {
      return objectMapper.readValue(in, InfrastructureRegistry.class);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse registry: " + location, e);
    }
  }

  private void seedManufacturers(List<String> manufacturers) {
    for (String name : manufacturers) {
      jdbcTemplate.update(
          "INSERT INTO tile_manufacturers (name) VALUES (?) ON CONFLICT (name) DO NOTHING", name);
    }
  }

  private void seedInfrastructure(List<RegistryCity> cities) {
    Map<String, Long> placeTypeIds = loadPlaceTypeIds();
    Map<String, Long> manufacturerIds = loadManufacturerIds();

    for (RegistryCity city : cities) {
      Long cityId =
          jdbcTemplate.queryForObject(
              """
              INSERT INTO cities (code, name) VALUES (?, ?)
              ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
              RETURNING id
              """,
              Long.class,
              city.code(),
              city.name());

      for (RegistryChokepoint chokepoint : city.chokepoints()) {
        Long placeTypeId = placeTypeIds.get(chokepoint.placeType());
        if (placeTypeId == null) {
          throw new IllegalStateException(
              "Unknown place type '" + chokepoint.placeType() + "' for chokepoint " + chokepoint.code());
        }

        Long chokepointId =
            jdbcTemplate.queryForObject(
                """
                INSERT INTO chokepoints (city_id, place_type_id, code, name)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (city_id, code) DO UPDATE SET
                  place_type_id = EXCLUDED.place_type_id,
                  name = EXCLUDED.name
                RETURNING id
                """,
                Long.class,
                cityId,
                placeTypeId,
                chokepoint.code(),
                chokepoint.name());

        for (RegistryTileInstance tile : chokepoint.tileInstances()) {
          Long manufacturerId = manufacturerIds.get(tile.manufacturer());
          if (manufacturerId == null) {
            throw new IllegalStateException(
                "Unknown manufacturer '" + tile.manufacturer() + "' for tile " + tile.tileId());
          }
          UUID tileUuid = UUID.fromString(tile.tileId());
          jdbcTemplate.update(
              """
              INSERT INTO tiles (
                tile_id, chokepoint_id, manufacturer_id, size, color,
                installation_date, removal_date, last_inspection_date, is_active
              ) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?)
              ON CONFLICT (tile_id) DO NOTHING
              """,
              tileUuid,
              chokepointId,
              manufacturerId,
              tile.size(),
              tile.color(),
              tile.installationDate(),
              tile.lastInspectionDate(),
              tile.isActive());
        }
      }
    }
  }

  private Map<String, Long> loadPlaceTypeIds() {
    return jdbcTemplate.query(
        "SELECT code, id FROM place_types",
        rs -> {
          Map<String, Long> map = new LinkedHashMap<>();
          while (rs.next()) {
            map.put(rs.getString("code"), rs.getLong("id"));
          }
          return map;
        });
  }

  private Map<String, Long> loadManufacturerIds() {
    return jdbcTemplate.query(
        "SELECT name, id FROM tile_manufacturers",
        rs -> {
          Map<String, Long> map = new LinkedHashMap<>();
          while (rs.next()) {
            map.put(rs.getString("name"), rs.getLong("id"));
          }
          return map;
        });
  }

  private int countCities() {
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cities", Integer.class);
    return count != null ? count : 0;
  }

  private int countTiles() {
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tiles", Integer.class);
    return count != null ? count : 0;
  }

  private record PlaceTypeSeed(String code, String label, String trafficTier) {}
}
