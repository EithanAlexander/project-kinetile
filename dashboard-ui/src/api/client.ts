/**
 * Shared JSON GET helper; forwards {@link RequestInit.signal} for query cancellation.
 *
 * <p>The physics engine API is open (no authentication); requests carry no credentials.
 */

const SAFE_HTTP_MESSAGES: Record<number, string> = {
  400: 'The request could not be processed. Check your filters and try again. 🔍',
  401: 'You are not authorized to access this data. 🔒',
  403: 'Access to this resource was denied. 🚫',
  404: 'The requested data was not found. 👻',
  429: 'Too many requests — please wait a moment. 🐢',
  500: 'The server encountered an error. Please try again later. 💥',
  502: 'The service is temporarily unavailable. 🌩️',
  503: 'The service is temporarily unavailable. 🌩️',
}

/** Thrown by {@link fetchJson} for non-OK responses; carries status for safe UI mapping. */
export class HttpError extends Error {
  readonly status: number

  constructor(status: number) {
    super(`HTTP request failed (${status})`)
    this.name = 'HttpError'
    this.status = status
  }
}

import { ApiValidationError } from './validate'

/**
 * Maps HTTP status codes and errors to user-safe messages (no response body parsing).
 */
export function toUserSafeError(error: unknown, fallback = 'Failed to load data'): string {
  if (error instanceof HttpError) {
    return SAFE_HTTP_MESSAGES[error.status] ?? fallback
  }
  if (error instanceof ApiValidationError) {
    return 'The server returned data in an unexpected format.'
  }
  return fallback
}

export async function fetchJson<T>(input: string, init?: RequestInit): Promise<T> {
  const res = await fetch(input, init)
  if (!res.ok) {
    throw new HttpError(res.status)
  }
  return res.json() as Promise<T>
}
