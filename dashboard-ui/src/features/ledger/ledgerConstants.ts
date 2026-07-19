export const TIME_PRESETS = [
  { id: 'all', label: 'Default window (last 24 months)' },
  { id: '1d', label: 'Last 24 hours' },
  { id: '2d', label: 'Last 2 days' },
  { id: '7d', label: 'Last 7 days' },
  { id: '30d', label: 'Last 30 days' },
] as const

export const PAGE_SIZES = [10, 50, 100, 200] as const

/** Ledger sort fields accepted by the physics API and column-header toggles. */
export const LEDGER_SORT_FIELDS = [
  'eventId',
  'eventTimestamp',
  'city',
  'location',
  'tile',
  'brand',
  'activation',
  'energy',
  'force',
  'mass',
  'impact',
] as const

export type LedgerSortField = (typeof LEDGER_SORT_FIELDS)[number]

/** Normalizes a `field,direction` sort string to a supported ledger sort. */
export function normalizeLedgerSort(sort: string): string {
  const [rawField, rawDir] = sort.split(',', 2)
  const field = rawField?.trim()
  const direction = rawDir?.trim().toLowerCase() === 'asc' ? 'asc' : 'desc'
  if (field && (LEDGER_SORT_FIELDS as readonly string[]).includes(field)) {
    return `${field},${direction}`
  }
  return 'eventTimestamp,desc'
}

export const SORT_OPTIONS = [
  { value: 'eventTimestamp,desc', label: 'Newest first' },
  { value: 'eventTimestamp,asc', label: 'Oldest first' },
  { value: 'eventId,asc', label: 'Event ID (A → Z)' },
  { value: 'eventId,desc', label: 'Event ID (Z → A)' },
  { value: 'city,asc', label: 'City (A → Z)' },
  { value: 'location,asc', label: 'Location (A → Z)' },
  { value: 'tile,asc', label: 'Tile ID (A → Z)' },
  { value: 'brand,asc', label: 'Brand (A → Z)' },
  { value: 'activation,desc', label: 'Successful activations first' },
  { value: 'energy,desc', label: 'Highest energy (J)' },
  { value: 'energy,asc', label: 'Lowest energy (J)' },
  { value: 'force,desc', label: 'Highest force (N)' },
  { value: 'mass,desc', label: 'Highest mass (kg)' },
  { value: 'impact,desc', label: 'Highest impact multiplier' },
] as const

export type TimePresetId = (typeof TIME_PRESETS)[number]['id']
