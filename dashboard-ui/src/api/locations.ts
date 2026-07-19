import { getJsonArrayStrict } from './http'
import { parseLocationEnergyRow } from './validate'
import type { LocationEnergyRow } from './types/locations'

export const LOCATIONS_PATH = '/api/v1/energy/locations'

export async function fetchLocations(signal?: AbortSignal): Promise<LocationEnergyRow[]> {
  return getJsonArrayStrict(LOCATIONS_PATH, parseLocationEnergyRow, signal)
}
