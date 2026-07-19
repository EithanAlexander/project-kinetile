import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { toUserSafeError } from '../../api/client'
import { fetchDevices } from '../../api/devices'
import { fetchLocations } from '../../api/locations'
import DeviceFeasibilityCard from '../../components/DeviceFeasibilityCard'
import { formatDecimal, formatEquivalent } from '../../utils/energyFormat'
import {
  feasibilityBadge,
  feasibilityPercent,
  formatActivationRate,
  runtimeDays,
  sumRows,
} from '../feasibility/locationAggregates'
import { useLocationScope } from '../feasibility/useLocationScope'

const DEVICE_SORT_OPTIONS = [
  { id: 'coverage_desc', label: 'Load coverage (high → low)' },
  { id: 'coverage_asc', label: 'Load coverage (low → high)' },
  { id: 'load_asc', label: 'Daily load (low → high)' },
  { id: 'load_desc', label: 'Daily load (high → low)' },
  { id: 'name_asc', label: 'Name (A → Z)' },
] as const

/** Compression statistics with city / chokepoint scope and edge-device feasibility. */
export default function ActivationSummary() {
  const {
    data: rows = [],
    isLoading,
    error,
  } = useQuery({
    queryKey: ['energy', 'locations'],
    queryFn: ({ signal }) => fetchLocations(signal),
  })

  const { data: devices = [] } = useQuery({
    queryKey: ['devices', 'catalog'],
    queryFn: ({ signal }) => fetchDevices(signal),
  })

  const {
    scopeCity,
    setScopeCity,
    scopeSiteKey,
    setScopeSiteKey,
    cityOptions,
    siteOptions,
    scopedRows,
    scopeLabel,
  } = useLocationScope(rows)

  const [hiddenDeviceIds, setHiddenDeviceIds] = useState(() => new Set<string>())
  const [deviceSort, setDeviceSort] = useState<string>('coverage_desc')

  const scoped = useMemo(() => sumRows(scopedRows), [scopedRows])

  const activationRate =
    scoped.totalCompressions > 0
      ? formatActivationRate(scoped.successfulActivations, scoped.totalCompressions)
      : null

  const totalWh = scoped.totalWattHours

  const hiddenDevices = useMemo(
    () => devices.filter((d) => hiddenDeviceIds.has(d.id)),
    [devices, hiddenDeviceIds],
  )

  const deviceCards = useMemo(() => {
    const cards = devices
      .filter((d) => !hiddenDeviceIds.has(d.id))
      .map((d) => {
        const pct = feasibilityPercent(totalWh, d)
        return {
          device: d,
          pct,
          badge: feasibilityBadge(pct),
          days: runtimeDays(totalWh, d),
        }
      })
    const byName = (a: (typeof cards)[0], b: (typeof cards)[0]) =>
      a.device.name.localeCompare(b.device.name, undefined, { sensitivity: 'base' })
    switch (deviceSort) {
      case 'coverage_asc':
        cards.sort((a, b) => (a.pct ?? -Infinity) - (b.pct ?? -Infinity) || byName(a, b))
        break
      case 'load_asc':
        cards.sort((a, b) => a.device.dailyRequiredWh - b.device.dailyRequiredWh || byName(a, b))
        break
      case 'load_desc':
        cards.sort((a, b) => b.device.dailyRequiredWh - a.device.dailyRequiredWh || byName(a, b))
        break
      case 'name_asc':
        cards.sort(byName)
        break
      default:
        cards.sort((a, b) => (b.pct ?? -Infinity) - (a.pct ?? -Infinity) || byName(a, b))
    }
    return cards
  }, [devices, hiddenDeviceIds, totalWh, deviceSort])

  const hideDevice = (id: string) =>
    setHiddenDeviceIds((prev) => {
      const next = new Set(prev)
      next.add(id)
      return next
    })

  const showDevice = (id: string) =>
    setHiddenDeviceIds((prev) => {
      const next = new Set(prev)
      next.delete(id)
      return next
    })

  const showAllDevices = () => setHiddenDeviceIds(new Set())

  const errorMessage = error ? toUserSafeError(error, 'Failed to load summary') : null
  const hasRows = rows.length > 0

  return (
    <div className="space-y-8">
      <header>
        <h2 className="rgf-h2-section">Activation statistics</h2>
        <p className="rgf-lead">
          Tile compression counts and generated energy from the threshold-activation model. Scope the
          numbers to all cities, a single city, or one chokepoint.
        </p>
      </header>

      {isLoading && (
        <p className="rgf-status rgf-status--panel rgf-status--loading">Loading activation summary…</p>
      )}
      {errorMessage && (
        <p className="rgf-status rgf-status--panel rgf-status--error" role="alert">
          {errorMessage}
        </p>
      )}

      {!isLoading && !errorMessage && (
        <>
          <section className="rgf-panel space-y-4">
            <h3 className="rgf-h3">Scope</h3>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="min-w-0">
                <label htmlFor="summary-scope-city" className="rgf-label">
                  City
                </label>
                <select
                  id="summary-scope-city"
                  className="rgf-input min-w-full"
                  value={scopeCity}
                  onChange={(e) => {
                    setScopeCity(e.target.value)
                    setScopeSiteKey('')
                  }}
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
                <label htmlFor="summary-scope-site" className="rgf-label">
                  Chokepoint / location
                </label>
                <select
                  id="summary-scope-site"
                  className="rgf-input min-w-full"
                  value={scopeSiteKey}
                  onChange={(e) => setScopeSiteKey(e.target.value)}
                >
                  <option value="">All chokepoints</option>
                  {siteOptions.map((o) => (
                    <option key={o.key} value={o.key}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <p className="text-sm opacity-80">
              Showing: <strong>{scopeLabel}</strong>
              {activationRate && <span className="opacity-75"> · {activationRate}</span>}
            </p>
          </section>

          {hasRows ? (
            <>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <div className="rgf-metric-card">
                  <p className="rgf-metric-label">Total compressions</p>
                  <p className="rgf-metric-value">{formatDecimal(scoped.totalCompressions, 0)}</p>
                </div>
                <div className="rgf-metric-card">
                  <p className="rgf-metric-label">Successful activations</p>
                  <p className="rgf-metric-value rgf-metric-value--success">
                    {formatDecimal(scoped.successfulActivations, 0)}
                  </p>
                </div>
                <div className="rgf-metric-card">
                  <p className="rgf-metric-label">Generated Wh</p>
                  <p className="rgf-metric-value rgf-metric-value--accent">
                    {formatEquivalent(scoped.totalWattHours)}
                  </p>
                </div>
                <div className="rgf-metric-card">
                  <p className="rgf-metric-label">Generated J</p>
                  <p className="rgf-metric-value">{formatDecimal(scoped.totalJoules, 1)}</p>
                </div>
              </div>

              <section className="rgf-panel space-y-4">
                <div className="flex flex-wrap items-end justify-between gap-4">
                  <div>
                    <h3 className="rgf-h3">Edge-device feasibility ({scopeLabel})</h3>
                    <p className="rgf-hint mt-1">
                      Every catalog device compared against the scoped generated energy. Hide the ones
                      you don&apos;t care about and sort to compare.
                    </p>
                  </div>
                  <div className="min-w-[14rem]">
                    <label htmlFor="summary-device-sort" className="rgf-label">
                      Sort devices
                    </label>
                    <select
                      id="summary-device-sort"
                      className="rgf-input min-w-full"
                      value={deviceSort}
                      onChange={(e) => setDeviceSort(e.target.value)}
                    >
                      {DEVICE_SORT_OPTIONS.map((opt) => (
                        <option key={opt.id} value={opt.id}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {hiddenDevices.length > 0 && (
                  <div className="rgf-toolbar">
                    <span className="text-[var(--rgf-text-subtle)]">Hidden:</span>
                    {hiddenDevices.map((d) => (
                      <button
                        key={d.id}
                        type="button"
                        onClick={() => showDevice(d.id)}
                        className="rgf-chip-tag"
                        title="Show this device again"
                      >
                        {d.name}
                        <span aria-hidden="true" className="text-[var(--rgf-text-subtle)]">
                          +
                        </span>
                      </button>
                    ))}
                    <button type="button" onClick={showAllDevices} className="rgf-btn-sm ml-auto">
                      Show all
                    </button>
                  </div>
                )}

                {deviceCards.length === 0 ? (
                  <p className="rgf-empty">
                    {devices.length === 0
                      ? 'No edge devices loaded.'
                      : 'All devices are hidden — use “Show all” to restore them.'}
                  </p>
                ) : (
                  <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {deviceCards.map(({ device, pct, badge, days }) => (
                      <DeviceFeasibilityCard
                        key={device.id}
                        device={device}
                        pct={pct}
                        badge={badge}
                        days={days}
                        onHide={() => hideDevice(device.id)}
                      />
                    ))}
                  </div>
                )}
              </section>
            </>
          ) : (
            <p className="rgf-empty">No compression data has been recorded yet.</p>
          )}
        </>
      )}
    </div>
  )
}
