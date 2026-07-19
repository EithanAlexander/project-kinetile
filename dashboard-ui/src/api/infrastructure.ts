import { getJson } from './http'

/** Place type metadata used to classify chokepoints by context and expected traffic. */
export interface PlaceTypeRow {
  id: number
  code: string
  label: string
  trafficTier: string
}

/** Tile manufacturer catalog row with a lightweight activity summary. */
export interface ManufacturerRow {
  id: number
  name: string
  activeTileCount: number
}

/** City catalog entry with aggregate counts used by infrastructure dashboards. */
export interface CityCatalogRow {
  id: number
  code: string
  name: string
  chokepointCount: number
  activeTileCount: number
}

/** Chokepoint catalog row scoped to a city. */
export interface ChokepointCatalogRow {
  id: number
  code: string
  name: string
  placeTypeCode: string
  placeTypeLabel: string
  trafficTier: string
  activeTileCount: number
}

/** Paginated tile list row returned for a chokepoint inventory query. */
export interface TileCatalogRow {
  tileId: string
  manufacturerName: string
  size: string
  color: string
  installationDate: string
  removalDate: string | null
  lastInspectionDate: string
  active: boolean
  lastCompressionAt: string | null
  totalCompressions: number
}

/** Expanded tile row used by stale-tile monitoring views. */
export interface TileDetailRow extends TileCatalogRow {
  cityName: string
  cityCode: string
  chokepointName: string
  chokepointCode: string
  placeTypeCode: string
  trafficTier: string
  firstCompressionAt: string | null
}

/** Standard Spring-style page wrapper for tile inventory results. */
export interface TilePage {
  content: TileCatalogRow[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

/** Fetches static place-type lookup values. */
export function fetchPlaceTypes(signal?: AbortSignal): Promise<PlaceTypeRow[]> {
  return getJson<PlaceTypeRow[]>('/api/v1/infrastructure/place-types', signal)
}

/** Fetches known tile manufacturers and their currently active tile counts. */
export function fetchManufacturers(signal?: AbortSignal): Promise<ManufacturerRow[]> {
  return getJson<ManufacturerRow[]>('/api/v1/infrastructure/manufacturers', signal)
}

/** Fetches city-level infrastructure catalog data. */
export function fetchCitiesCatalog(signal?: AbortSignal): Promise<CityCatalogRow[]> {
  return getJson<CityCatalogRow[]>('/api/v1/infrastructure/cities', signal)
}

/** Fetches chokepoints for a specific city id. */
export function fetchChokepoints(cityId: number, signal?: AbortSignal): Promise<ChokepointCatalogRow[]> {
  return getJson<ChokepointCatalogRow[]>(`/api/v1/infrastructure/cities/${cityId}/chokepoints`, signal)
}

/** Fetches one paginated tile inventory page for a chokepoint. */
export function fetchTilesForChokepoint(
  chokepointId: number,
  page = 0,
  size = 50,
  signal?: AbortSignal,
): Promise<TilePage> {
  return getJson<TilePage>(
    `/api/v1/infrastructure/chokepoints/${chokepointId}/tiles?page=${page}&size=${size}`,
    signal,
  )
}

/** Fetches tiles flagged as stale by inspection/compression recency rules. */
export function fetchStaleTiles(signal?: AbortSignal): Promise<TileDetailRow[]> {
  return getJson<TileDetailRow[]>('/api/v1/infrastructure/tiles/stale', signal)
}

/** Shortens long identifiers for compact UI display while preserving a leading prefix. */
export function truncateUuid(value: string | null | undefined, visible = 8): string {
  if (!value) return '—'
  if (value.length <= visible + 3) return value
  return `${value.slice(0, visible)}…`
}
