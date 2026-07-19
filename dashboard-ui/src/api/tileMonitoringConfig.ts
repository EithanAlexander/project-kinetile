import { physicsApiUrl } from '../config/apiBase'
import { fetchJson } from './client'
import { parseTileMonitoringConfig } from './validate'
import type { TileMonitoringConfig } from './types/tileMonitoring'

export type { TileMonitoringConfig } from './types/tileMonitoring'

export const TILE_MONITORING_CONFIG_URL = physicsApiUrl('/api/v1/config/tile-monitoring')

/** GETs tile monitoring config; throws on missing or non-numeric fields. */
export async function fetchTileMonitoringConfig(signal?: AbortSignal): Promise<TileMonitoringConfig> {
  const raw = await fetchJson<unknown>(TILE_MONITORING_CONFIG_URL, { signal })
  return parseTileMonitoringConfig(raw)
}
