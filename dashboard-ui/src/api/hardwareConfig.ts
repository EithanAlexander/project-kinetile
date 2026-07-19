import { physicsApiUrl } from '../config/apiBase'
import { fetchJson } from './client'
import { parseHardwareConfig } from './validate'
import type { HardwareConfig } from './types/hardware'

export type { HardwareConfig } from './types/hardware'

export const HARDWARE_CONFIG_URL = physicsApiUrl('/api/v1/config/hardware')

/** GETs hardware config from /api/v1/config/hardware; throws on missing or non-numeric fields. */
export async function fetchHardwareConfig(signal?: AbortSignal): Promise<HardwareConfig> {
  const raw = await fetchJson<unknown>(HARDWARE_CONFIG_URL, { signal })
  return parseHardwareConfig(raw)
}
