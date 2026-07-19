import { physicsApiUrl } from '../config/apiBase'
import { fetchJson } from './client'

/**
 * GET JSON from a physics-engine API path (relative to {@link physicsApiUrl}).
 */
export function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  const p = path.startsWith('/') ? path : `/${path}`
  return fetchJson<T>(physicsApiUrl(p), { signal })
}

/**
 * GET a JSON array, parsing each element; skips entries the parser rejects.
 */
export async function getJsonArray<T>(
  path: string,
  parseItem: (value: unknown) => T | null,
  signal?: AbortSignal,
): Promise<T[]> {
  const data = await getJson<unknown>(path, signal)
  if (!Array.isArray(data)) return []
  const out: T[] = []
  for (const item of data) {
    const parsed = parseItem(item)
    if (parsed != null) out.push(parsed)
  }
  return out
}

/**
 * GET a JSON array where every element must parse; throws if any item is invalid.
 */
export async function getJsonArrayStrict<T>(
  path: string,
  parseItem: (value: unknown) => T | null,
  signal?: AbortSignal,
): Promise<T[]> {
  const data = await getJson<unknown>(path, signal)
  if (!Array.isArray(data)) {
    throw new TypeError('Expected a JSON array from the server')
  }
  const out: T[] = []
  for (const item of data) {
    const parsed = parseItem(item)
    if (parsed == null) {
      throw new Error('Server returned malformed data')
    }
    out.push(parsed)
  }
  return out
}
