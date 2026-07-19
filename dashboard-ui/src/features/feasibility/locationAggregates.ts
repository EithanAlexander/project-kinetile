import type { EdgeDevice } from '../../api/types/devices'
import type { LocationEnergyRow } from '../../api/types/locations'
import { formatDecimal } from '../../utils/energyFormat'

/**
 * Pure helpers for location feasibility views: row normalization, city roll-ups,
 * device coverage math, and honest off-grid tier labels.
 */

export type { LocationEnergyRow }

export function joulesValue(row: LocationEnergyRow | Record<string, unknown>): number {
  const n = Number(row?.totalJoules)
  return Number.isFinite(n) ? n : 0
}

export function wattHoursValue(row: LocationEnergyRow | Record<string, unknown>): number {
  const n = Number(row?.totalWattHours)
  return Number.isFinite(n) ? n : 0
}

/** True when a row carries compression counters (guards partial or legacy payloads). */
export function hasCompressionMetrics(row: LocationEnergyRow | Record<string, unknown>): boolean {
  return row != null && typeof row.totalCompressions === 'number'
}

export function asNonNegativeInt(value: unknown): number {
  const n = Number(value)
  if (!Number.isFinite(n) || n < 0) return 0
  return Math.trunc(n)
}

function asText(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

export function locationSortKey(row: LocationEnergyRow | Record<string, unknown>): string {
  const city = asText(row?.city)
  const loc = asText(row?.location)
  return `${city} ${loc}`.trim().toLowerCase()
}

export function normalizeCity(row: LocationEnergyRow | Record<string, unknown>): string {
  return asText(row?.city)
}

export function normalizeSite(row: LocationEnergyRow | Record<string, unknown>): string {
  return asText(row?.location)
}

/** Stable composite key for city + site filter options. */
export function siteOptionKey(city: string, site: string): string {
  return JSON.stringify([city, site])
}

/** Inverse of {@link siteOptionKey}; returns empty strings on malformed input. */
export function parseSiteOptionKey(key: string): { city: string; site: string } {
  try {
    const pair = JSON.parse(key) as unknown
    if (Array.isArray(pair) && pair.length === 2) {
      return { city: String(pair[0] ?? ''), site: String(pair[1] ?? '') }
    }
  } catch {
    /* ignore */
  }
  return { city: '', site: '' }
}

/** Sums energy rows into one row per city (for “by city” view). */
export function aggregateRowsByCity(rows: LocationEnergyRow[]): LocationEnergyRow[] {
  const map = new Map<
    string,
    {
      city: string
      totalJoules: number
      totalWattHours: number
      nSites: number
      totalCompressions: number
      successfulActivations: number
    }
  >()
  for (const r of rows) {
    const cityRaw = normalizeCity(r)
    const mapKey = cityRaw || '\u0000'
    let agg = map.get(mapKey)
    if (!agg) {
      agg = {
        city: cityRaw || '—',
        totalJoules: 0,
        totalWattHours: 0,
        nSites: 0,
        totalCompressions: 0,
        successfulActivations: 0,
      }
      map.set(mapKey, agg)
    }
    agg.totalJoules += joulesValue(r)
    agg.totalWattHours += wattHoursValue(r)
    agg.nSites += 1
    if (hasCompressionMetrics(r)) {
      const row = r as { totalCompressions?: unknown; successfulActivations?: unknown }
      agg.totalCompressions += asNonNegativeInt(row.totalCompressions)
      agg.successfulActivations += asNonNegativeInt(row.successfulActivations)
    }
  }
  return [...map.values()].map((agg) => ({
    city: agg.city,
    location:
      agg.nSites === 1
        ? '1 chokepoint (city roll-up)'
        : `${agg.nSites} chokepoints (city roll-up)`,
    totalJoules: agg.totalJoules,
    totalWattHours: agg.totalWattHours,
    totalCompressions: agg.totalCompressions,
    successfulActivations: agg.successfulActivations,
  }))
}

/** Totals an array of location rows into a single scoped activation summary. */
export function sumRows(rows: LocationEnergyRow[]): {
  totalCompressions: number
  successfulActivations: number
  totalJoules: number
  totalWattHours: number
} {
  let totalCompressions = 0
  let successfulActivations = 0
  let totalJoules = 0
  let totalWattHours = 0
  for (const r of rows) {
    totalJoules += joulesValue(r)
    totalWattHours += wattHoursValue(r)
    if (hasCompressionMetrics(r)) {
      const row = r as { totalCompressions?: unknown; successfulActivations?: unknown }
      totalCompressions += asNonNegativeInt(row.totalCompressions)
      successfulActivations += asNonNegativeInt(row.successfulActivations)
    }
  }
  return { totalCompressions, successfulActivations, totalJoules, totalWattHours }
}

/** Sort key for table display; uses city only when `groupBy === 'city'`. */
export function displaySortKey(row: LocationEnergyRow | Record<string, unknown>, groupBy: string): string {
  if (groupBy === 'city') {
    return asText(row?.city).toLowerCase()
  }
  return locationSortKey(row)
}

/** Daily Wh generated as a percentage of the selected device's daily requirement. */
export function feasibilityPercent(
  totalWh: unknown,
  device: EdgeDevice | null | undefined,
): number | null {
  const wh = Number(totalWh)
  const req = device == null ? Number.NaN : Number(device.dailyRequiredWh)
  if (!Number.isFinite(wh) || !Number.isFinite(req) || req <= 0) return null
  return (wh / req) * 100
}

/**
 * Estimate how many days of an edge device's load the generated energy covers.
 *
 * @param totalWh - Total generated energy in watt-hours.
 * @param device - Catalog device with a daily Wh requirement.
 * @returns Days of runtime, or `null` when inputs are missing or invalid.
 */
export function runtimeDays(
  totalWh: unknown,
  device: EdgeDevice | null | undefined,
): number | null {
  const wh = Number(totalWh)
  const req = device == null ? Number.NaN : Number(device.dailyRequiredWh)
  if (!Number.isFinite(wh) || !Number.isFinite(req) || req <= 0) return null
  return wh / req
}

/** Maps feasibility % to honest UI badge copy and CSS modifiers. */
export function feasibilityBadge(pct: number | null): {
  label: string
  className: string
  barClass: string
} {
  if (pct === null) {
    return {
      label: '⚪ No target selected',
      className: 'rgf-feas-badge rgf-feas-badge--none',
      barClass: 'rgf-progress-fill rgf-progress-fill--none',
    }
  }
  if (pct < 50) {
    return {
      label: '🔴 Insufficient (Requires Grid)',
      className: 'rgf-feas-badge rgf-feas-badge--low',
      barClass: 'rgf-progress-fill rgf-progress-fill--low',
    }
  }
  if (pct <= 100) {
    return {
      label: '🟡 Requires Some Grid Support',
      className: 'rgf-feas-badge rgf-feas-badge--mid',
      barClass: 'rgf-progress-fill rgf-progress-fill--mid',
    }
  }
  return {
    label: '🟢 Viable Off-Grid',
    className: 'rgf-feas-badge rgf-feas-badge--high',
    barClass: 'rgf-progress-fill rgf-progress-fill--high',
  }
}

/** Formats successful/total compressions as a percentage, or null when total is zero. */
export function formatActivationRate(successful: number, total: number): string | null {
  if (total <= 0) return null
  const pct = (successful / total) * 100
  return `${formatDecimal(pct, 1)}% activation rate`
}
