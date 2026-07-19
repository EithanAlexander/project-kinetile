/* eslint-disable react-hooks/set-state-in-effect -- city default & series visibility sync from query keys */
import { useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { toUserSafeError } from '../../api/client'
import type { TimeseriesRow } from '../../api/types/timeseries'
import {
  TIMESERIES_DAILY_BY_CITY_URL,
  TIMESERIES_DAILY_BY_LOCATION_URL,
  fetchTimeseriesRows,
} from '../../api/timeseries'
import ContextNotice from '../../components/ContextNotice'
import { useChartTheme } from '../../hooks/useChartTheme'
import { formatDecimal } from '../../utils/energyFormat'
import {
  MAX_TIMESERIES_QUERY_MONTHS,
  appendRangeToParams,
  clampTimeRangeToMaxCalendarMonths,
  earliestSelectableYmd,
  expandWideChartToTimeRange,
  formatYmdAsDMonY,
  isValidCalendarYmd,
  localYmdFromTime,
  pivotDailyWh,
  rangeFromPreset,
  timeRangeFromLocalYmd,
  todayLocalYmd,
  withTimeRangeQuery,
} from '../../utils/timeseriesQuery'
import CollapsibleChartSection from './CollapsibleChartSection'
import DailyWhLineChart from './DailyWhLineChart'
import SeriesToggleRow from './SeriesToggleRow'
import {
  brushResetKey,
  normalizeBrushIndices,
  type BrushIndices,
  type ChartDataRow,
  type DragPreview,
} from './chartRangeDrag'
import { LINE_COLORS, PRESET_IDS, TIME_PRESETS, type TimePresetId } from './timeseriesConstants'

function buildByLocationUrl(city: string, range: { since?: string; until?: string }): string {
  const params = appendRangeToParams(range)
  params.set('city', city)
  return `${TIMESERIES_DAILY_BY_LOCATION_URL}?${params.toString()}`
}

/** Stable fallback so hooks that depend on row arrays do not see a new `[]` every render. */
const EMPTY_ROWS: TimeseriesRow[] = []

function asTimeseriesRows(data: unknown): TimeseriesRow[] {
  return Array.isArray(data) ? (data as TimeseriesRow[]) : EMPTY_ROWS
}

function sameVisibilityMap(
  prev: Record<string, boolean | undefined>,
  next: Record<string, boolean | undefined>,
): boolean {
  const pk = Object.keys(prev)
  const nk = Object.keys(next)
  if (pk.length !== nk.length) return false
  for (const k of nk) {
    if (prev[k] !== next[k]) return false
  }
  return true
}

function stringField(row: TimeseriesRow | Record<string, unknown>, key: string): string {
  const v = row[key as keyof typeof row]
  if (typeof v === 'string') return v.trim()
  if (typeof v === 'number' && Number.isFinite(v)) return String(v)
  if (typeof v === 'boolean') return v ? 'true' : 'false'
  return ''
}

function renderCityOptions(cityOptions: string[]) {
  if (cityOptions.length === 0) {
    return (
      <option value="" disabled>
        No cities in range
      </option>
    )
  }
  return cityOptions.map((c) => (
    <option key={c} value={c}>
      {c}
    </option>
  ))
}

/**
 * Daily watt-hour trends from the physics engine (per city, per site, and network total).
 */
export default function TimeSeriesDashboard() {
  const chartTheme = useChartTheme()
  const [preset, setPreset] = useState<TimePresetId>('30d')
  const [customStart, setCustomStart] = useState('')
  const [customEnd, setCustomEnd] = useState('')
  const [cityForSites, setCityForSites] = useState('')
  const [cityVisibility, setCityVisibility] = useState<Record<string, boolean | undefined>>(() => ({}))
  const [locationVisibility, setLocationVisibility] = useState<Record<string, boolean | undefined>>(
    () => ({}),
  )
  const [sectionOpen, setSectionOpen] = useState({
    location: true,
    city: true,
    network: true,
  })
  /** `endIndex` uses a large sentinel until data exists; `normalizeBrushIndices` clamps to series length. */
  const [locationBrush, setLocationBrush] = useState<BrushIndices>({
    startIndex: 0,
    endIndex: Number.MAX_SAFE_INTEGER,
  })
  const [cityBrush, setCityBrush] = useState<BrushIndices>({
    startIndex: 0,
    endIndex: Number.MAX_SAFE_INTEGER,
  })
  const [networkBrush, setNetworkBrush] = useState<BrushIndices>({
    startIndex: 0,
    endIndex: Number.MAX_SAFE_INTEGER,
  })
  const [dragPreview, setDragPreview] = useState<DragPreview | null>(null)

  const datesOverride = Boolean(customStart?.trim() && customEnd?.trim())
  const hasStrayDateInput = Boolean(
    (customStart?.trim() && !customEnd?.trim()) || (!customStart?.trim() && customEnd?.trim()),
  )

  useEffect(() => {
    if (!PRESET_IDS.has(preset)) setPreset('30d')
  }, [preset])

  const todayYmd = todayLocalYmd()
  const earliestYmd = earliestSelectableYmd(MAX_TIMESERIES_QUERY_MONTHS)
  const earliestDayLabel = formatYmdAsDMonY(earliestYmd)
  const todayDayLabel = formatYmdAsDMonY(todayYmd)

  const startDateMax = useMemo(() => {
    const today = todayLocalYmd()
    const e = customEnd?.trim()
    if (e && isValidCalendarYmd(e) && e <= today) return e
    return today
  }, [customEnd])

  const endDateMin = useMemo(() => {
    const earliest = earliestSelectableYmd(MAX_TIMESERIES_QUERY_MONTHS)
    const s = customStart?.trim()
    if (s && isValidCalendarYmd(s) && s >= earliest) return s
    return earliest
  }, [customStart])

  const invalidCustomDateRange = useMemo(() => {
    if (!datesOverride) return false
    const earliest = earliestSelectableYmd(MAX_TIMESERIES_QUERY_MONTHS)
    const today = todayLocalYmd()
    const a = customStart.trim()
    const b = customEnd.trim()
    if (!isValidCalendarYmd(a) || !isValidCalendarYmd(b)) return true
    if (a < earliest || b < earliest) return true
    if (a > today || b > today) return true
    return false
  }, [datesOverride, customStart, customEnd])

  const rawRange = useMemo(() => {
    if (datesOverride) {
      const earliest = earliestSelectableYmd(MAX_TIMESERIES_QUERY_MONTHS)
      const today = todayLocalYmd()
      const a = customStart.trim()
      const b = customEnd.trim()
      if (
        isValidCalendarYmd(a) &&
        isValidCalendarYmd(b) &&
        a >= earliest &&
        b >= earliest &&
        a <= today &&
        b <= today
      ) {
        const custom = timeRangeFromLocalYmd(a, b)
        if (custom.since && custom.until) return custom
      }
      return rangeFromPreset(preset)
    }
    return rangeFromPreset(preset)
  }, [preset, customStart, customEnd, datesOverride])

  const { range, wasRangeClamped } = useMemo(() => {
    const clamped = clampTimeRangeToMaxCalendarMonths(rawRange, MAX_TIMESERIES_QUERY_MONTHS)
    const clampedFlag =
      rawRange.since !== clamped.since || rawRange.until !== clamped.until
    return { range: clamped, wasRangeClamped: clampedFlag }
  }, [rawRange])

  const cityQuery = useQuery({
    queryKey: ['timeseries', 'daily', 'byCity', range],
    queryFn: ({ signal }) =>
      fetchTimeseriesRows(withTimeRangeQuery(TIMESERIES_DAILY_BY_CITY_URL, range), signal),
    enabled: Boolean(range.since && range.until),
  })

  const siteQuery = useQuery({
    queryKey: ['timeseries', 'daily', 'byLocation', range, cityForSites],
    queryFn: ({ signal }) =>
      fetchTimeseriesRows(buildByLocationUrl(cityForSites.trim(), range), signal),
    enabled: Boolean(range.since && range.until) && Boolean(cityForSites.trim()),
  })

  const byCityRows = asTimeseriesRows(cityQuery.data)
  const byLocationRows = asTimeseriesRows(siteQuery.data)

  const loadingMain = Boolean(cityQuery.isFetching)
  const loadingSites = Boolean(siteQuery.isFetching)

  const errorMain = cityQuery.error
    ? toUserSafeError(cityQuery.error, 'Failed to load trends')
    : null

  const errorSites = siteQuery.error
    ? toUserSafeError(siteQuery.error, 'Failed to load locations')
    : null

  const { chartData: cityChartData, seriesKeys: citySeriesKeys } = useMemo(
    () => pivotDailyWh(byCityRows, (r) => stringField(r as TimeseriesRow, 'city')),
    [byCityRows],
  )
  const { chartData: locationChartData, seriesKeys: locationSeriesKeys } = useMemo(
    () => pivotDailyWh(byLocationRows, (r) => stringField(r as TimeseriesRow, 'location')),
    [byLocationRows],
  )

  const cityChartExpanded = useMemo(
    () => expandWideChartToTimeRange(cityChartData, citySeriesKeys, range) as ChartDataRow[],
    [cityChartData, citySeriesKeys, range],
  )

  const locationChartExpanded = useMemo(
    () => expandWideChartToTimeRange(locationChartData, locationSeriesKeys, range) as ChartDataRow[],
    [locationChartData, locationSeriesKeys, range],
  )

  const locationByBucket = useMemo(() => {
    const m = new Map<string, ChartDataRow>()
    for (const row of locationChartExpanded) {
      const iso = String(row.bucketStart ?? '')
      if (!iso) continue
      const ymd = localYmdFromTime(new Date(iso).getTime())
      m.set(ymd, row)
    }
    return m
  }, [locationChartExpanded])

  const cityOptions = useMemo(() => {
    const set = new Set<string>()
    for (const r of byCityRows) {
      const c = r.city
      if (c != null && String(c).trim() !== '') set.add(String(c))
    }
    return [...set].sort((a, b) => a.localeCompare(b))
  }, [byCityRows])

  useEffect(() => {
    if (cityOptions.length === 0) {
      setCityForSites('')
      return
    }
    setCityForSites((prev) => {
      if (prev && cityOptions.includes(prev)) return prev
      return cityOptions[0]
    })
  }, [cityOptions])

  useEffect(() => {
    setCityVisibility((prev) => {
      const next: Record<string, boolean | undefined> = {}
      for (const k of citySeriesKeys) {
        next[k] = k in prev ? prev[k] : true
      }
      if (sameVisibilityMap(prev, next)) return prev
      return next
    })
  }, [citySeriesKeys])

  useEffect(() => {
    setLocationVisibility((prev) => {
      const next: Record<string, boolean | undefined> = {}
      for (const k of locationSeriesKeys) {
        next[k] = prev[k] ?? true
      }
      if (sameVisibilityMap(prev, next)) return prev
      return next
    })
  }, [cityForSites, locationSeriesKeys])

  const visibleCityKeys = useMemo(
    () => citySeriesKeys.filter((k) => cityVisibility[k] !== false),
    [citySeriesKeys, cityVisibility],
  )

  const citySubsetActive = useMemo(
    () => citySeriesKeys.some((k) => cityVisibility[k] === false),
    [citySeriesKeys, cityVisibility],
  )

  const locationSubsetActive = useMemo(
    () => locationSeriesKeys.some((k) => locationVisibility[k] === false),
    [locationSeriesKeys, locationVisibility],
  )

  const seriesSubsetActive = citySubsetActive || locationSubsetActive

  const networkLocationAdjustmentsActive =
    locationSubsetActive && visibleCityKeys.includes(cityForSites.trim())

  const filteredNetworkChartData = useMemo((): ChartDataRow[] => {
    if (!range.since || !range.until) return []
    const siteCity = cityForSites.trim()
    const siteVisible = siteCity && visibleCityKeys.includes(siteCity)

    return cityChartExpanded.map((row) => {
      let total = 0
      for (const c of visibleCityKeys) {
        total += Number(row[c]) || 0
      }
      const iso = String(row.bucketStart ?? '')
      const ymd = iso ? localYmdFromTime(new Date(iso).getTime()) : ''
      if (siteVisible && ymd) {
        const locRow = locationByBucket.get(ymd)
        if (locRow) {
          for (const loc of locationSeriesKeys) {
            if (locationVisibility[loc] === false) {
              total -= Number(locRow[loc]) || 0
            }
          }
        }
      }
      return {
        bucketStart: row.bucketStart,
        label: row.label,
        totalWh: Math.max(0, total),
      }
    })
  }, [
    range.since,
    range.until,
    cityChartExpanded,
    visibleCityKeys,
    cityForSites,
    locationByBucket,
    locationSeriesKeys,
    locationVisibility,
  ])

  const locationBrushKey = brushResetKey(locationChartExpanded)
  const cityBrushKey = brushResetKey(cityChartExpanded)
  const networkBrushKey = brushResetKey(filteredNetworkChartData)

  useEffect(() => {
    const len = locationChartExpanded.length
    setLocationBrush({ startIndex: 0, endIndex: Math.max(0, len - 1) })
  }, [locationBrushKey, locationChartExpanded.length])

  useEffect(() => {
    const len = cityChartExpanded.length
    setCityBrush({ startIndex: 0, endIndex: Math.max(0, len - 1) })
  }, [cityBrushKey, cityChartExpanded.length])

  useEffect(() => {
    const len = filteredNetworkChartData.length
    setNetworkBrush({ startIndex: 0, endIndex: Math.max(0, len - 1) })
  }, [networkBrushKey, filteredNetworkChartData.length])

  const locationChartZoomed = useMemo(() => {
    const { startIndex, endIndex } = normalizeBrushIndices(
      locationBrush,
      locationChartExpanded.length,
    )
    return locationChartExpanded.slice(startIndex, endIndex + 1)
  }, [locationChartExpanded, locationBrush])

  const cityChartZoomed = useMemo(() => {
    const { startIndex, endIndex } = normalizeBrushIndices(cityBrush, cityChartExpanded.length)
    return cityChartExpanded.slice(startIndex, endIndex + 1)
  }, [cityChartExpanded, cityBrush])

  const networkChartZoomed = useMemo(() => {
    const { startIndex, endIndex } = normalizeBrushIndices(
      networkBrush,
      filteredNetworkChartData.length,
    )
    return filteredNetworkChartData.slice(startIndex, endIndex + 1)
  }, [filteredNetworkChartData, networkBrush])

  const locationLines = useMemo(
    () =>
      locationSeriesKeys.map((key, i) => ({
        dataKey: key,
        name: key,
        stroke: LINE_COLORS[i % LINE_COLORS.length],
        hidden: locationVisibility[key] === false,
      })),
    [locationSeriesKeys, locationVisibility],
  )

  const cityLines = useMemo(
    () =>
      citySeriesKeys.map((key, i) => ({
        dataKey: key,
        name: key,
        stroke: LINE_COLORS[i % LINE_COLORS.length],
        hidden: cityVisibility[key] === false,
      })),
    [citySeriesKeys, cityVisibility],
  )

  const selectAllCities = () => {
    setCityVisibility((prev) => {
      const next = { ...prev }
      for (const k of citySeriesKeys) next[k] = true
      return next
    })
  }

  const deselectAllCities = () => {
    setCityVisibility((prev) => {
      const next = { ...prev }
      for (const k of citySeriesKeys) next[k] = false
      return next
    })
  }

  const selectAllLocations = () => {
    setLocationVisibility((prev) => {
      const next = { ...prev }
      for (const k of locationSeriesKeys) next[k] = true
      return next
    })
  }

  const rangeReady = Boolean(range.since && range.until)

  const networkSubsetNotice = useMemo(() => {
    if (citySubsetActive && networkLocationAdjustmentsActive) {
      return `This line sums visible cities only. Hidden locations in “${cityForSites || '—'}” are subtracted from that city’s share.`
    }
    if (citySubsetActive) {
      return 'This line sums visible cities only; unchecked cities are excluded.'
    }
    if (networkLocationAdjustmentsActive) {
      return `Hidden locations in “${cityForSites || '—'}” are subtracted from that city’s share in the total.`
    }
    return null
  }, [citySubsetActive, networkLocationAdjustmentsActive, cityForSites])

  return (
    <div className="rgf-ts space-y-8">
      <section className="rgf-ts-intro">
        <h2>Energy over time</h2>
        <p>
          Daily generated energy (watt-hours per calendar day) from stored tile compressions.
          Buckets follow the database date of each event; use filters to compare cities, sites
          within a city, and the whole network.
        </p>

        <div className="mt-5 grid gap-4 lg:grid-cols-2 lg:items-end">
          <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_auto_auto] sm:items-end">
            <div className="min-w-0">
              <label htmlFor="ts-preset" className="rgf-label">
                Time period
              </label>
              <select
                id="ts-preset"
                className="rgf-select mt-1 w-full max-w-none"
                value={preset}
                onChange={(e) => {
                  setPreset(e.target.value as TimePresetId)
                  setCustomStart('')
                  setCustomEnd('')
                }}
              >
                {TIME_PRESETS.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="ts-start" className="rgf-label">
                Start date
              </label>
              <input
                id="ts-start"
                type="date"
                min={earliestYmd}
                max={startDateMax}
                className="rgf-input mt-1"
                value={customStart}
                onChange={(e) => setCustomStart(e.target.value)}
                aria-describedby="ts-date-format-hint"
              />
            </div>
            <div>
              <label htmlFor="ts-end" className="rgf-label">
                End date
              </label>
              <input
                id="ts-end"
                type="date"
                min={endDateMin}
                max={todayYmd}
                className="rgf-input mt-1"
                value={customEnd}
                onChange={(e) => setCustomEnd(e.target.value)}
                aria-describedby="ts-date-format-hint"
              />
            </div>
          </div>

          <div className="flex min-w-0 flex-col gap-2 lg:flex-row lg:items-end">
            <div className="min-w-0 flex-1">
              <label htmlFor="ts-city-sites" className="rgf-label">
                City for site breakdown
              </label>
              <select
                id="ts-city-sites"
                className="rgf-select mt-1 w-full max-w-none disabled:cursor-not-allowed disabled:opacity-45"
                value={cityForSites}
                onChange={(e) => setCityForSites(e.target.value)}
                disabled={cityOptions.length === 0}
              >
                {renderCityOptions(cityOptions)}
              </select>
            </div>
            <button
              type="button"
              className="rgf-btn-sm shrink-0 px-2 py-1"
              disabled={preset === '30d' && !datesOverride && !hasStrayDateInput}
              onClick={() => {
                setCustomStart('')
                setCustomEnd('')
                setPreset('30d')
              }}
            >
              Reset Time Period
            </button>
          </div>
        </div>

        <p id="ts-date-format-hint" className="rgf-hint mt-3 text-xs">
          Use the calendar pickers for start and end (your browser&apos;s native date UI). Chart axes
          below label each day like <span className="font-mono">20-Sep-2025</span>. Allowed window:{' '}
          <span className="font-mono">{earliestDayLabel}</span> →{' '}
          <span className="font-mono">{todayDayLabel}</span>.
        </p>

        <div className="mt-3 grid gap-2 text-sm leading-snug text-[var(--rgf-text-subtle)] lg:grid-cols-2 lg:gap-4">
          <p className="rgf-hint">
            Presets use whole local calendar days through today. If both start and end are chosen in
            the calendars, they override the preset. Queries are limited to{' '}
            {MAX_TIMESERIES_QUERY_MONTHS} months.
          </p>
          <p className="rgf-hint">
            Charts always span the full selected window (zero Wh where there is no data). Unchecked
            cities or locations are excluded from the network total for the selected city.
          </p>
          <p className="rgf-hint lg:col-span-2">
            On each line chart, hold the left mouse button, drag across the plot, and release to zoom
            to that date range. Use “Show full range” to reset. Touch: drag on the chart the same way.
          </p>
        </div>

        {wasRangeClamped && rangeReady && (
          <div className="mt-4">
            <ContextNotice title="Date range adjusted">
              The requested window was longer than {MAX_TIMESERIES_QUERY_MONTHS} months; the start
              date was moved forward to fit the limit. All charts below use the adjusted range.
            </ContextNotice>
          </div>
        )}
      </section>

      {invalidCustomDateRange && (
        <div
          className="rgf-notice rgf-notice--error px-4 py-3"
          role="alert"
        >
          <p className="rgf-notice-title">Custom dates could not be read</p>
          <p className="rgf-notice-body mt-1">
            Pick dates between <span className="font-mono">{earliestDayLabel}</span> and{' '}
            <span className="font-mono">{todayDayLabel}</span> using the calendars. Charts below use
            your time period preset until both dates are valid.
          </p>
        </div>
      )}

      {errorMain && (
        <p className="rgf-status rgf-status--panel rgf-status--error" role="alert">
          {errorMain}
        </p>
      )}

      {loadingMain && rangeReady && (
        <p className="rgf-status rgf-status--panel rgf-status--loading">
          Loading daily aggregates…
        </p>
      )}

      {!loadingMain && rangeReady && !errorMain && (
        <>
          {errorSites && (
            <p className="rgf-status rgf-status--panel rgf-status--error" role="alert">
              {errorSites}
            </p>
          )}

          <CollapsibleChartSection
            title="Per location in selected city"
            description={`Chokepoints in “${cityForSites || '—'}” (daily Wh). Toggle lines below.`}
            open={sectionOpen.location}
            onToggle={(o) => setSectionOpen((s) => ({ ...s, location: o }))}
            empty={!cityForSites}
            emptyMessage="Choose a city above to plot locations."
          >
            {locationSeriesKeys.length > 0 && (
              <SeriesToggleRow
                keys={locationSeriesKeys}
                visibility={locationVisibility}
                onToggle={(key) =>
                  setLocationVisibility((prev) => ({
                    ...prev,
                    [key]: prev[key] === false,
                  }))
                }
                onSelectAll={selectAllLocations}
              />
            )}
            {locationSubsetActive && (
              <ContextNotice title="Partial view">
                Some locations are hidden. Only checked locations appear in this chart.
              </ContextNotice>
            )}
            {loadingSites && <p className="rgf-hint mb-3">Loading locations…</p>}
            <DailyWhLineChart
              chartId="location"
              ariaLabel="Watt-hours per day by location"
              plotHeightClass="h-[420px]"
              fullRows={locationChartExpanded}
              zoomedData={locationChartZoomed}
              brush={locationBrush}
              setBrush={setLocationBrush}
              dragPreview={dragPreview}
              setDragPreview={setDragPreview}
              lines={locationLines}
            />
          </CollapsibleChartSection>

          <CollapsibleChartSection
            title="Per city"
            description="Each line is one city’s daily watt-hours in the selected period. Toggle lines below."
            open={sectionOpen.city}
            onToggle={(o) => setSectionOpen((s) => ({ ...s, city: o }))}
            empty={false}
            emptyMessage=""
          >
            {citySeriesKeys.length > 0 && (
              <SeriesToggleRow
                keys={citySeriesKeys}
                visibility={cityVisibility}
                onToggle={(key) =>
                  setCityVisibility((prev) => ({
                    ...prev,
                    [key]: prev[key] === false,
                  }))
                }
                onSelectAll={selectAllCities}
                onDeselectAll={deselectAllCities}
                selectAllLabel="Show all cities"
                deselectAllLabel="Hide all cities"
              />
            )}
            {citySubsetActive && (
              <ContextNotice title="Partial view">
                Some cities are hidden. Only checked cities appear in this chart.
              </ContextNotice>
            )}
            <DailyWhLineChart
              chartId="city"
              ariaLabel="Watt-hours per day by city"
              plotHeightClass="h-[400px]"
              fullRows={cityChartExpanded}
              zoomedData={cityChartZoomed}
              brush={cityBrush}
              setBrush={setCityBrush}
              dragPreview={dragPreview}
              setDragPreview={setDragPreview}
              lines={cityLines}
            />
          </CollapsibleChartSection>

          <CollapsibleChartSection
            title="All cities (network total)"
            description="Daily sum of watt-hours for visible cities; hidden locations in the selected city are subtracted from that city’s share."
            open={sectionOpen.network}
            onToggle={(o) => setSectionOpen((s) => ({ ...s, network: o }))}
            empty={false}
            emptyMessage=""
          >
            {networkSubsetNotice && (
              <ContextNotice title="Filtered total">{networkSubsetNotice}</ContextNotice>
            )}
            <DailyWhLineChart
              chartId="network"
              ariaLabel="Filtered network watt-hours per day"
              plotHeightClass="h-[360px]"
              fullRows={filteredNetworkChartData}
              zoomedData={networkChartZoomed}
              brush={networkBrush}
              setBrush={setNetworkBrush}
              dragPreview={dragPreview}
              setDragPreview={setDragPreview}
              lines={[
                {
                  dataKey: 'totalWh',
                  name: seriesSubsetActive ? 'Filtered total Wh' : 'Total Wh',
                  stroke: chartTheme.accent,
                  dot: { r: 2 },
                },
              ]}
              tooltipFormatter={(value) => [
                `${formatDecimal(value, 3)} Wh`,
                seriesSubsetActive ? 'Filtered total' : 'Total',
              ]}
            />
          </CollapsibleChartSection>
        </>
      )}
    </div>
  )
}
