/** Mirrors {@code LocationCompressionMetricsDTO} from the physics engine. */
export interface LocationEnergyRow {
  city: string
  location: string
  totalJoules: number
  totalWattHours: number
  totalCompressions: number
  successfulActivations: number
}
