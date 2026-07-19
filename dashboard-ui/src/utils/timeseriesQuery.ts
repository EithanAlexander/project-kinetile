export type TimePresetId = '7d' | '30d' | '6m' | '365d'

/** Maximum calendar span for timeseries API queries (limits payload size). */
export const MAX_TIMESERIES_QUERY_MONTHS = 13

export interface TimeRange {
  since: string | null
  until: string | null
}

const YMD_RE = /^\d{4}-\d{2}-\d{2}$/

/**
 * True if {@link ymd} is a real calendar day in the local timezone (`YYYY-MM-DD`).
 * Rejects malformed strings and impossible dates (e.g. `2026-02-31`).
 */
export function isValidCalendarYmd(ymd: string): boolean {
  const s = ymd.trim()
  if (!YMD_RE.test(s)) return false
  const [yy, mm, dd] = s.split('-').map(Number)
  const d = new Date(yy, mm - 1, dd)
  return d.getFullYear() === yy && d.getMonth() === mm - 1 && d.getDate() === dd
}

/** English abbreviated month names for labels (e.g. {@link formatChartDayLabel}). */
const MONTH_SHORT_EN = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
] as const

/**
 * Display a canonical `YYYY-MM-DD` as `DD-MMM-YYYY` (e.g. `20-Sep-2025`). Day is zero-padded.
 */
export function formatYmdAsDMonY(ymd: string): string {
  const s = ymd.trim()
  if (!isValidCalendarYmd(s)) return s
  const [yy, mm, dd] = s.split('-').map(Number)
  const d = new Date(yy, mm - 1, dd)
  const day = String(d.getDate()).padStart(2, '0')
  const mon = MONTH_SHORT_EN[d.getMonth()]
  return `${day}-${mon}-${d.getFullYear()}`
}

/**
 * Display a canonical {@link isValidCalendarYmd} `YYYY-MM-DD` string as `DD-MM-YYYY` (zero-padded).
 */
export function formatYmdAsDmy(ymd: string): string {
  const s = ymd.trim()
  if (!isValidCalendarYmd(s)) return s
  const [y, m, d] = s.split('-')
  return `${d}-${m}-${y}`
}

/**
 * Parse user-entered `DD-MM-YYYY` (day and month may be one or two digits) to canonical `YYYY-MM-DD`, or `null`.
 */
export function parseDmyToYmd(dmy: string): string | null {
  const s = dmy.trim()
  const match = /^(\d{1,2})-(\d{1,2})-(\d{4})$/.exec(s)
  if (!match) return null
  const dd = match[1].padStart(2, '0')
  const mm = match[2].padStart(2, '0')
  const yyyy = match[3]
  const ymd = `${yyyy}-${mm}-${dd}`
  return isValidCalendarYmd(ymd) ? ymd : null
}

function localMidnightToIso(yy: number, mm: number, dd: number): string {
  return new Date(yy, mm - 1, dd).toISOString()
}

function localEndOfDayToIso(yy: number, mm: number, dd: number): string {
  return new Date(yy, mm - 1, dd, 23, 59, 59, 999).toISOString()
}

/**
 * @param ymd Calendar date in local timezone (`YYYY-MM-DD`).
 * @returns ISO-8601 instant at local midnight.
 */
export function startOfLocalDayIso(ymd: string): string {
  const s = ymd.trim()
  if (isValidCalendarYmd(s)) {
    const [yy, mm, dd] = s.split('-').map(Number)
    return localMidnightToIso(yy, mm, dd)
  }
  console.warn('[timeseriesQuery] startOfLocalDayIso: invalid YMD, using today', { ymd })
  const t = localYmdFromTime(Date.now())
  const [yy, mm, dd] = t.split('-').map(Number)
  return localMidnightToIso(yy, mm, dd)
}

/**
 * @param ymd Calendar date in local timezone (`YYYY-MM-DD`).
 * @returns ISO-8601 instant at local end-of-day.
 */
export function endOfLocalDayIso(ymd: string): string {
  const s = ymd.trim()
  if (isValidCalendarYmd(s)) {
    const [yy, mm, dd] = s.split('-').map(Number)
    return localEndOfDayToIso(yy, mm, dd)
  }
  console.warn('[timeseriesQuery] endOfLocalDayIso: invalid YMD, using today', { ymd })
  const t = localYmdFromTime(Date.now())
  const [yy, mm, dd] = t.split('-').map(Number)
  return localEndOfDayToIso(yy, mm, dd)
}

