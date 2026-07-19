/** One persisted tile compression row from the energy ledger API. */
export interface LedgerRow {
  id?: string | number | null
  eventTimestamp?: string | null
  location?: string | null
  city?: string | null
  tileId?: string | null
  manufacturerName?: string | null
  eventId?: string | null
  massKg?: number | null
  impactMultiplier?: number | null
  calculatedForceNewtons?: number | null
  calculatedEnergyJoules?: number | null
  activationSuccessful?: boolean | null
}

/** Raw Spring-style page payload before normalization. */
export interface LedgerPageRaw {
  totalPages?: number
  page?: number
  content?: unknown[]
  totalElements?: number
}

/** Normalized ledger page used by the UI; `clampToPage` requests a page correction when out of range. */
export interface LedgerPageParsed {
  clampToPage: number | null
  content: LedgerRow[]
  totalElements: number
  totalPages: number
}

/** Returns a row's event timestamp, or `null` when absent. */
export function rowTimestamp(row: LedgerRow): string | null {
  return row.eventTimestamp ?? null
}
