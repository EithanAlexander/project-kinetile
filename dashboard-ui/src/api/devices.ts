import { getJsonArrayStrict } from './http'
import { parseEdgeDevice } from './validate'
import type { EdgeDevice } from './types/devices'

export const DEVICES_PATH = '/api/v1/devices'

export async function fetchDevices(signal?: AbortSignal): Promise<EdgeDevice[]> {
  return getJsonArrayStrict(DEVICES_PATH, parseEdgeDevice, signal)
}
