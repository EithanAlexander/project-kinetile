import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { toUserSafeError } from '../../api/client'
import { fetchLedgerPageRaw } from '../../api/ledger'
import type { LedgerPageParsed } from '../../api/types/ledger'
import LedgerFilters from './LedgerFilters'
import LedgerTable from './LedgerTable'
import type { TimePresetId } from './ledgerConstants'
import {
  buildLedgerQueryUrl,
  parseLedgerPagePayload,
  resolveLedgerDisplay,
  type LedgerQueryState,
} from './ledgerQuery'

export default function EnergyLedger() {
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(50)
  const [sort, setSort] = useState('eventTimestamp,desc')
  const [timePreset, setTimePreset] = useState<TimePresetId>('all')
  const [locationContains, setLocationContains] = useState('')
  const [eventIdPrefix, setEventIdPrefix] = useState('')
  const [minEnergyJoules, setMinEnergyJoules] = useState('')
  const [maxEnergyJoules, setMaxEnergyJoules] = useState('')
  const [minPowerWatts, setMinPowerWatts] = useState('')
  const [maxPowerWatts, setMaxPowerWatts] = useState('')
  const [minImpactMultiplier, setMinImpactMultiplier] = useState('')
  const [maxImpactMultiplier, setMaxImpactMultiplier] = useState('')
  const [activationOnly, setActivationOnly] = useState(false)

  const [lastGood, setLastGood] = useState<LedgerPageParsed | null>(null)

  const queryState: LedgerQueryState = useMemo(
    () => ({
      page,
      pageSize,
      sort,
      timePreset,
      locationContains,
      eventIdPrefix,
      minEnergyJoules,
      maxEnergyJoules,
      minPowerWatts,
      maxPowerWatts,
      minImpactMultiplier,
      maxImpactMultiplier,
      activationOnly,
    }),
    [
      page,
      pageSize,
      sort,
      timePreset,
      locationContains,
      eventIdPrefix,
      minEnergyJoules,
      maxEnergyJoules,
      minPowerWatts,
      maxPowerWatts,
      minImpactMultiplier,
      maxImpactMultiplier,
      activationOnly,
    ],
  )

  const [debouncedQueryState, setDebouncedQueryState] = useState(queryState)

  useEffect(() => {
    const handle = globalThis.setTimeout(() => setDebouncedQueryState(queryState), 400)
    return () => globalThis.clearTimeout(handle)
  }, [queryState])

  const requestUrl = useMemo(() => buildLedgerQueryUrl(debouncedQueryState), [debouncedQueryState])

  const { data, error, isFetching, isPlaceholderData } = useQuery({
    queryKey: ['ledger', debouncedQueryState],
    queryFn: async ({ signal }) => {
      const raw = await fetchLedgerPageRaw(requestUrl, signal)
      return parseLedgerPagePayload(raw)
    },
    placeholderData: keepPreviousData,
  })

  if (data?.clampToPage != null && page !== data.clampToPage) {
    setPage(data.clampToPage)
  }

  if (data && data.clampToPage == null && data !== lastGood) {
    setLastGood(data)
  }

  const displayData = useMemo(
    () => resolveLedgerDisplay(data, error, lastGood),
    [data, error, lastGood],
  )

  const rows = displayData.content
  const totalElements = displayData.totalElements
  const totalPages = displayData.totalPages
  const loading = isFetching && !isPlaceholderData

  const resetPage = useCallback(() => setPage(0), [])

  const rangeLabel = useMemo(() => {
    if (totalElements === 0) return 'No rows'
    const start = page * pageSize + 1
    const end = Math.min((page + 1) * pageSize, totalElements)
    return `${start}–${end} of ${totalElements}`
  }, [page, pageSize, totalElements])

  const identitySectionActive = eventIdPrefix.trim() !== '' || activationOnly
  const placeSectionActive = locationContains.trim() !== ''
  const energySectionActive =
    minEnergyJoules.trim() !== '' ||
    maxEnergyJoules.trim() !== '' ||
    minPowerWatts.trim() !== '' ||
    maxPowerWatts.trim() !== ''
  const impactSectionActive =
    minImpactMultiplier.trim() !== '' || maxImpactMultiplier.trim() !== ''

  const errorMessage = error ? toUserSafeError(error, 'Failed to load data') : null

  return (
    <div className="space-y-6">
      <header>
        <h2 className="rgf-h2-section">Compression ledger</h2>
        <p className="rgf-lead-ledger">
          Each row is one tile compression with force, activation outcome, and generated watt-hours.
        </p>
      </header>

      <LedgerFilters
        timePreset={timePreset}
        setTimePreset={setTimePreset}
        sort={sort}
        setSort={setSort}
        pageSize={pageSize}
        setPageSize={setPageSize}
        eventIdPrefix={eventIdPrefix}
        setEventIdPrefix={setEventIdPrefix}
        locationContains={locationContains}
        setLocationContains={setLocationContains}
        minEnergyJoules={minEnergyJoules}
        setMinEnergyJoules={setMinEnergyJoules}
        maxEnergyJoules={maxEnergyJoules}
        setMaxEnergyJoules={setMaxEnergyJoules}
        minPowerWatts={minPowerWatts}
        setMinPowerWatts={setMinPowerWatts}
        maxPowerWatts={maxPowerWatts}
        setMaxPowerWatts={setMaxPowerWatts}
        minImpactMultiplier={minImpactMultiplier}
        setMinImpactMultiplier={setMinImpactMultiplier}
        maxImpactMultiplier={maxImpactMultiplier}
        setMaxImpactMultiplier={setMaxImpactMultiplier}
        activationOnly={activationOnly}
        setActivationOnly={setActivationOnly}
        resetPage={resetPage}
        identitySectionActive={identitySectionActive}
        placeSectionActive={placeSectionActive}
        energySectionActive={energySectionActive}
        impactSectionActive={impactSectionActive}
      />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="rgf-label m-0">
          {loading ? 'Loading…' : rangeLabel}
          {!loading && totalPages > 0 && (
            <span className="ml-2 font-normal opacity-80">
              (page {page + 1} of {Math.max(1, totalPages)})
            </span>
          )}
        </p>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            className="rgf-input cursor-pointer px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-50"
            disabled={isFetching || page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Previous
          </button>
          <button
            type="button"
            className="rgf-input cursor-pointer px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-50"
            disabled={isFetching || page >= totalPages - 1 || totalPages === 0}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      </div>

      <div className="rgf-panel-ledger">
        {loading && (
          <p className="rgf-status rgf-status--panel rgf-status--loading">
            Loading compression ledger…
          </p>
        )}
        {errorMessage && !loading && (
          <p className="rgf-status rgf-status--panel rgf-status--error" role="alert">
            {errorMessage}
          </p>
        )}
        {!loading && !errorMessage && (
          <LedgerTable
            rows={rows}
            sort={sort}
            onSortChange={(nextSort) => {
              setSort(nextSort)
              resetPage()
            }}
            onSortReset={() => {
              setSort('eventTimestamp,desc')
              resetPage()
            }}
          />
        )}
      </div>
    </div>
  )
}
