import type { TileCatalogRow } from '../../api/infrastructure'

/** One label/count pair in a chokepoint inventory breakdown. */
export interface CountBucket {
  label: string
  count: number
}

/** Aggregated chokepoint tile inventory metrics for summary panels. */
export interface TileInventorySummary {
  registeredTiles: number
  activeTiles: number
  inactiveTiles: number
  staleTiles: number
  totalCompressions: number
  byManufacturer: CountBucket[]
  bySize: CountBucket[]
  byColor: CountBucket[]
}

/** Groups tiles by a string field and returns descending count buckets. */
export function countByLabel(
  tiles: TileCatalogRow[],
  pick: (tile: TileCatalogRow) => string,
): CountBucket[] {
  const counts = new Map<string, number>()
  for (const tile of tiles) {
    const label = pick(tile).trim() || '—'
    counts.set(label, (counts.get(label) ?? 0) + 1)
  }
  return [...counts.entries()]
    .map(([label, count]) => ({ label, count }))
    .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label, undefined, { sensitivity: 'base' }))
}

/**
 * Builds summary metrics for the selected chokepoint inventory view.
 *
 * @param tiles Loaded tile rows for the current page.
 * @param isStale Predicate matching backend stale-active rules for one tile row.
 * @param registeredTiles Total registered tiles when paginated; defaults to {@code tiles.length}.
 */
export function buildTileInventorySummary(
  tiles: TileCatalogRow[],
  isStale: (tile: TileCatalogRow) => boolean,
  registeredTiles = tiles.length,
): TileInventorySummary {
  const activeTiles = tiles.filter((tile) => tile.active)
  const inactiveTiles = tiles.length - activeTiles.length
  const staleAmongActive = activeTiles.filter(isStale)

  return {
    registeredTiles,
    activeTiles: activeTiles.length,
    inactiveTiles,
    staleTiles: staleAmongActive.length,
    totalCompressions: tiles.reduce((sum, tile) => sum + tile.totalCompressions, 0),
    byManufacturer: countByLabel(tiles, (tile) => tile.manufacturerName),
    bySize: countByLabel(tiles, (tile) => tile.size),
    byColor: countByLabel(tiles, (tile) => tile.color),
  }
}
