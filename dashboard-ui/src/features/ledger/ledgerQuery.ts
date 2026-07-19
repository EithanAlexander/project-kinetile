import { LEDGER_API_BASE } from '../../api/ledger'
import type { LedgerPageParsed, LedgerRow } from '../../api/types/ledger'
import { normalizeLedgerSort, type TimePresetId } from './ledgerConstants'

/** Max length for free-text ledger filters, sent to the API to bound query size. */
export const MAX_LEDGER_FILTER_LENGTH = 128

/** UI-controlled ledger query inputs (pagination, sort, time preset, and filters). */
export interface LedgerQueryState {
  page: number
  pageSize: number
  sort: string
  timePreset: TimePresetId
  locationContains: string
  eventIdPrefix: string
  minEnergyJoules: string
  maxEnergyJoules: string
  minPowerWatts: string
  maxPowerWatts: string
  minImpactMultiplier: string
  maxImpactMultiplier: string
  activationOnly: boolean
}

/**
 * Converts a relative time preset (e.g. `7d`) into an ISO `since` instant.
 *
 * `all` maps to the platform default ledger window (last 24 months) to stay aligned
 * with API time-range expectations and UI wording.
 */
export function sinceInstantForPreset(presetId: string): string | null {
  if (presetId === 'all') return new Date(Date.now() - 24 * 31 * 86_400_000).toISOString()
  const days = { '1d': 1, '2d': 2, '7d': 7, '30d': 30 }[presetId]
  if (days == null) return null
  return new Date(Date.now() - days * 86_400_000).toISOString()
}

/** Trims whitespace and caps the string at `maxLen` characters. */
function trimToMax(value: string, maxLen: number): string {
  const t = value.trim()
  return t.length > maxLen ? t.slice(0, maxLen) : t
}

/** Sets a query param only when the input parses to a finite number; ignores blanks/invalid input. */
function appendFiniteNumberParam(params: URLSearchParams, key: string, inputStr: string): void {
  const t = inputStr.trim()
  if (t === '') return
  const n = Number(t)
  if (Number.isFinite(n)) params.set(key, String(n))
}

function parseFinite(inputStr: string): number | null {
  const t = inputStr.trim()
  if (t === '') return null
  const n = Number(t)
  return Number.isFinite(n) ? n : null
}

/**
 * UI convenience conversion for a "watts" filter.
 * Ledger rows represent single compression events, so we treat power over a 1-second event window.
 */
const POWER_FILTER_EVENT_SECONDS = 1

/** Builds the ledger API URL from query state, validating sort and sanitizing/trimming filters. */
export function buildLedgerQueryUrl(state: LedgerQueryState): string {
  const params = new URLSearchParams()
  params.set('page', String(state.page))
  params.set('size', String(state.pageSize))

  params.set('sort', normalizeLedgerSort(state.sort))

  const since = sinceInstantForPreset(state.timePreset)
  if (since) params.set('since', since)

  const loc = trimToMax(state.locationContains, MAX_LEDGER_FILTER_LENGTH)
  if (loc) params.set('locationContains', loc)

  const prefix = trimToMax(state.eventIdPrefix, MAX_LEDGER_FILTER_LENGTH)
  if (prefix) params.set('eventIdPrefix', prefix)

  const minEnergyJoules = parseFinite(state.minEnergyJoules)
  const maxEnergyJoules = parseFinite(state.maxEnergyJoules)
  const minPowerWatts = parseFinite(state.minPowerWatts)
  const maxPowerWatts = parseFinite(state.maxPowerWatts)

  const minFromWatts = minPowerWatts == null ? null : minPowerWatts * POWER_FILTER_EVENT_SECONDS
  const maxFromWatts = maxPowerWatts == null ? null : maxPowerWatts * POWER_FILTER_EVENT_SECONDS

  const finalMinEnergy =
    minEnergyJoules != null && minFromWatts != null
      ? Math.max(minEnergyJoules, minFromWatts)
      : (minEnergyJoules ?? minFromWatts)
  const finalMaxEnergy =
    maxEnergyJoules != null && maxFromWatts != null
      ? Math.min(maxEnergyJoules, maxFromWatts)
      : (maxEnergyJoules ?? maxFromWatts)

  if (finalMinEnergy != null) params.set('minEnergyJoules', String(finalMinEnergy))
  if (finalMaxEnergy != null) params.set('maxEnergyJoules', String(finalMaxEnergy))
  appendFiniteNumberParam(params, 'minImpactMultiplier', state.minImpactMultiplier)
  appendFiniteNumberParam(params, 'maxImpactMultiplier', state.maxImpactMultiplier)
  if (state.activationOnly) params.set('activationOnly', 'true')

  return `${LEDGER_API_BASE}?${params.toString()}`
}

/** Normalizes a raw page payload; requests a clamp to the last page when the page is out of range. */
export function parseLedgerPagePayload(data: unknown): LedgerPageParsed {
  const o = data as Record<string, unknown> | null
  const totalPages = typeof o?.totalPages === 'number' ? o.totalPages : 0
  const serverPage = typeof o?.page === 'number' ? o.page : 0
  if (totalPages > 0 && serverPage >= totalPages) {
    return { clampToPage: totalPages - 1, content: [], totalElements: 0, totalPages: 0 }
  }
  const rawContent = o?.content
  const content = Array.isArray(rawContent) ? (rawContent as LedgerRow[]) : []
  const totalElements = typeof o?.totalElements === 'number' ? o.totalElements : 0
  return { clampToPage: null, content, totalElements, totalPages }
}

const EMPTY_LEDGER_PAGE: LedgerPageParsed = {
  clampToPage: null,
  content: [],
  totalElements: 0,
  totalPages: 0,
}

/**
 * Resolves which ledger page to display during fetch, error, or out-of-range clamp.
 */
export function resolveLedgerDisplay(
  data: LedgerPageParsed | undefined,
  error: unknown,
  lastGood: LedgerPageParsed | null,
): LedgerPageParsed {
  if (error) return EMPTY_LEDGER_PAGE
  if (!data) return lastGood ?? EMPTY_LEDGER_PAGE
  if (data.clampToPage != null) return lastGood ?? EMPTY_LEDGER_PAGE
  return data
}
