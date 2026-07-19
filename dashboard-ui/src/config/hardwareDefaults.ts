import type { HardwareConfig } from '../api/types/hardware'

/**
 * Documented defaults aligned with {@code application.yml} hardware.tile.* fallbacks.
 * Used when {@code /api/v1/config/hardware} is unavailable.
 */
export const DEFAULT_HARDWARE_CONFIG: HardwareConfig = {
  activationThresholdNewtons: 100,
  minRatedOutputJoules: 2,
  maxRatedOutputJoules: 5,
  maxScaleMassKg: 90,
  maxDisplacementMeters: 0.0001,
  gravity: 9.81,
}
