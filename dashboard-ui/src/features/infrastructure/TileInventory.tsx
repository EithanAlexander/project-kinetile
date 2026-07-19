import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useCallback, useMemo, useState, type Dispatch, type ReactNode, type SetStateAction } from 'react'
import { toUserSafeError } from '../../api/client'
import {
  fetchChokepoints,
  fetchCitiesCatalog,
  fetchTilesForChokepoint,
  truncateUuid,
  type ChokepointCatalogRow,
  type CityCatalogRow,
  type TileCatalogRow,
} from '../../api/infrastructure'
import { fetchTileMonitoringConfig } from '../../api/tileMonitoringConfig'
import { DEFAULT_TILE_MONITORING_CONFIG } from '../../config/tileMonitoringDefaults'
import { formatDaysSinceLastCompression, formatTimestamp, formatTimestampCompact } from '../../utils/formatTimestamp'
import {
  buildTileInventorySummary,
  type CountBucket,
  type TileInventorySummary,
} from './tileInventorySummary'
import {
  DEFAULT_TILE_INVENTORY_SORT,
  parseTileInventorySort,
  sortTileRows,
  toggleTileInventorySort,
  type TileInventorySortField,
} from './tileInventorySort'
import { isStaleActiveTile } from './tileStale'

const TILE_PAGE_SIZE_OPTIONS = [25, 50, 100] as const

const COLUMN_DEFS = [
  { id: 'tileId', label: 'Tile ID', sortField: 'tileId', alignRight: false, width: 16 },
  { id: 'manufacturer', label: 'Manufacturer', sortField: 'manufacturer', alignRight: false, width: 13 },
  { id: 'size', label: 'Size', sortField: 'size', alignRight: false, width: 9 },
  { id: 'color', label: 'Color', sortField: 'color', alignRight: false, width: 9 },
  { id: 'installed', label: 'Installed', sortField: 'installed', alignRight: false, width: 10 },
  { id: 'lastCompression', label: 'Last compression', sortField: 'lastCompression', alignRight: false, width: 12 },
  {
    id: 'daysSinceLastCompression',
    label: 'Days since last compression',
    sortField: 'daysSinceLastCompression',
    alignRight: true,
    width: 11,
  },
  { id: 'compressions', label: 'Compressions', sortField: 'compressions', alignRight: true, width: 9 },
] as const satisfies ReadonlyArray<{
  id: string
  label: string
  sortField: TileInventorySortField
  alignRight: boolean
  width: number
}>

const DEFAULT_COLUMN_WIDTHS = COLUMN_DEFS.map((col) => col.width)

function withUpdatedColumnWidth(
  widths: number[],
  targetIndex: number,
  nextWidth: number,
): number[] {
  const updated = [...widths]
  if (targetIndex >= 0 && targetIndex < updated.length) {
    updated[targetIndex] = nextWidth
  }
  return updated
}

function staleBadge(
  tile: Pick<TileCatalogRow, 'active' | 'lastCompressionAt'>,
  isStale: (tile: Pick<TileCatalogRow, 'active' | 'lastCompressionAt'>) => boolean,
) {
  if (isStale(tile)) {
    return (
      <span className="rgf-badge-stale">Stale</span>
    )
  }
  if (!tile.lastCompressionAt) {
    return (
      <span className="rgf-badge-inactive">No activity</span>
    )
  }
  return null
}

const TILE_TABLE_COLSPAN = COLUMN_DEFS.length

function tileTableMessage(message: string) {
  return (
    <tr>
      <td colSpan={TILE_TABLE_COLSPAN} className="rgf-cell-empty">
        {message}
      </td>
    </tr>
  )
}

