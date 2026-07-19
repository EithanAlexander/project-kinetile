/** Tile hardware constants exposed by {@code /api/v1/config/hardware}. */
export interface HardwareConfig {
  activationThresholdNewtons: number
  minRatedOutputJoules: number
  maxRatedOutputJoules: number
  maxScaleMassKg: number
  maxDisplacementMeters: number
  gravity: number
}
