import { describe, expect, it } from 'vitest'
import type { TileCatalogRow } from '../../api/infrastructure'
import { buildTileInventorySummary, countByLabel } from './tileInventorySummary'
import { isStaleActiveTile } from './tileStale'

const THRESHOLD_DAYS = 5
const NOW = Date.parse('2026-07-06T12:00:00Z')
const isStale = (tile: TileCatalogRow) => isStaleActiveTile(tile, THRESHOLD_DAYS, NOW)

function tile(partial: Partial<TileCatalogRow> & Pick<TileCatalogRow, 'tileId'>): TileCatalogRow {
  return {
    manufacturerName: 'Aslan',
    size: '60×60 cm',
    color: 'Slate',
    installationDate: '2024-01-01',
    removalDate: null,
    lastInspectionDate: '2024-06-01',
    active: true,
    lastCompressionAt: '2026-07-05T12:00:00Z',
    totalCompressions: 10,
    ...partial,
  }
}

const sampleTiles: TileCatalogRow[] = [
  tile({ tileId: 'a', manufacturerName: 'Aslan', size: '60×60 cm', color: 'Slate', totalCompressions: 12 }),
  tile({ tileId: 'b', manufacturerName: 'Aslan', size: '60×60 cm', color: 'Graphite', totalCompressions: 8 }),
  tile({
    tileId: 'c',
    manufacturerName: 'Æ Inc',
    size: '45×45 cm',
    color: 'Graphite',
    active: false,
    totalCompressions: 0,
    lastCompressionAt: null,
  }),
]

describe('countByLabel', () => {
  it('groups tiles and sorts by descending count', () => {
    expect(countByLabel(sampleTiles, (row) => row.manufacturerName)).toEqual([
      { label: 'Aslan', count: 2 },
      { label: 'Æ Inc', count: 1 },
    ])
  })
})

describe('buildTileInventorySummary', () => {
  it('aggregates headline metrics and breakdown buckets', () => {
    const summary = buildTileInventorySummary(sampleTiles, isStale, 5)
    expect(summary.registeredTiles).toBe(5)
    expect(summary.activeTiles).toBe(2)
    expect(summary.inactiveTiles).toBe(1)
    expect(summary.staleTiles).toBe(0)
    expect(summary.totalCompressions).toBe(20)
    expect(summary.bySize).toEqual([
      { label: '60×60 cm', count: 2 },
      { label: '45×45 cm', count: 1 },
    ])
    expect(summary.byColor).toEqual([
      { label: 'Graphite', count: 2 },
      { label: 'Slate', count: 1 },
    ])
  })

  it('counts stale tiles among active tiles only', () => {
    const staleTile = tile({
      tileId: 'stale',
      lastCompressionAt: '2020-01-01T00:00:00Z',
    })
    const summary = buildTileInventorySummary([...sampleTiles, staleTile], isStale)
    expect(summary.activeTiles).toBe(3)
    expect(summary.staleTiles).toBe(1)
  })
})