function renderTileTableBody(
  chokepointId: number | '',
  tilesLoading: boolean,
  tiles: TileCatalogRow[],
  isStale: (tile: Pick<TileCatalogRow, 'active' | 'lastCompressionAt'>) => boolean,
  hideStaleTiles: boolean,
): ReactNode {
  if (chokepointId === '') {
    return tileTableMessage('Select a city and chokepoint to view tiles.')
  }
  if (tilesLoading) {
    return tileTableMessage('Loading tiles…')
  }
  if (tiles.length === 0) {
    return tileTableMessage(
      hideStaleTiles
        ? 'No non-stale tiles in this chokepoint.'
        : 'No tiles registered for this chokepoint.',
    )
  }
  return tiles.map((tile) => (
    <tr key={tile.tileId}>
      <td className="rgf-td-mono-sm" title={tile.tileId}>
        {truncateUuid(tile.tileId)}
        {staleBadge(tile, isStale)}
      </td>
      <td>{tile.manufacturerName}</td>
      <td>{tile.size}</td>
      <td>{tile.color}</td>
      <td>{tile.installationDate}</td>
      <td className="rgf-td-mono-sm" title={formatTimestamp(tile.lastCompressionAt)}>
        {formatTimestampCompact(tile.lastCompressionAt)}
      </td>
      <td
        className="rgf-td-num-right"
        title={
          tile.lastCompressionAt
            ? `${formatDaysSinceLastCompression(tile.lastCompressionAt)} days since ${formatTimestamp(tile.lastCompressionAt)}`
            : undefined
        }
      >
        {formatDaysSinceLastCompression(tile.lastCompressionAt)}
      </td>
      <td className="text-right tabular-nums">{tile.totalCompressions}</td>
    </tr>
  ))
}

function resolveInventoryError(citiesError: unknown, tilesError: unknown): string | null {
  if (citiesError) {
    return toUserSafeError(citiesError, 'Failed to load cities')
  }
  if (tilesError) {
    return toUserSafeError(tilesError, 'Failed to load tiles')
  }
  return null
}

const BREAKDOWN_COLUMN_DEFS = [
  { id: 'value', label: 'Value', alignRight: false, defaultWidth: 168, minWidth: 96 },
  { id: 'tiles', label: 'Tiles', alignRight: true, defaultWidth: 72, minWidth: 56 },
] as const

const BREAKDOWN_DEFAULT_COLUMN_WIDTHS = BREAKDOWN_COLUMN_DEFS.map((column) => column.defaultWidth)
const BREAKDOWN_COLUMN_MIN_WIDTHS = BREAKDOWN_COLUMN_DEFS.map((column) => column.minWidth)

function beginBreakdownColumnResize(
  colWidths: number[],
  columnIndex: number,
  startX: number,
  setColWidths: Dispatch<SetStateAction<number[]>>,
) {
  const startWidth = colWidths[columnIndex] ?? BREAKDOWN_DEFAULT_COLUMN_WIDTHS[columnIndex]
  const minWidth = BREAKDOWN_COLUMN_MIN_WIDTHS[columnIndex] ?? 44
  const onMove = (event: PointerEvent) => {
    const next = Math.max(minWidth, startWidth + (event.clientX - startX))
    setColWidths((prev) => withUpdatedColumnWidth(prev, columnIndex, next))
  }
  const onUp = () => {
    globalThis.removeEventListener('pointermove', onMove)
    globalThis.removeEventListener('pointerup', onUp)
  }
  globalThis.addEventListener('pointermove', onMove)
  globalThis.addEventListener('pointerup', onUp)
}

