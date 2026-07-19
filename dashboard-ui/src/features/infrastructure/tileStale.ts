import type { TileCatalogRow } from '../../api/infrastructure'

const MS_PER_DAY = 86_400_000

/**
 * Whether an active tile is stale per backend rules:
 * no compression, or last compression before {@code now - thresholdDays}.
 */
export function isStaleActiveTile(
  tile: Pick<TileCatalogRow, 'active' | 'lastCompressionAt'>,
  thresholdDays: number,
  nowMs: number = Date.now(),
): boolean {
  if (!tile.active) return false
  if (!tile.lastCompressionAt) return true
  const lastMs = new Date(tile.lastCompressionAt).getTime()
  if (Number.isNaN(lastMs)) return true
  const thresholdMs = nowMs - thresholdDays * MS_PER_DAY
  return lastMs < thresholdMs
}
