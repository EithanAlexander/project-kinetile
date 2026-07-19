package com.projectkinetile.physicsengine.api;

import com.projectkinetile.physicsengine.config.TileMonitoringProperties;

/**
 * Tile monitoring thresholds exposed to dashboard clients for inventory staleness badges.
 *
 * @param inactivityThresholdDays active tiles with no compression within this window are stale
 */
public record TileMonitoringConfigDTO(int inactivityThresholdDays) {

  public static TileMonitoringConfigDTO from(TileMonitoringProperties properties) {
    return new TileMonitoringConfigDTO(properties.getInactivityThresholdDays());
  }
}
