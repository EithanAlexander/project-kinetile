import type { EdgeDevice } from './types/devices'
import type { HardwareConfig } from './types/hardware'
import type { TileMonitoringConfig } from './types/tileMonitoring'
import type { LocationEnergyRow } from './types/locations'
import type { TimeseriesRow } from './types/timeseries'

/**
 * Runtime validators for untrusted API JSON (BYOD / zero-trust boundary).
 * Single-row parsers return null on failure; list parsers either reject the
 * whole payload (parseEdgeDeviceList, parseLocationEnergyList) or skip bad rows
 * (parseTimeseriesRowList).
 */

/** Thrown when an API payload fails runtime validation. */
export class ApiValidationError extends Error {
  constructor(message = 'Server returned malformed data') {
    super(message)
    this.name = 'ApiValidationError'
  }
}

export function parseFiniteNumber(value: unknown): number | null {
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

export function parseString(value: unknown): string | null {
  if (typeof value !== 'string') return null
  return value
}

export function parseRecord(value: unknown): Record<string, unknown> | null {
  if (value == null || typeof value !== 'object' || Array.isArray(value)) return null
  return value as Record<string, unknown>
}

export function parseNonNegativeInt(value: unknown): number | null {
  const n = parseFiniteNumber(value)
  if (n == null || n < 0) return null
  return Math.trunc(n)
}

/** Parses one EdgeDevice; returns null if any field is invalid. */
export function parseEdgeDevice(value: unknown): EdgeDevice | null {
  const o = parseRecord(value)
  if (!o) return null
  const id = parseString(o.id)
  const name = parseString(o.name)
  const dailyRequiredWh = parseFiniteNumber(o.dailyRequiredWh)
  if (!id || !name || dailyRequiredWh == null || dailyRequiredWh < 0) return null
  return { id, name, dailyRequiredWh }
}

/** Strict catalog parse — throws if the root is not an array or any element is invalid. */
export function parseEdgeDeviceList(value: unknown): EdgeDevice[] {
  if (!Array.isArray(value)) {
    throw new ApiValidationError()
  }
  const out: EdgeDevice[] = []
  for (const item of value) {
    const parsed = parseEdgeDevice(item)
    if (parsed == null) {
      throw new ApiValidationError()
    }
    out.push(parsed)
  }
  return out
}

/** Parses one location energy aggregate row (LocationCompressionMetricsDTO shape). */
export function parseLocationEnergyRow(value: unknown): LocationEnergyRow | null {
  const o = parseRecord(value)
  if (!o) return null
  const city = parseString(o.city)
  const location = parseString(o.location)
  const totalJoules = parseFiniteNumber(o.totalJoules)
  const totalWattHours = parseFiniteNumber(o.totalWattHours)
  const totalCompressions = parseNonNegativeInt(o.totalCompressions)
  const successfulActivations = parseNonNegativeInt(o.successfulActivations)
  if (
    city == null ||
    location == null ||
    totalJoules == null ||
    totalWattHours == null ||
    totalCompressions == null ||
    successfulActivations == null
  ) {
    return null
  }
  return {
    city,
    location,
    totalJoules,
    totalWattHours,
    totalCompressions,
    successfulActivations,
  }
}

/** Strict locations list — throws if the root is not an array or any row is invalid. */
export function parseLocationEnergyList(value: unknown): LocationEnergyRow[] {
  if (!Array.isArray(value)) {
    throw new ApiValidationError()
  }
  const out: LocationEnergyRow[] = []
  for (const item of value) {
    const parsed = parseLocationEnergyRow(item)
    if (parsed == null) {
      throw new ApiValidationError()
    }
    out.push(parsed)
  }
  return out
}

function parseInstantString(value: unknown): string | null {
  if (typeof value === 'string' && value.trim() !== '') return value
  return null
}

/** Parses one daily timeseries bucket; city and location are optional per endpoint. */
export function parseTimeseriesRow(value: unknown): TimeseriesRow | null {
  const o = parseRecord(value)
  if (!o) return null
  const bucketStart = parseInstantString(o.bucketStart)
  const totalJoules = parseFiniteNumber(o.totalJoules)
  const totalWattHours = parseFiniteNumber(o.totalWattHours)
  const totalCompressions = parseNonNegativeInt(o.totalCompressions)
  const successfulActivations = parseNonNegativeInt(o.successfulActivations)
  if (
    bucketStart == null ||
    totalJoules == null ||
    totalWattHours == null ||
    totalCompressions == null ||
    successfulActivations == null
  ) {
    return null
  }
  const city = parseString(o.city) ?? undefined
  const location = parseString(o.location) ?? undefined
  return {
    bucketStart,
    city,
    location,
    totalJoules,
    totalWattHours,
    totalCompressions,
    successfulActivations,
  }
}

/** Lenient timeseries list — skips invalid rows (warns in dev); returns [] if root is not an array. */
export function parseTimeseriesRowList(value: unknown): TimeseriesRow[] {
  if (!Array.isArray(value)) return []
  const out: TimeseriesRow[] = []
  for (const item of value) {
    const parsed = parseTimeseriesRow(item)
    if (parsed != null) out.push(parsed)
    else if (import.meta.env.DEV) {
      console.warn('[validate] skipped malformed timeseries row', item)
    }
  }
  return out
}

/** Parses /api/v1/config/hardware; throws on missing or non-numeric fields. */
export function parseHardwareConfig(value: unknown): HardwareConfig {
  const o = parseRecord(value)
  if (!o) throw new ApiValidationError()
  const activationThresholdNewtons = parseFiniteNumber(o.activationThresholdNewtons)
  const minRatedOutputJoules = parseFiniteNumber(o.minRatedOutputJoules)
  const maxRatedOutputJoules = parseFiniteNumber(o.maxRatedOutputJoules)
  const maxScaleMassKg = parseFiniteNumber(o.maxScaleMassKg)
  const maxDisplacementMeters = parseFiniteNumber(o.maxDisplacementMeters)
  const gravity = parseFiniteNumber(o.gravity)
  if (
    activationThresholdNewtons == null ||
    minRatedOutputJoules == null ||
    maxRatedOutputJoules == null ||
    maxScaleMassKg == null ||
    maxDisplacementMeters == null ||
    gravity == null
  ) {
    throw new ApiValidationError()
  }
  return {
    activationThresholdNewtons,
    minRatedOutputJoules,
    maxRatedOutputJoules,
    maxScaleMassKg,
    maxDisplacementMeters,
    gravity,
  }
}

/** Parses /api/v1/config/tile-monitoring; throws on missing or non-numeric fields. */
export function parseTileMonitoringConfig(value: unknown): TileMonitoringConfig {
  const o = parseRecord(value)
  if (!o) throw new ApiValidationError()
  const inactivityThresholdDays = parseFiniteNumber(o.inactivityThresholdDays)
  if (inactivityThresholdDays == null || inactivityThresholdDays < 1) {
    throw new ApiValidationError()
  }
  return { inactivityThresholdDays: Math.floor(inactivityThresholdDays) }
}