/** Local calendar date as `YYYY-MM-DD` from an instant. */
export function localYmdFromTime(ms: number): string {
  if (!Number.isFinite(ms)) {
    return localYmdFromTime(Date.now())
  }
  const d = new Date(ms)
  if (Number.isNaN(d.getTime())) {
    return localYmdFromTime(Date.now())
  }
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Adds signed whole days to a local calendar `YYYY-MM-DD`. */
export function addLocalCalendarDaysYmd(ymd: string, deltaDays: number): string {
  const [yy, mm, dd] = ymd.split('-').map(Number)
  const d = new Date(yy, mm - 1, dd)
  d.setDate(d.getDate() + deltaDays)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Adds calendar months to a local `YYYY-MM-DD` (month overflow handled by `Date`). */
export function addMonthsLocalYmd(ymd: string, deltaMonths: number): string {
  const [yy, mm, dd] = ymd.split('-').map(Number)
  const d = new Date(yy, mm - 1, dd)
  d.setMonth(d.getMonth() + deltaMonths)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * Last {@link dayCount} local calendar days through “today” (inclusive), bounded to local midnights.
 */
export function calendarRollingRangeDays(dayCount: number, nowMs: number): TimeRange {
  const endYmd = localYmdFromTime(nowMs)
  const startYmd = addLocalCalendarDaysYmd(endYmd, -(dayCount - 1))
  return {
    since: startOfLocalDayIso(startYmd),
    until: endOfLocalDayIso(endYmd),
  }
}

/**
 * Inclusive local-day bounds from two `YYYY-MM-DD` strings (start and end).
 */
export function timeRangeFromLocalYmd(startYmd: string, endYmd: string): TimeRange {
  let a = startYmd.trim()
  let b = endYmd.trim()
  if (!isValidCalendarYmd(a) || !isValidCalendarYmd(b)) {
    return { since: null, until: null }
  }
  if (a > b) {
    const t = a
    a = b
    b = t
  }
  return {
    since: startOfLocalDayIso(a),
    until: endOfLocalDayIso(b),
  }
}

/**
 * If the span from {@link range.since} to {@link range.until} exceeds {@link maxMonths} calendar
 * months, moves {@link since} forward to the earliest allowed start (still local calendar days).
 */
export function clampTimeRangeToMaxCalendarMonths(
  range: TimeRange,
  maxMonths: number,
): TimeRange {
  if (!range.since || !range.until) {
    return range
  }
  const endMs = new Date(range.until).getTime()
  const startMs = new Date(range.since).getTime()
  if (!Number.isFinite(endMs) || !Number.isFinite(startMs)) {
    return { since: null, until: null }
  }
  const endYmd = localYmdFromTime(endMs)
  const startYmdRaw = localYmdFromTime(startMs)
  if (!isValidCalendarYmd(endYmd) || !isValidCalendarYmd(startYmdRaw)) {
    return { since: null, until: null }
  }
  const limitStartYmd = addMonthsLocalYmd(endYmd, -maxMonths)
  const startYmd = startYmdRaw < limitStartYmd ? limitStartYmd : startYmdRaw
  return {
    since: startOfLocalDayIso(startYmd),
    until: endOfLocalDayIso(endYmd),
  }
}

/** Earliest local calendar date allowed in date pickers (inclusive). */
export function earliestSelectableYmd(maxMonths: number, nowMs = Date.now()): string {
  return addMonthsLocalYmd(localYmdFromTime(nowMs), -maxMonths)
}

export function todayLocalYmd(nowMs = Date.now()): string {
  return localYmdFromTime(nowMs)
}

/** Every local calendar day from {@link sinceIso} through {@link untilIso} (inclusive). */
export function listLocalYmdsInclusive(sinceIso: string, untilIso: string): string[] {
  const t0 = new Date(sinceIso).getTime()
  const t1 = new Date(untilIso).getTime()
  if (!Number.isFinite(t0) || !Number.isFinite(t1) || t0 > t1) {
    return []
  }
  const startYmd = localYmdFromTime(t0)
  const endYmd = localYmdFromTime(t1)
  if (!isValidCalendarYmd(startYmd) || !isValidCalendarYmd(endYmd)) {
    return []
  }
  const out: string[] = []
  let ymd = startYmd
  while (ymd <= endYmd) {
    out.push(ymd)
    ymd = addLocalCalendarDaysYmd(ymd, 1)
  }
  return out
}

/**
 * Ensures one row per day in {@link range} (zeros for missing days) so charts span the full window.
 */
export function expandWideChartToTimeRange(
  chartData: Array<Record<string, string | number>>,
  seriesKeys: string[],
  range: TimeRange,
): Array<Record<string, string | number>> {
  if (!range.since || !range.until) {
    return chartData
  }
  const byYmd = new Map<string, Record<string, string | number>>()
  for (const row of chartData) {
    const iso = String(row.bucketStart ?? '')
    if (!iso) continue
    const ymd = localYmdFromTime(new Date(iso).getTime())
    byYmd.set(ymd, row)
  }
  return listLocalYmdsInclusive(range.since, range.until).map((ymd) => {
    const existing = byYmd.get(ymd)
    const bucketStart = startOfLocalDayIso(ymd)
    const label = formatChartDayLabel(bucketStart)
    if (existing) {
      const merged: Record<string, string | number> = { ...existing, bucketStart, label }
      for (const k of seriesKeys) {
        if (!(k in merged) || merged[k] === undefined) merged[k] = 0
      }
      return merged
    }
    const obj: Record<string, string | number> = { bucketStart, label }
    for (const k of seriesKeys) obj[k] = 0
    return obj
  })
}

/**
 * Preset-only range (no custom dates): past week, month, six months, or year through today.
 */
export function rangeFromPreset(presetId: string, nowMs = Date.now()): TimeRange {
  const endYmd = localYmdFromTime(nowMs)
  if (presetId === '7d') {
    return calendarRollingRangeDays(7, nowMs)
  }
  if (presetId === '30d') {
    return calendarRollingRangeDays(30, nowMs)
  }
  if (presetId === '6m') {
    const startYmd = addMonthsLocalYmd(endYmd, -6)
    return {
      since: startOfLocalDayIso(startYmd),
      until: endOfLocalDayIso(endYmd),
    }
  }
  if (presetId === '365d') {
    return calendarRollingRangeDays(365, nowMs)
  }
  return calendarRollingRangeDays(30, nowMs)
}

export function appendRangeToParams(range: TimeRange): URLSearchParams {
  const params = new URLSearchParams()
  if (range.since) params.set('since', range.since)
  if (range.until) params.set('until', range.until)
  return params
}

export function withTimeRangeQuery(path: string, range: TimeRange): string {
  const params = appendRangeToParams(range)
  const qs = params.toString()
  if (!qs) return path
  const sep = path.includes('?') ? '&' : '?'
  return `${path}${sep}${qs}`
}

/** Chart axis / table: local calendar day as `DD-MMM-YYYY` (e.g. `20-Sep-2025`). */
export function formatChartDayLabel(bucketStartIso: string): string {
  const d = new Date(bucketStartIso)
  if (Number.isNaN(d.getTime())) return String(bucketStartIso)
  const day = String(d.getDate()).padStart(2, '0')
  const mon = MONTH_SHORT_EN[d.getMonth()]
  return `${day}-${mon}-${d.getFullYear()}`
}

export interface TotalSeriesPoint {
  bucketStart: string
  label: string
  totalWh: number
}

/** One line per calendar day (network total endpoint). */
export function totalSeriesFromRows(rows: unknown): TotalSeriesPoint[] {
  if (!Array.isArray(rows)) return []
  return rows
    .map((r) => {
      const rec = r as Record<string, unknown>
      const bucketStart = String(rec.bucketStart ?? '')
      const totalWh = Number(rec.totalWattHours)
      return {
        bucketStart,
        label: formatChartDayLabel(bucketStart),
        totalWh: Number.isFinite(totalWh) ? totalWh : 0,
      }
    })
    .filter((r) => r.bucketStart !== '')
    .sort((a, b) => a.bucketStart.localeCompare(b.bucketStart))
}

export interface PivotResult {
  chartData: Array<Record<string, string | number>>
  seriesKeys: string[]
}

/** Pivots bucketed rows into wide chart rows (one column per series id). */
export function pivotDailyWh(
  rows: unknown,
  seriesKeyFn: (row: Record<string, unknown>) => string,
): PivotResult {
  if (!Array.isArray(rows)) {
    return { chartData: [], seriesKeys: [] }
  }
  const dayKeys = [...new Set(rows.map((r) => String((r as Record<string, unknown>).bucketStart ?? '')))]
    .filter(Boolean)
    .sort()
  const seriesKeys = [...new Set(rows.map((r) => seriesKeyFn(r as Record<string, unknown>)).filter((k) => k !== ''))].sort()
  const map = new Map<string, Record<string, string | number>>()
  for (const day of dayKeys) {
    const obj: Record<string, string | number> = { bucketStart: day, label: formatChartDayLabel(day) }
    for (const k of seriesKeys) {
      obj[k] = 0
    }
    map.set(day, obj)
  }
  for (const r of rows) {
    const rec = r as Record<string, unknown>
    const day = String(rec.bucketStart ?? '')
    const key = seriesKeyFn(rec)
    if (!map.has(day) || key === '') continue
    const wh = Number(rec.totalWattHours)
    const rowObj = map.get(day)!
    rowObj[key] = (Number(rowObj[key]) || 0) + (Number.isFinite(wh) ? wh : 0)
  }
  return { chartData: dayKeys.map((d) => map.get(d)!), seriesKeys }
}
