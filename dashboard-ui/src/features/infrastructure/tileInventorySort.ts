import type { TileCatalogRow } from '../../api/infrastructure'
import { daysSinceTimestamp } from '../../utils/formatTimestamp'

/** Default tile table ordering (matches API tile id ordering). */
export const DEFAULT_TILE_INVENTORY_SORT = 'tileId,asc'

const SORT_FIELDS = [
  'tileId',
  'manufacturer',
  'size',
  'color',
  'installed',
  'lastCompression',
  'daysSinceLastCompression',
  'compressions',
] as const

export type TileInventorySortField = (typeof SORT_FIELDS)[number]

function isSortField(value: string): value is TileInventorySortField {
  return (SORT_FIELDS as readonly string[]).includes(value)
}

/** Parses a ledger-style sort token (`field,asc|desc`). */
export function parseTileInventorySort(sort: string): {
  field: TileInventorySortField
  dir: 'asc' | 'desc'
} {
  const [field, dir] = sort.split(',')
  return {
    field: field && isSortField(field) ? field : 'tileId',
    dir: dir === 'asc' ? 'asc' : 'desc',
  }
}

function timestampMs(iso: string | null): number | null {
  if (!iso) return null
  const ms = Date.parse(iso)
  return Number.isNaN(ms) ? null : ms
}

function compareText(left: string, right: string): number {
  return left.localeCompare(right, undefined, { sensitivity: 'base', numeric: true })
}

function compareNullableNumbers(left: number | null, right: number | null): number {
  if (left == null && right == null) return 0
  if (left == null) return 1
  if (right == null) return -1
  return left - right
}

function compareTiles(
  left: TileCatalogRow,
  right: TileCatalogRow,
  field: TileInventorySortField,
): number {
  switch (field) {
    case 'tileId':
      return compareText(left.tileId, right.tileId)
    case 'manufacturer':
      return compareText(left.manufacturerName, right.manufacturerName)
    case 'size':
      return compareText(left.size, right.size)
    case 'color':
      return compareText(left.color, right.color)
    case 'installed':
      return compareText(left.installationDate, right.installationDate)
    case 'lastCompression':
      return compareNullableNumbers(
        timestampMs(left.lastCompressionAt),
        timestampMs(right.lastCompressionAt),
      )
    case 'daysSinceLastCompression':
      return compareNullableNumbers(
        daysSinceTimestamp(left.lastCompressionAt),
        daysSinceTimestamp(right.lastCompressionAt),
      )
    case 'compressions':
      return left.totalCompressions - right.totalCompressions
    default:
      return 0
  }
}

function compareNullableField(
  left: TileCatalogRow,
  right: TileCatalogRow,
  field: TileInventorySortField,
): number | null {
  if (field !== 'lastCompression' && field !== 'daysSinceLastCompression') {
    return null
  }
  const leftValue =
    field === 'lastCompression'
      ? timestampMs(left.lastCompressionAt)
      : daysSinceTimestamp(left.lastCompressionAt)
  const rightValue =
    field === 'lastCompression'
      ? timestampMs(right.lastCompressionAt)
      : daysSinceTimestamp(right.lastCompressionAt)
  if (leftValue == null && rightValue == null) return 0
  if (leftValue == null) return 1
  if (rightValue == null) return -1
  return null
}

/** Returns a new array sorted for the selected column and direction. */
export function sortTileRows(tiles: TileCatalogRow[], sort: string): TileCatalogRow[] {
  const { field, dir } = parseTileInventorySort(sort)
  const oriented = dir === 'asc' ? 1 : -1
  return [...tiles].sort((left, right) => {
    const nullableOrder = compareNullableField(left, right, field)
    if (nullableOrder != null) {
      return nullableOrder
    }
    return compareTiles(left, right, field) * oriented
  })
}

/** Cycles sort direction for a column header click. */
export function toggleTileInventorySort(currentSort: string, sortField: TileInventorySortField): string {
  const { field, dir } = parseTileInventorySort(currentSort)
  if (field !== sortField) {
    return `${sortField},desc`
  }
  return dir === 'desc' ? `${sortField},asc` : `${sortField},desc`
}
