/**
 * Formats large energy / distance numbers with grouped thousands.
 */
export function formatDecimal(value: unknown, maximumFractionDigits = 2): string {
  const n = Number(value)
  if (!Number.isFinite(n)) return '—'
  return new Intl.NumberFormat('en-US', { maximumFractionDigits }).format(n)
}

/** Matches backend EnergyAnalyticsController: 1 Wh = 3600 J, rounded to three decimals. */
export function wattHoursFromJoules(joules: unknown): number {
  const j = Number(joules)
  if (!Number.isFinite(j)) return 0
  const wh = j / 3600
  return Math.round(wh * 1000) / 1000
}

/**
 * Converts Joules to Watt-hours without early rounding.
 *
 * <p>Use this when you want small per-event values to still display with variation
 * (e.g. ledger rows). For user-facing rollups where consistency with the backend rounding
 * is preferred, use {@link wattHoursFromJoules} instead.</p>
 */
export function wattHoursFromJoulesExact(joules: unknown): number {
  const j = Number(joules)
  if (!Number.isFinite(j)) return 0
  return j / 3600
}

/**
 * Formats small fractional equivalents (phone charges, streetlight hours) so tiny totals
 * (e.g. pedestrians) still show non-zero values instead of rounding away.
 */
export function formatEquivalent(value: unknown): string {
  const n = Number(value)
  if (!Number.isFinite(n)) return '—'
  if (n === 0) return '0'
  const a = Math.abs(n)
  let maximumFractionDigits = 2
  if (a < 0.01) maximumFractionDigits = 5
  else if (a < 1) maximumFractionDigits = 4
  else if (a < 10) maximumFractionDigits = 3
  return new Intl.NumberFormat('en-US', {
    maximumFractionDigits,
    minimumFractionDigits: 0,
  }).format(n)
}
