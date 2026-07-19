import { describe, expect, it } from 'vitest'
import {
  daysSinceTimestamp,
  formatDaysSinceLastCompression,
  formatTimestamp,
  formatTimestampCompact,
} from './formatTimestamp'

describe('formatTimestamp', () => {
  it('returns em dash for nullish and empty input', () => {
    expect(formatTimestamp(null)).toBe('—')
    expect(formatTimestamp(undefined)).toBe('—')
    expect(formatTimestamp('')).toBe('—')
  })

  it('returns em dash for unsupported object input', () => {
    expect(formatTimestamp({ when: '2026-01-01' })).toBe('—')
  })

  it('formats valid ISO strings', () => {
    expect(formatTimestamp('2026-06-14T12:00:00.000Z')).not.toBe('—')
  })

  it('returns raw text when string is not parseable as a date', () => {
    expect(formatTimestamp('not-a-date')).toBe('not-a-date')
  })
})

describe('formatTimestampCompact', () => {
  it('returns em dash for unsupported object input', () => {
    expect(formatTimestampCompact({})).toBe('—')
  })

  it('formats valid ISO strings compactly', () => {
    expect(formatTimestampCompact('2026-06-14T12:00:00.000Z')).toMatch(/\d/)
  })
})

describe('daysSinceTimestamp', () => {
  it('returns null for missing timestamps', () => {
    expect(daysSinceTimestamp(null)).toBeNull()
    expect(formatDaysSinceLastCompression(undefined)).toBe('—')
  })

  it('returns whole days elapsed from a fixed clock', () => {
    const now = Date.parse('2026-07-06T12:00:00.000Z')
    expect(daysSinceTimestamp('2026-07-01T12:00:00.000Z', now)).toBe(5)
    expect(formatDaysSinceLastCompression('2026-07-01T12:00:00.000Z', now)).toBe('5')
  })

  it('never returns negative days for future timestamps', () => {
    const now = Date.parse('2026-07-06T12:00:00.000Z')
    expect(daysSinceTimestamp('2026-07-10T12:00:00.000Z', now)).toBe(0)
  })
})
