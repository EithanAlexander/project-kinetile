import { physicsApiUrl } from '../config/apiBase'
import { fetchJson } from './client'
import { parseTimeseriesRowList } from './validate'
import type { TimeseriesRow } from './types/timeseries'

export const TIMESERIES_DAILY_BY_CITY_URL = physicsApiUrl(
  '/api/v1/energy/timeseries/daily/by-city',
)
export const TIMESERIES_DAILY_BY_LOCATION_URL = physicsApiUrl(
  '/api/v1/energy/timeseries/daily/by-location',
)

/** GETs timeseries rows from the given API endpoint (daily by city or location).
 * Throws if the root is not an array or any row is invalid. */
export async function fetchTimeseriesRows(
  url: string,
  signal?: AbortSignal,
): Promise<TimeseriesRow[]> {
  const data = await fetchJson<unknown>(url, { signal })
  return parseTimeseriesRowList(data)
}
