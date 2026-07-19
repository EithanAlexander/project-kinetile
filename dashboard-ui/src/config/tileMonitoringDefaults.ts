import type { TileMonitoringConfig } from '../api/types/tileMonitoring'

/**
 * Documented defaults aligned with {@code application.yml} app.tile-monitoring fallbacks.
 * Used when {@code /api/v1/config/tile-monitoring} is unavailable.
 */
export const DEFAULT_TILE_MONITORING_CONFIG: TileMonitoringConfig = {
  inactivityThresholdDays: 5,
}
