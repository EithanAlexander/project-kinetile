import { describe, expect, it } from 'vitest'
import { isStaleActiveTile } from './tileStale'

const NOW = Date.parse('2026-07-06T12:00:00Z')

describe('isStaleActiveTile', () => {
  it('returns false for inactive tiles', () => {
    expect(
      isStaleActiveTile(
        { active: false, lastCompressionAt: null },
        5,
        NOW,
      ),
    ).toBe(false)
  })

  it('returns true when active with no compression', () => {
    expect(
      isStaleActiveTile(
        { active: true, lastCompressionAt: null },
        5,
        NOW,
      ),
    ).toBe(true)
  })

  it('returns true when last compression is before the threshold instant', () => {
    expect(
      isStaleActiveTile(
        { active: true, lastCompressionAt: '2026-06-30T11:59:59Z' },
        5,
        NOW,
      ),
    ).toBe(true)
  })

  it('returns false when last compression is within the threshold window', () => {
    expect(
      isStaleActiveTile(
        { active: true, lastCompressionAt: '2026-07-05T12:00:01Z' },
        5,
        NOW,
      ),
    ).toBe(false)
  })
})
