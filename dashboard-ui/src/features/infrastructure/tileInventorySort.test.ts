import { describe, expect, it } from 'vitest'
import type { TileCatalogRow } from '../../api/infrastructure'
import {
  DEFAULT_TILE_INVENTORY_SORT,
  sortTileRows,
  toggleTileInventorySort,
} from './tileInventorySort'

function tile(partial: Partial<TileCatalogRow> & Pick<TileCatalogRow, 'tileId'>): TileCatalogRow {
  return {
    manufacturerName: 'Aslan',
    size: '600x600',
    color: 'Slate',
    installationDate: '2025-01-01',
    removalDate: null,
    lastInspectionDate: '2025-02-01',
    active: true,
    lastCompressionAt: '2025-06-01T12:00:00Z',
    totalCompressions: 10,
    ...partial,
  }
}

const rows: TileCatalogRow[] = [
  tile({ tileId: 'b', manufacturerName: 'GFS', totalCompressions: 5, lastCompressionAt: '2025-06-10T12:00:00Z' }),
  tile({ tileId: 'a', manufacturerName: 'Aslan', totalCompressions: 20, lastCompressionAt: null }),
  tile({ tileId: 'c', manufacturerName: 'Æ Inc', totalCompressions: 12, lastCompressionAt: '2025-05-01T12:00:00Z' }),
]

describe('sortTileRows', () => {
  it('sorts by tile id ascending by default token', () => {
    expect(sortTileRows(rows, DEFAULT_TILE_INVENTORY_SORT).map((row) => row.tileId)).toEqual([
      'a',
      'b',
      'c',
    ])
  })

  it('sorts compressions descending', () => {
    expect(sortTileRows(rows, 'compressions,desc').map((row) => row.totalCompressions)).toEqual([20, 12, 5])
  })

  it('puts missing last compression last when sorting recent first', () => {
    expect(sortTileRows(rows, 'lastCompression,desc').map((row) => row.tileId)).toEqual(['b', 'c', 'a'])
  })
})

describe('toggleTileInventorySort', () => {
  it('switches to a new column in descending order', () => {
    expect(toggleTileInventorySort('tileId,asc', 'lastCompression')).toBe('lastCompression,desc')
  })

  it('flips direction on the active column', () => {
    expect(toggleTileInventorySort('lastCompression,desc', 'lastCompression')).toBe('lastCompression,asc')
    expect(toggleTileInventorySort('lastCompression,asc', 'lastCompression')).toBe('lastCompression,desc')
  })
})
