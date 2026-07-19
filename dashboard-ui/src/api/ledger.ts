import { physicsApiUrl } from '../config/apiBase'
import type { LedgerPageRaw } from './types/ledger'
import { fetchJson } from './client'

export const LEDGER_API_BASE = physicsApiUrl('/api/v1/energy/ledger')

export async function fetchLedgerPageRaw(
  url: string,
  signal?: AbortSignal,
): Promise<LedgerPageRaw> {
  return fetchJson<LedgerPageRaw>(url, { signal })
}
