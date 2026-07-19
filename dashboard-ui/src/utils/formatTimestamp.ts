/** Coerce API timestamp values to text without object default stringification. */
function toTimestampText(iso: unknown): string | null {
  if (iso == null || iso === '') return null
  if (typeof iso === 'string') return iso
  if (typeof iso === 'number') return Number.isNaN(iso) ? null : String(iso)
  if (iso instanceof Date) return Number.isNaN(iso.getTime()) ? null : iso.toISOString()
  return null
}

/** Formats an ISO-like timestamp for ledger and tables. */
export function formatTimestamp(iso: unknown): string {
  const text = toTimestampText(iso)
  if (text == null) return '—'
  const d = new Date(text)
  return Number.isNaN(d.getTime()) ? text : d.toLocaleString()
}

/** Compact timestamp for dense tables (ledger rows). */
export function formatTimestampCompact(iso: unknown): string {
  const text = toTimestampText(iso)
  if (text == null) return '—'
  const d = new Date(text)
  if (Number.isNaN(d.getTime())) return text
  return d.toLocaleString(undefined, {
    month: 'numeric',
    day: 'numeric',
    year: '2-digit',
    hour: 'numeric',
    minute: '2-digit',
  })
}

const MS_PER_DAY = 86_400_000

/** Whole days elapsed since an ISO-like timestamp; {@code null} when unknown. */
export function daysSinceTimestamp(iso: unknown, nowMs: number = Date.now()): number | null {
  const text = toTimestampText(iso)
  if (text == null) return null
  const then = new Date(text)
  if (Number.isNaN(then.getTime())) return null
  const elapsedMs = nowMs - then.getTime()
  if (elapsedMs < 0) return 0
  return Math.floor(elapsedMs / MS_PER_DAY)
}

/** Formats elapsed whole days for inventory tables. */
export function formatDaysSinceLastCompression(iso: unknown, nowMs: number = Date.now()): string {
  const days = daysSinceTimestamp(iso, nowMs)
  if (days == null) return '—'
  return String(days)
}
