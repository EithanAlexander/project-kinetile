export function parsePositiveNumber(raw: unknown): number | null {
  const n = Number.parseFloat(String(raw).trim())
  if (!Number.isFinite(n) || n <= 0) return null
  return n
}

export function parseNonNegativeInt(raw: unknown): number | null {
  const n = Number.parseInt(String(raw).trim(), 10)
  if (!Number.isFinite(n) || n < 0) return null
  return n
}

export function parseNonNegativeNumber(raw: unknown): number | null {
  const n = Number.parseFloat(String(raw).trim())
  if (!Number.isFinite(n) || n < 0) return null
  return n
}
