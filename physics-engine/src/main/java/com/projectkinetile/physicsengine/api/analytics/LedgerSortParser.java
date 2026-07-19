package com.projectkinetile.physicsengine.api.analytics;

import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

/** Parses and whitelists ledger sort expressions for safe JPA queries. */
public final class LedgerSortParser {

  private static final String KEY_EVENT_TIMESTAMP = "eventTimestamp";
  private static final String KEY_CALCULATED_ENERGY = "calculatedEnergyJoules";
  private static final String KEY_IMPACT_MULTIPLIER = "impactMultiplier";
  private static final String KEY_MASS_KG = "massKg";
  private static final String KEY_FORCE = "calculatedForceNewtons";
  private static final String KEY_EVENT_ID = "eventId";
  private static final String KEY_ID = "id";
  private static final String KEY_ACTIVATION = "activationSuccessful";
  private static final String KEY_CITY = "city";
  private static final String KEY_LOCATION = "location";
  private static final String SORT_TILE_LOCATION = "tile.chokepoint.name";
  private static final String SORT_TILE_CITY = "tile.chokepoint.city.name";
  private static final String SORT_MANUFACTURER = "tile.manufacturer.name";

  private LedgerSortParser() {}

  /**
   * Parses a {@code "property,direction"} sort expression into a safe {@link Sort}.
   *
   * @param sortParam raw sort expression (may be {@code null} or blank)
   * @return non-null sort restricted to whitelisted properties
   */
  public static @NonNull Sort parseLedgerSort(String sortParam) {
    if (sortParam == null || sortParam.isBlank()) {
      return Objects.requireNonNull(Sort.by(Sort.Direction.DESC, KEY_EVENT_TIMESTAMP));
    }
    String[] parts = sortParam.trim().split(",", 2);
    String propKey = parts[0].trim().toLowerCase();
    Sort.Direction direction =
        parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    String property =
        switch (propKey) {
          case KEY_EVENT_TIMESTAMP, "timestamp", "time", "posted" -> KEY_EVENT_TIMESTAMP;
          case "energy", "joules", "wh" -> KEY_CALCULATED_ENERGY;
          case "impact", "impactmultiplier" -> KEY_IMPACT_MULTIPLIER;
          case "mass" -> KEY_MASS_KG;
          case "force" -> KEY_FORCE;
          case "tile", "tileid" -> "tileId";
          case "event", "eventid" -> KEY_EVENT_ID;
          case KEY_CITY -> SORT_TILE_CITY;
          case KEY_LOCATION, "site", "chokepoint" -> SORT_TILE_LOCATION;
          case "brand", "manufacturer", "manufacturername" -> SORT_MANUFACTURER;
          case "activation", "activationsuccessful" -> KEY_ACTIVATION;
          case KEY_ID -> KEY_ID;
          default -> KEY_EVENT_TIMESTAMP;
        };
    return Objects.requireNonNull(Sort.by(direction, property));
  }
}
