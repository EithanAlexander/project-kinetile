import { PAGE_SIZES, SORT_OPTIONS, TIME_PRESETS, type TimePresetId } from './ledgerConstants'

export interface LedgerFiltersProps {
  timePreset: TimePresetId
  setTimePreset: (v: TimePresetId) => void
  sort: string
  setSort: (v: string) => void
  pageSize: number
  setPageSize: (n: number) => void
  eventIdPrefix: string
  setEventIdPrefix: (v: string) => void
  locationContains: string
  setLocationContains: (v: string) => void
  minEnergyJoules: string
  setMinEnergyJoules: (v: string) => void
  maxEnergyJoules: string
  setMaxEnergyJoules: (v: string) => void
  minPowerWatts: string
  setMinPowerWatts: (v: string) => void
  maxPowerWatts: string
  setMaxPowerWatts: (v: string) => void
  minImpactMultiplier: string
  setMinImpactMultiplier: (v: string) => void
  maxImpactMultiplier: string
  setMaxImpactMultiplier: (v: string) => void
  activationOnly: boolean
  setActivationOnly: (v: boolean) => void
  resetPage: () => void
  identitySectionActive: boolean
  placeSectionActive: boolean
  energySectionActive: boolean
  impactSectionActive: boolean
}

export default function LedgerFilters({
  timePreset,
  setTimePreset,
  sort,
  setSort,
  pageSize,
  setPageSize,
  eventIdPrefix,
  setEventIdPrefix,
  locationContains,
  setLocationContains,
  minEnergyJoules,
  setMinEnergyJoules,
  maxEnergyJoules,
  setMaxEnergyJoules,
  minPowerWatts,
  setMinPowerWatts,
  maxPowerWatts,
  setMaxPowerWatts,
  minImpactMultiplier,
  setMinImpactMultiplier,
  maxImpactMultiplier,
  setMaxImpactMultiplier,
  activationOnly,
  setActivationOnly,
  resetPage,
  identitySectionActive,
  placeSectionActive,
  energySectionActive,
  impactSectionActive,
}: Readonly<LedgerFiltersProps>) {
  return (
    <div className="rgf-filter-sections">
      <details className="rgf-filter-details" open>
        <summary>When &amp; ordering</summary>
        <div className="rgf-filter-details-body">
          <div className="rgf-filter-grid rgf-filter-grid-3">
            <div>
              <label htmlFor="ledger-time" className="rgf-label">
                Time window
              </label>
              <select
                id="ledger-time"
                className="rgf-input w-full"
                value={timePreset}
                onChange={(e) => {
                  setTimePreset(e.target.value as TimePresetId)
                  resetPage()
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
              <label htmlFor="ledger-sort" className="rgf-label">
                Sort
              </label>
              <select
                id="ledger-sort"
                className="rgf-input w-full"
                value={sort}
                onChange={(e) => {
                  setSort(e.target.value)
                  resetPage()
                }}
              >
                {SORT_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="ledger-page-size" className="rgf-label">
                Rows per page
              </label>
              <select
                id="ledger-page-size"
                className="rgf-input w-full"
                value={String(pageSize)}
                onChange={(e) => {
                  setPageSize(Number(e.target.value))
                  resetPage()
                }}
              >
                {PAGE_SIZES.map((n) => (
                  <option key={n} value={String(n)}>
                    {n}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      </details>

      <details className="rgf-filter-details">
        <summary>
          Event ID
          {identitySectionActive ? ' — active' : ''}
        </summary>
        <div className="rgf-filter-details-body">
          <div className="rgf-filter-grid">
            <div>
              <label htmlFor="ledger-event-prefix" className="rgf-label">
                Event id starts with
              </label>
              <input
                id="ledger-event-prefix"
                type="text"
                value={eventIdPrefix}
                onChange={(e) => {
                  setEventIdPrefix(e.target.value)
                  resetPage()
                }}
                placeholder="Prefix match"
                className="rgf-input w-full"
                autoComplete="off"
              />
            </div>
            <div className="flex items-end">
              <label className="rgf-label flex items-center gap-2">
                <input
                  type="checkbox"
                  aria-label="Successful activations only"
                  checked={activationOnly}
                  onChange={(e) => {
                    setActivationOnly(e.target.checked)
                    resetPage()
                  }}
                />
                <span>Successful activations only</span>
              </label>
            </div>
          </div>
        </div>
      </details>

      <details className="rgf-filter-details">
        <summary>
          Location
          {placeSectionActive ? ' — active' : ''}
        </summary>
        <div className="rgf-filter-details-body">
          <div className="rgf-filter-grid">
            <div>
              <label htmlFor="ledger-location" className="rgf-label">
                Location contains
              </label>
              <input
                id="ledger-location"
                type="search"
                value={locationContains}
                onChange={(e) => {
                  setLocationContains(e.target.value)
                  resetPage()
                }}
                placeholder="Substring match on location name"
                className="rgf-input w-full"
                autoComplete="off"
              />
            </div>
          </div>
        </div>
      </details>

      <details className="rgf-filter-details rgf-filter-details-full">
        <summary>
          Energy Min / Max
          {energySectionActive ? ' — active' : ''}
        </summary>
        <div className="rgf-filter-details-body">
          <div className="rgf-filter-grid rgf-filter-grid-4">
            <div>
              <label htmlFor="ledger-min-energy" className="rgf-label">
                Minimum (J)
              </label>
              <input
                id="ledger-min-energy"
                type="number"
                inputMode="decimal"
                min={0}
                step="any"
                value={minEnergyJoules}
                onChange={(e) => {
                  setMinEnergyJoules(e.target.value)
                  resetPage()
                }}
                placeholder="e.g. 5"
                className="rgf-input w-full"
              />
            </div>
            <div>
              <label htmlFor="ledger-max-energy" className="rgf-label">
                Maximum (J)
              </label>
              <input
                id="ledger-max-energy"
                type="number"
                inputMode="decimal"
                min={0}
                step="any"
                value={maxEnergyJoules}
                onChange={(e) => {
                  setMaxEnergyJoules(e.target.value)
                  resetPage()
                }}
                placeholder="Optional upper cap"
                className="rgf-input w-full"
              />
            </div>
            <div>
              <label htmlFor="ledger-min-watts" className="rgf-label">
                Minimum (W)
              </label>
              <input
                id="ledger-min-watts"
                type="number"
                inputMode="decimal"
                min={0}
                step="any"
                value={minPowerWatts}
                onChange={(e) => {
                  setMinPowerWatts(e.target.value)
                  resetPage()
                }}
                placeholder="e.g. 5"
                className="rgf-input w-full"
              />
            </div>
            <div>
              <label htmlFor="ledger-max-watts" className="rgf-label">
                Maximum (W)
              </label>
              <input
                id="ledger-max-watts"
                type="number"
                inputMode="decimal"
                min={0}
                step="any"
                value={maxPowerWatts}
                onChange={(e) => {
                  setMaxPowerWatts(e.target.value)
                  resetPage()
                }}
                placeholder="Optional upper cap"
                className="rgf-input w-full"
              />
            </div>
          </div>
        </div>
      </details>

      <details className="rgf-filter-details">
        <summary>
          Impact multiplier
          {impactSectionActive ? ' — active' : ''}
        </summary>
        <div className="rgf-filter-details-body">
          <div className="rgf-filter-grid">
            <div>
              <label htmlFor="ledger-min-impact" className="rgf-label">
                Minimum (×)
              </label>
              <input
                id="ledger-min-impact"
                type="number"
                inputMode="decimal"
                min={1}
                max={1.5}
                step="any"
                value={minImpactMultiplier}
                onChange={(e) => {
                  setMinImpactMultiplier(e.target.value)
                  resetPage()
                }}
                placeholder="1.0"
                className="rgf-input w-full"
              />
            </div>
            <div>
              <label htmlFor="ledger-max-impact" className="rgf-label">
                Maximum (×)
              </label>
              <input
                id="ledger-max-impact"
                type="number"
                inputMode="decimal"
                min={1}
                max={1.5}
                step="any"
                value={maxImpactMultiplier}
                onChange={(e) => {
                  setMaxImpactMultiplier(e.target.value)
                  resetPage()
                }}
                placeholder="1.5"
                className="rgf-input w-full"
              />
            </div>
          </div>
        </div>
      </details>
    </div>
  )
}
