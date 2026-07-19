import type { LedgerRow } from '../../api/types/ledger'
import { rowTimestamp } from '../../api/types/ledger'
import { truncateUuid } from '../../api/infrastructure'
import { formatDecimal, formatEquivalent, wattHoursFromJoulesExact } from '../../utils/energyFormat'
import { formatTimestamp, formatTimestampCompact } from '../../utils/formatTimestamp'
import { useMemo, useState } from 'react'

export interface LedgerTableProps {
  rows: LedgerRow[]
  sort: string
  onSortChange: (sort: string) => void
  onSortReset: () => void
}

const COLUMN_DEFS = [
  { id: 'eventId', label: 'Event ID', sortField: 'eventId', alignRight: false, width: 16 },
  { id: 'posted', label: 'Timestamp', sortField: 'eventTimestamp', alignRight: false, width: 13 },
  { id: 'city', label: 'City', sortField: 'city', alignRight: false, width: 12 },
  { id: 'location', label: 'Location', sortField: 'location', alignRight: false, width: 16 },
  { id: 'tile', label: 'Tile', sortField: 'tile', alignRight: false, width: 9 },
  { id: 'brand', label: 'Brand', sortField: 'brand', alignRight: false, width: 11 },
  { id: 'activation', label: 'Activation', sortField: 'activation', alignRight: false, width: 11 },
  { id: 'wh', label: 'Generated Wh', sortField: 'energy', alignRight: true, width: 9 },
  { id: 'force', label: 'Force', sortField: 'force', alignRight: true, width: 8 },
  { id: 'mass', label: 'Mass', sortField: 'mass', alignRight: true, width: 7 },
  { id: 'impact', label: 'Impact', sortField: 'impact', alignRight: true, width: 6 },
] as const

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

export default function LedgerTable({
  rows,
  sort,
  onSortChange,
  onSortReset,
}: Readonly<LedgerTableProps>) {
  const [colWidths, setColWidths] = useState<number[]>(DEFAULT_COLUMN_WIDTHS)

  const sortState = useMemo(() => {
    const [field, dir] = sort.split(',')
    return { field: field ?? '', dir: dir ?? 'desc' }
  }, [sort])

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
    if (sortState.field !== sortField) {
      onSortChange(`${sortField},desc`)
      return
    }
    if (sortState.dir === 'desc') {
      onSortChange(`${sortField},asc`)
      return
    }
    onSortChange(`${sortField},desc`)
  }

  const sortMarker = (columnIndex: number): string => {
    const field = COLUMN_DEFS[columnIndex]?.sortField
    if (!field || sortState.field !== field) return ''
    return sortState.dir === 'asc' ? ' ▲' : ' ▼'
  }

  return (
    <div className="rgf-ledger-table-wrap">
      <div className="rgf-ledger-table-toolbar">
        <button type="button" className="rgf-input cursor-pointer px-3 py-1.5 text-sm" onClick={onSortReset}>
          Reset sort
        </button>
      </div>
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
                    disabled={!column.sortField}
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
          {rows.length === 0 ? (
            <tr>
              <td colSpan={11} className="rgf-cell-empty">
                No compressions match your filters.
              </td>
            </tr>
          ) : (
            rows.map((row, i) => {
              const wh = wattHoursFromJoulesExact(row?.calculatedEnergyJoules)
              return (
                <tr
                  key={row?.id === undefined || row?.id === null ? `idx-${i}` : `id-${row.id}`}
                  style={{
                    backgroundColor:
                      i % 2 === 0
                        ? 'var(--rgf-surface-table-row-alt)'
                        : 'var(--rgf-surface-table-row-alt2)',
                  }}
                >
                  <td className="rgf-td-entity" title={row?.eventId ?? undefined}>
                    {row?.eventId ?? '—'}
                  </td>
                  <td className="rgf-td-mono-sm" title={formatTimestamp(rowTimestamp(row))}>
                    {formatTimestampCompact(rowTimestamp(row))}
                  </td>
                  <td className="rgf-td-truncate" title={row?.city ?? undefined}>
                    {row?.city ?? '—'}
                  </td>
                  <td className="rgf-td-truncate" title={row?.location ?? undefined}>
                    {row?.location ?? '—'}
                  </td>
                  <td className="rgf-td-mono-sm" title={row?.tileId ?? undefined}>
                    {truncateUuid(row?.tileId ?? null)}
                  </td>
                  <td className="rgf-td-truncate" title={row?.manufacturerName ?? undefined}>
                    {row?.manufacturerName ?? '—'}
                  </td>
                  <td className="rgf-td-body">
                    {row?.activationSuccessful ? 'Successful' : 'Below threshold'}
                  </td>
                  <td className="rgf-td-energy text-right">
                    <span className="rgf-energy-value">{formatEquivalent(wh)}</span>
                    <span className="rgf-energy-unit"> Wh</span>
                  </td>
                  <td className="rgf-td-num-right">
                    {typeof row?.calculatedForceNewtons === 'number'
                      ? formatDecimal(row.calculatedForceNewtons, 1)
                      : '—'}
                  </td>
                  <td className="rgf-td-num-right">
                    {typeof row?.massKg === 'number' ? Number(row.massKg).toFixed(2) : '—'}
                  </td>
                  <td className="rgf-td-num-right">
                    {typeof row?.impactMultiplier === 'number'
                      ? Number(row.impactMultiplier).toFixed(2)
                      : '—'}
                  </td>
                </tr>
              )
            })
          )}
        </tbody>
      </table>
    </div>
  )
}