function InventoryBreakdownTable({
  title,
  buckets,
}: Readonly<{ title: string; buckets: CountBucket[] }>) {
  const [colWidths, setColWidths] = useState<number[]>([...BREAKDOWN_DEFAULT_COLUMN_WIDTHS])
  const tableWidth = colWidths.reduce((sum, width) => sum + width, 0)

  return (
    <div className="rgf-breakdown-table-wrap space-y-2">
      <h4 className="rgf-label">{title}</h4>
      <div className="overflow-x-auto">
        <table
          className="rgf-table-ledger rgf-table-breakdown"
          style={{ width: tableWidth }}
        >
          <colgroup>
            {colWidths.map((width, idx) => (
              <col key={BREAKDOWN_COLUMN_DEFS[idx].id} style={{ width }} />
            ))}
          </colgroup>
          <thead>
            <tr>
              {BREAKDOWN_COLUMN_DEFS.map((column, idx) => (
                <th
                  key={column.id}
                  scope="col"
                  className={column.alignRight ? 'text-right rgf-th-resizable' : 'rgf-th-resizable'}
                >
                  {column.label}
                  <div
                    aria-label={`Resize ${column.label} column`}
                    className="rgf-col-resizer"
                    onPointerDown={(e) => beginBreakdownColumnResize(colWidths, idx, e.clientX, setColWidths)}
                  />
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {buckets.length === 0 ? (
              <tr>
                <td colSpan={2} className="rgf-cell-empty">
                  No tiles in this chokepoint.
                </td>
              </tr>
            ) : (
              buckets.map((bucket) => (
                <tr key={bucket.label}>
                  <td className="rgf-td-breakdown-value" title={bucket.label}>
                    {bucket.label}
                  </td>
                  <td className="rgf-td-num-right">{bucket.count}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function TileInventorySummaryPanel({
  summary,
  loading,
  chokepointSelected,
  breakdownTruncated,
  inactivityThresholdDays,
}: Readonly<{
  summary: TileInventorySummary
  loading: boolean
  chokepointSelected: boolean
  breakdownTruncated: boolean
  inactivityThresholdDays: number
}>) {
  if (!chokepointSelected) {
    return null
  }

  return (
    <section className="space-y-4" aria-label="Inventory summary">
      <div>
        <h3 className="text-sm font-semibold uppercase tracking-wide text-[var(--rgf-text-heading)]">
          Inventory summary
        </h3>
        <p className="rgf-hint">
          Stale tiles are still deployed and active, but have no compression within the configured
          inactivity threshold ({inactivityThresholdDays} days).
          {breakdownTruncated ? ' Metrics and breakdowns reflect the loaded tile page.' : ''}
        </p>
      </div>

      {loading ? (
        <p className="rgf-status rgf-status--inline rgf-status--loading">Loading summary…</p>
      ) : (
        <div className="rgf-inventory-summary-cluster space-y-4">
          <div className="rgf-inventory-metrics">
            <div className="rgf-metric-card rgf-metric-card--compact">
              <p className="rgf-metric-label">Registered tiles</p>
              <p className="rgf-metric-value rgf-metric-value--accent">{summary.registeredTiles}</p>
            </div>
            <div className="rgf-metric-card rgf-metric-card--compact">
              <p className="rgf-metric-label">Active tiles</p>
              <p className="rgf-metric-value">{summary.activeTiles}</p>
              {summary.inactiveTiles > 0 && (
                <p className="rgf-hint mt-1">{summary.inactiveTiles} inactive</p>
              )}
            </div>
            <div className="rgf-metric-card rgf-metric-card--compact">
              <p className="rgf-metric-label">Stale (active, idle)</p>
              <p className="rgf-metric-value">{summary.staleTiles}</p>
            </div>
            <div className="rgf-metric-card rgf-metric-card--compact">
              <p className="rgf-metric-label">Total compressions</p>
              <p className="rgf-metric-value">{summary.totalCompressions}</p>
            </div>
          </div>

          <div className="flex flex-wrap items-start gap-4">
            <InventoryBreakdownTable title="By manufacturer" buckets={summary.byManufacturer} />
            <InventoryBreakdownTable title="By tile size" buckets={summary.bySize} />
            <InventoryBreakdownTable title="By color" buckets={summary.byColor} />
          </div>
        </div>
      )}
    </section>
  )
}

export default function TileInventory() {
  const [cityId, setCityId] = useState<number | ''>('')
  const [chokepointId, setChokepointId] = useState<number | ''>('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(50)
  const [colWidths, setColWidths] = useState<number[]>(DEFAULT_COLUMN_WIDTHS)
  const [sort, setSort] = useState(DEFAULT_TILE_INVENTORY_SORT)
  const [hideStaleTiles, setHideStaleTiles] = useState(false)

  const resetPage = useCallback(() => setPage(0), [])

  const sortState = useMemo(() => parseTileInventorySort(sort), [sort])

  const beginResize = (columnIndex: number, startX: number) => {
    const startWidth = colWidths[columnIndex] ?? DEFAULT_COLUMN_WIDTHS[columnIndex]
    const onMove = (event: PointerEvent) => {
      const delta = (event.clientX - startX) / 6
      const next = Math.max(4, Math.min(42, startWidth + delta))
      setColWidths((prev) => withUpdatedColumnWidth(prev, columnIndex, next))
    }
    const onUp = () => {
      globalThis.removeEventListener('pointermove', onMove)
      globalThis.removeEventListener('pointerup', onUp)
    }
    globalThis.addEventListener('pointermove', onMove)
    globalThis.addEventListener('pointerup', onUp)
  }

  const toggleColumnSort = (columnIndex: number) => {
    const sortField = COLUMN_DEFS[columnIndex]?.sortField
    if (!sortField) return
    setSort((current) => toggleTileInventorySort(current, sortField))
  }

  const sortMarker = (columnIndex: number): string => {
    const field = COLUMN_DEFS[columnIndex]?.sortField
    if (!field || sortState.field !== field) return ''
    return sortState.dir === 'asc' ? ' ▲' : ' ▼'
  }

  const {
    data: cities = [],
    isLoading: citiesLoading,
    error: citiesError,
  } = useQuery({
    queryKey: ['infrastructure', 'cities'],
    queryFn: ({ signal }) => fetchCitiesCatalog(signal),
  })

  const {
    data: chokepoints = [],
    isLoading: chokepointsLoading,
  } = useQuery({
    queryKey: ['infrastructure', 'chokepoints', cityId],
    queryFn: ({ signal }) => fetchChokepoints(Number(cityId), signal),
    enabled: cityId !== '',
  })

  const {
    data: tilePage,
    isLoading: tilesLoading,
    isFetching: tilesFetching,
    error: tilesError,
  } = useQuery({
    queryKey: ['infrastructure', 'tiles', chokepointId, page, pageSize],
    queryFn: ({ signal }) => fetchTilesForChokepoint(Number(chokepointId), page, pageSize, signal),
    enabled: chokepointId !== '',
    placeholderData: keepPreviousData,
  })

  const {
    data: monitoringFromApi,
    isError: monitoringError,
  } = useQuery({
    queryKey: ['tile-monitoring', 'config'],
    queryFn: ({ signal }) => fetchTileMonitoringConfig(signal),
    staleTime: 5 * 60_000,
  })

  const inactivityThresholdDays =
    monitoringError || !monitoringFromApi
      ? DEFAULT_TILE_MONITORING_CONFIG.inactivityThresholdDays
      : monitoringFromApi.inactivityThresholdDays

  const isStale = useCallback(
    (tile: Pick<TileCatalogRow, 'active' | 'lastCompressionAt'>) =>
      isStaleActiveTile(tile, inactivityThresholdDays),
    [inactivityThresholdDays],
  )

  const selectedCity = cities.find((c: CityCatalogRow) => c.id === cityId)
  const selectedChokepoint = chokepoints.find((cp: ChokepointCatalogRow) => cp.id === chokepointId)
  const tiles: TileCatalogRow[] = tilePage?.content ?? []
  const sortedTiles = useMemo(() => sortTileRows(tiles, sort), [tiles, sort])
  const visibleTiles = useMemo(
    () => (hideStaleTiles ? sortedTiles.filter((tile) => !isStale(tile)) : sortedTiles),
    [sortedTiles, hideStaleTiles, isStale],
  )
  const registeredTiles = tilePage?.totalElements ?? tiles.length
  const totalPages = tilePage?.totalPages ?? 0
  const breakdownTruncated = registeredTiles > tiles.length

  const rangeLabel = useMemo(() => {
    if (registeredTiles === 0) return 'No tiles'
    const start = page * pageSize + 1
    const end = Math.min((page + 1) * pageSize, registeredTiles)
    return `Showing ${start}–${end} of ${registeredTiles} tiles`
  }, [page, pageSize, registeredTiles])

  const summary = useMemo(
    () => buildTileInventorySummary(tiles, isStale, registeredTiles),
    [tiles, isStale, registeredTiles],
  )

  const error = resolveInventoryError(citiesError, tilesError)

  return (
    <section className="rgf-panel space-y-6">
      <div>
        <h2 className="rgf-panel-title">Tile inventory</h2>
        <p className="rgf-panel-lead">
          Browse registered tiles by city and chokepoint. Stale badges use the configured inactivity
          threshold and each tile&apos;s last compression time.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="block space-y-1">
          <span className="rgf-label">City</span>
          <select
            className="rgf-input w-full"
            value={cityId}
            disabled={citiesLoading}
            onChange={(e) => {
              const next = e.target.value ? Number(e.target.value) : ''
              setCityId(next)
              setChokepointId('')
              setPage(0)
              setSort(DEFAULT_TILE_INVENTORY_SORT)
              setHideStaleTiles(false)
            }}
          >
            <option value="">Select city…</option>
            {cities.map((city) => (
              <option key={city.id} value={city.id}>
                {city.name} ({city.activeTileCount} tiles)
              </option>
            ))}
          </select>
        </label>

        <label className="block space-y-1">
          <span className="rgf-label">Chokepoint</span>
          <select
            className="rgf-input w-full"
            value={chokepointId}
            disabled={cityId === '' || chokepointsLoading}
            onChange={(e) => {
              setChokepointId(e.target.value ? Number(e.target.value) : '')
              resetPage()
              setSort(DEFAULT_TILE_INVENTORY_SORT)
              setHideStaleTiles(false)
            }}
          >
            <option value="">Select chokepoint…</option>
            {chokepoints.map((cp) => (
              <option key={cp.id} value={cp.id}>
                {cp.name} ({cp.activeTileCount} tiles · {cp.trafficTier})
              </option>
            ))}
          </select>
        </label>
      </div>

      {selectedCity && selectedChokepoint && (
        <p className="rgf-hint">
          {selectedChokepoint.placeTypeLabel} · {selectedChokepoint.activeTileCount} active tiles at{' '}
          {selectedChokepoint.name}, {selectedCity.name}
        </p>
      )}

      {error && <p className="rgf-status rgf-status--inline rgf-status--error">{error}</p>}

      <TileInventorySummaryPanel
        summary={summary}
        loading={tilesLoading && chokepointId !== ''}
        chokepointSelected={chokepointId !== ''}
        breakdownTruncated={breakdownTruncated}
        inactivityThresholdDays={inactivityThresholdDays}
      />

      {chokepointId !== '' && (
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <p className="rgf-label m-0">
              {tilesLoading && !tilePage ? 'Loading…' : rangeLabel}
              {!tilesLoading && totalPages > 0 && (
                <span className="ml-2 font-normal opacity-80">
                  (page {page + 1} of {Math.max(1, totalPages)})
                </span>
              )}
            </p>
            <label className="flex items-center gap-2">
              <span className="rgf-label m-0">Per page</span>
              <select
                className="rgf-input cursor-pointer px-2 py-1 text-sm"
                value={String(pageSize)}
                onChange={(e) => {
                  setPageSize(Number(e.target.value))
                  resetPage()
                }}
              >
                {TILE_PAGE_SIZE_OPTIONS.map((size) => (
                  <option key={size} value={size}>
                    {size}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="rgf-input cursor-pointer px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-50"
              disabled={tilesFetching || page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </button>
            <button
              type="button"
              className="rgf-input cursor-pointer px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-50"
              disabled={tilesFetching || page >= totalPages - 1 || totalPages === 0}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </button>
          </div>
        </div>
      )}

      <div className="rgf-ledger-table-wrap">
        <div className="rgf-ledger-table-toolbar rgf-ledger-table-toolbar--actions">
          <button
            type="button"
            className="rgf-btn-toolbar"
            disabled={chokepointId === ''}
            aria-pressed={hideStaleTiles}
            onClick={() => setHideStaleTiles((hidden) => !hidden)}
          >
            {hideStaleTiles ? 'Show stale tiles' : 'Hide stale tiles'}
          </button>
          <button
            type="button"
            className="rgf-btn-toolbar"
            onClick={() => setSort(DEFAULT_TILE_INVENTORY_SORT)}
          >
            Reset sort
          </button>
        </div>
        <div className="overflow-x-auto">
        <table className="rgf-table-ledger">
          <colgroup>
            {colWidths.map((width, idx) => (
              <col key={COLUMN_DEFS[idx].id} style={{ width: `${width}%` }} />
            ))}
          </colgroup>
          <thead>
            <tr>
              {COLUMN_DEFS.map((column, idx) => (
                <th
                  key={column.id}
                  scope="col"
                  className={column.alignRight ? 'text-right rgf-th-resizable' : 'rgf-th-resizable'}
                >
                  <button
                    type="button"
                    className="rgf-th-sort-btn"
                    onClick={() => toggleColumnSort(idx)}
                  >
                    {column.label}
                    {sortMarker(idx)}
                  </button>
                  <div
                    aria-label={`Resize ${column.label} column`}
                    className="rgf-col-resizer"
                    onPointerDown={(e) => beginResize(idx, e.clientX)}
                  />
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {renderTileTableBody(chokepointId, tilesLoading, visibleTiles, isStale, hideStaleTiles)}
          </tbody>
        </table>
        </div>
      </div>
    </section>
  )
}
