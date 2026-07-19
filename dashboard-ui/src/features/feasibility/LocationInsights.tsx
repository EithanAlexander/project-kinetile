/* eslint-disable react-hooks/set-state-in-effect -- device picker follows loaded catalog rows */
import { useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import type { EdgeDevice } from '../../api/types/devices'
import type { LocationEnergyRow } from '../../api/types/locations'
import { toUserSafeError } from '../../api/client'
import { fetchDevices } from '../../api/devices'
import { fetchCitiesCatalog } from '../../api/infrastructure'
import { fetchLocations } from '../../api/locations'
import { formatDecimal, formatEquivalent } from '../../utils/energyFormat'
import {
  aggregateRowsByCity,
  asNonNegativeInt,
  displaySortKey,
  feasibilityBadge,
  feasibilityPercent,
  formatActivationRate,
  hasCompressionMetrics,
  joulesValue,
  locationSortKey,
  normalizeCity,
  normalizeSite,
  parseSiteOptionKey,
  wattHoursValue,
} from './locationAggregates'
import { useLocationScope } from './useLocationScope'

const SORT_OPTIONS = [
  { id: 'wh_desc', label: 'All-time Wh (high → low)' },
  { id: 'wh_asc', label: 'All-time Wh (low → high)' },
  { id: 'joules_desc', label: 'All-time Joules (high → low)' },
  { id: 'joules_asc', label: 'All-time Joules (low → high)' },
  { id: 'name_asc', label: 'Name (A → Z)' },
] as const

/** All-time feasibility vs. edge-device load: cumulative harvest per chokepoint or city roll-up. */
export default function LocationInsights() {
  const {
    data: rows = [],
    isLoading: loading,
    error: locationsError,
  } = useQuery({
    queryKey: ['energy', 'locations'],
    queryFn: ({ signal }) => fetchLocations(signal),
  })

  const {
    data: devices = [],
    isLoading: devicesLoading,
    error: devicesErrorObj,
  } = useQuery({
    queryKey: ['devices', 'catalog'],
    queryFn: ({ signal }) => fetchDevices(signal),
  })

  const { data: catalogCities = [] } = useQuery({
    queryKey: ['infrastructure', 'cities'],
    queryFn: ({ signal }) => fetchCitiesCatalog(signal),
  })

  const catalogCityNames = useMemo(
    () => catalogCities.map((c) => c.name).filter(Boolean),
    [catalogCities],
  )

  const {
    scopeCity,
    setScopeCity,
    scopeSiteKey,
    setScopeSiteKey,
    cityOptions,
    siteOptions,
  } = useLocationScope(rows, catalogCityNames)

  const error = locationsError ? toUserSafeError(locationsError, 'Failed to load data') : null
  const devicesError = devicesErrorObj
    ? toUserSafeError(devicesErrorObj, 'Failed to load devices')
    : null

  const [selectedDevice, setSelectedDevice] = useState<EdgeDevice | null>(null)
  const [locationFilter, setLocationFilter] = useState('')
  const [sortBy, setSortBy] = useState<string>('wh_desc')
  const [groupBy, setGroupBy] = useState<'site' | 'city'>('site')

  useEffect(() => {
    if (devices.length === 0) {
      setSelectedDevice(null)
      return
    }
    setSelectedDevice((prev) => {
      if (prev && devices.some((d) => d.id === prev.id)) return prev
      return devices[0] ?? null
    })
  }, [devices])

  const textFiltered = useMemo(() => {
    const q = locationFilter.trim().toLowerCase()
    if (!q) return rows
    return rows.filter((r) => locationSortKey(r).includes(q))
  }, [rows, locationFilter])

  const scopeFiltered = useMemo(() => {
    let out = textFiltered
    if (scopeCity) {
      out = out.filter((r) => normalizeCity(r) === scopeCity)
    }
    if (groupBy === 'site' && scopeSiteKey) {
      const { city, site } = parseSiteOptionKey(scopeSiteKey)
      out = out.filter((r) => normalizeCity(r) === city && normalizeSite(r) === site)
    }
    return out
  }, [textFiltered, scopeCity, scopeSiteKey, groupBy])

  const displayRows = useMemo((): LocationEnergyRow[] => {
    if (groupBy === 'city') {
      return aggregateRowsByCity(scopeFiltered)
    }
    return scopeFiltered
  }, [scopeFiltered, groupBy])

  const sorted = useMemo(() => {
    const copy = [...displayRows]
    if (sortBy === 'wh_desc') {
      copy.sort((a, b) => wattHoursValue(b) - wattHoursValue(a))
    } else if (sortBy === 'wh_asc') {
      copy.sort((a, b) => wattHoursValue(a) - wattHoursValue(b))
    } else if (sortBy === 'joules_desc') {
      copy.sort((a, b) => joulesValue(b) - joulesValue(a))
    } else if (sortBy === 'joules_asc') {
      copy.sort((a, b) => joulesValue(a) - joulesValue(b))
    } else {
      copy.sort((a, b) =>
        displaySortKey(a, groupBy).localeCompare(displaySortKey(b, groupBy), undefined, {
          sensitivity: 'base',
        }),
      )
    }
    return copy
  }, [displayRows, sortBy, groupBy])

  const pageBusy = loading || devicesLoading

  return (
    <div className="space-y-6">
      <section className="rgf-section">
        <div className="mb-3 flex flex-wrap items-end justify-between gap-2">
          <div>
            <h2 className="rgf-section-kicker">All-time site feasibility</h2>
            <p className="rgf-section-lead">
              Each card sums every harvest event at that location without a date filter.
              <br />
              Choose a device to compare that total against one day of its power consumption.
              <br />
              The coverage bar shows how much of a single day&apos;s load you&apos;ve accumulated in
              total, not what the site generates on a typical day.
              <br />
              Open{' '}
              <Link to="/dashboards/trends" className="rgf-link">
                Trends over time
              </Link>{' '}
              to see harvest energy within a date range.
            </p>
          </div>
        </div>
        <div className="min-w-0 max-w-full">
          <label htmlFor="edge-device-select" className="rgf-label">
            Select Target Edge Device
          </label>
          <select
            id="edge-device-select"
            value={selectedDevice?.id ?? ''}
            onChange={(e) => {
              const id = e.target.value
              const next = devices.find((d) => d.id === id) ?? null
              setSelectedDevice(next)
            }}
            disabled={devices.length === 0 || devicesLoading}
            className="rgf-select max-w-none"
          >
            {devices.length === 0 && !devicesLoading ? (
              <option value="">No devices loaded</option>
            ) : (
              devices.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name} ({formatDecimal(d.dailyRequiredWh, 3)} Wh/day)
                </option>
              ))
            )}
          </select>
          {devicesLoading && <p className="rgf-hint mt-2">Loading edge device catalog…</p>}
          {devicesError && !devicesLoading && (
            <p className="rgf-status rgf-status--inline rgf-status--error mt-2" role="alert">
              {devicesError}
            </p>
          )}
        </div>
      </section>

      <section className="rgf-section">
        <h3 className="rgf-section-kicker">Roll-up and scope</h3>
        <p className="rgf-section-lead mb-4 max-w-3xl">
          Keep per-chokepoint cards, or sum all-time totals by city. Optionally restrict to a single
          city or a single chokepoint (site + street / place label from the API). Scope filters do not
          change the time window — totals remain all-time for the selected locations.
        </p>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div className="min-w-0">
            <label htmlFor="view-group-by" className="rgf-label">
              View
            </label>
            <select
              id="view-group-by"
              value={groupBy}
              onChange={(e) => {
                const v = e.target.value as 'site' | 'city'
                setGroupBy(v)
                if (v === 'city') setScopeSiteKey('')
              }}
              className="rgf-select max-w-none"
            >
              <option value="site">By chokepoint (each site)</option>
              <option value="city">By city (aggregated)</option>
            </select>
          </div>
          <div className="min-w-0">
            <label htmlFor="scope-city" className="rgf-label">
              City scope
            </label>
            <select
              id="scope-city"
              value={scopeCity}
              onChange={(e) => {
                setScopeCity(e.target.value)
                setScopeSiteKey('')
              }}
              className="rgf-select max-w-none"
            >
              <option value="">All cities</option>
              {cityOptions.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
          <div className="min-w-0">
            <label htmlFor="scope-site" className="rgf-label">
              Chokepoint scope
            </label>
            <select
              id="scope-site"
              value={scopeSiteKey}
              onChange={(e) => setScopeSiteKey(e.target.value)}
              disabled={groupBy === 'city'}
              className="rgf-select max-w-none disabled:cursor-not-allowed disabled:opacity-45"
            >
              <option value="">All chokepoints</option>
              {siteOptions.map((o) => (
                <option key={o.key} value={o.key}>
                  {o.label}
                </option>
              ))}
            </select>
            {groupBy === 'city' && (
              <p className="rgf-hint mt-1.5">
                Chokepoint filter applies only in “By chokepoint” view.
              </p>
            )}
          </div>
        </div>
      </section>

      <div className="flex flex-col gap-4 sm:flex-row sm:flex-wrap sm:items-end">
        <div className="min-w-[12rem] flex-1">
          <label htmlFor="location-insights-filter" className="rgf-label">
            Search
          </label>
          <input
            id="location-insights-filter"
            type="search"
            value={locationFilter}
            onChange={(e) => setLocationFilter(e.target.value)}
            placeholder="Search by street or area…"
            className="rgf-input"
            autoComplete="off"
          />
        </div>
        <div className="min-w-[14rem]">
          <label htmlFor="location-sort" className="rgf-label">
            Sort
          </label>
          <select
            id="location-sort"
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="rgf-select"
          >
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.id} value={opt.id}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {pageBusy && (
        <p className="rgf-status rgf-status--panel rgf-status--loading">
          Syncing all-time location aggregates and device catalog…
        </p>
      )}
      {error && !pageBusy && (
        <p className="rgf-status rgf-status--panel rgf-status--error" role="alert">
          {error}
        </p>
      )}
      {!pageBusy && !error && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {sorted.length === 0 ? (
            <div className="rgf-empty">
              No rows match your search, scope, or roll-up settings.
            </div>
          ) : (
            sorted.map((row, i) => {
              const city = row.city
              const site = row.location
              const cardKey = `${city}|${site}|${i}`
              const whDisplay = formatEquivalent(row.totalWattHours)
              const joulesDisplay = formatDecimal(row.totalJoules)
              const compressionMetrics = hasCompressionMetrics(row)
              const totalCompressions = compressionMetrics
                ? asNonNegativeInt(row.totalCompressions)
                : null
              const successfulActivations = compressionMetrics
                ? asNonNegativeInt(row.successfulActivations)
                : null
              const activationRate =
                compressionMetrics && totalCompressions != null && totalCompressions > 0
                  ? formatActivationRate(successfulActivations ?? 0, totalCompressions)
                  : null
              const pct = feasibilityPercent(row.totalWattHours, selectedDevice)
              const meta = feasibilityBadge(pct)
              const barWidth = pct === null ? 0 : Math.min(100, pct)

              return (
                <article key={cardKey} className="rgf-card-loc">
                  <h3>
                    <span className="block">{city || '—'}</span>
                    <span className="rgf-card-subtitle">{site || '—'}</span>
                  </h3>
                  <p className="rgf-card-metric">
                    {whDisplay}
                    <span>Wh all-time</span>
                  </p>
                  <p className="mt-1 font-mono text-xs tabular-nums text-[var(--rgf-text-subtle)]">
                    {joulesDisplay} J all-time
                  </p>
                  {compressionMetrics && totalCompressions != null && (
                    <div className="rgf-card-detail">
                      <p className="tabular-nums">
                        <span className="rgf-card-detail-label">Total compressions</span>{' '}
                        <span className="rgf-card-detail-value">
                          {totalCompressions.toLocaleString()}
                        </span>
                      </p>
                      <p className="tabular-nums">
                        <span className="rgf-card-detail-label">Successful activations</span>{' '}
                        <span className="rgf-card-detail-value rgf-card-detail-value--success">
                          {(successfulActivations ?? 0).toLocaleString()}
                        </span>
                        {activationRate && (
                          <span className="rgf-card-detail-label"> ({activationRate})</span>
                        )}
                      </p>
                    </div>
                  )}

                  <div className={`mt-3 ${meta.className}`}>{meta.label}</div>

                  <div className="mt-3">
                    <div className="rgf-progress-meta">
                      <span>All-time vs. one day of load</span>
                      <span className="rgf-progress-meta-value">
                        {pct === null ? '—' : `${formatDecimal(pct, 2)}%`}
                      </span>
                    </div>
                    <div className="rgf-progress-track">
                      <progress
                        className="absolute inset-0 h-full w-full opacity-0"
                        max={100}
                        value={barWidth}
                        aria-label="All-time harvest versus one day of target device load"
                      />
                      <div className={meta.barClass} style={{ width: `${barWidth}%` }} />
                    </div>
                    {selectedDevice && pct !== null && (
                      <p className="mt-1.5 text-[0.6875rem] leading-relaxed text-[var(--rgf-text-subtle)]">
                        Cumulative total vs. one day of {selectedDevice.name} (
                        {formatDecimal(selectedDevice.dailyRequiredWh, 3)} Wh/day required)
                      </p>
                    )}
                  </div>
                </article>
              )
            })
          )}
        </div>
      )}
    </div>
  )
}
