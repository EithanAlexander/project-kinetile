/** Daily bucket row from compression analytics time-series endpoints. */
export interface TimeseriesRow {
  bucketStart: string
  city?: string
  location?: string
  totalJoules: number
  totalWattHours: number
  totalCompressions: number
  successfulActivations: number
}
