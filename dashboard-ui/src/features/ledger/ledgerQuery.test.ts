import { describe, expect, it } from 'vitest'
import {
  buildLedgerQueryUrl,
  parseLedgerPagePayload,
  resolveLedgerDisplay,
  type LedgerQueryState,
} from './ledgerQuery'
import { normalizeLedgerSort } from './ledgerConstants'

function baseState(over: Partial<LedgerQueryState> = {}): LedgerQueryState {
  return {
    page: 0,
    pageSize: 50,
    sort: 'eventTimestamp,desc',
    timePreset: 'all',
    locationContains: '',
    eventIdPrefix: '',
    minEnergyJoules: '',
    maxEnergyJoules: '',
    minPowerWatts: '',
    maxPowerWatts: '',
    minImpactMultiplier: '',
    maxImpactMultiplier: '',
    activationOnly: false,
    ...over,
  }
}

describe('buildLedgerQueryUrl', () => {
  it('includes pagination and sort', () => {
    const url = buildLedgerQueryUrl(baseState({ page: 2, pageSize: 10, sort: 'energy,desc' }))
    expect(url).toContain('page=2')
    expect(url).toContain('size=10')
    expect(url).toContain('sort=energy%2Cdesc')
  })

  it('keeps optional filters empty but applies default 24-month window', () => {
    const url = buildLedgerQueryUrl(baseState())
    expect(url).not.toContain('type=')
    expect(url).toContain('since=')
    expect(url).not.toContain('activationOnly=')
  })

  it('falls back to default sort when value is not allowlisted', () => {
    const url = buildLedgerQueryUrl(baseState({ sort: 'invalid,asc' }))
    expect(url).toContain('sort=eventTimestamp%2Cdesc')
  })

  it('accepts column-header sort fields not listed in the dropdown', () => {
    const url = buildLedgerQueryUrl(baseState({ sort: 'location,asc' }))
    expect(url).toContain('sort=location%2Casc')
  })

  it('truncates long filter strings', () => {
    const url = buildLedgerQueryUrl(
      baseState({ locationContains: 'x'.repeat(200) }),
    )
    const match = /locationContains=([^&]+)/.exec(url)
    expect(match).not.toBeNull()
    expect(decodeURIComponent(match![1]).length).toBe(128)
  })

  it('adds filters when set', () => {
    const url = buildLedgerQueryUrl(
      baseState({
        timePreset: '7d',
        locationContains: 'Main',
        eventIdPrefix: 'e-',
        minEnergyJoules: '1',
        maxEnergyJoules: '100',
        minPowerWatts: '2',
        maxPowerWatts: '99',
        minImpactMultiplier: '1.0',
        maxImpactMultiplier: '1.5',
        activationOnly: true,
      }),
    )
    expect(url).toContain('locationContains=Main')
    expect(url).toContain('eventIdPrefix=e-')
    expect(url).toContain('minEnergyJoules=2')
    expect(url).toContain('maxEnergyJoules=99')
    expect(url).toContain('minImpactMultiplier=1')
    expect(url).toContain('maxImpactMultiplier=1.5')
    expect(url).toContain('activationOnly=true')
    expect(url).toMatch(/since=/)
  })
})

describe('normalizeLedgerSort', () => {
  it('keeps supported field and direction', () => {
    expect(normalizeLedgerSort('brand,asc')).toBe('brand,asc')
    expect(normalizeLedgerSort('tile,desc')).toBe('tile,desc')
  })

  it('defaults invalid fields to newest first', () => {
    expect(normalizeLedgerSort('bogus,asc')).toBe('eventTimestamp,desc')
  })
})

describe('parseLedgerPagePayload', () => {
  it('returns clamp when server page is out of range', () => {
    const r = parseLedgerPagePayload({
      totalPages: 3,
      page: 3,
      content: [{ id: 1 }],
      totalElements: 100,
    })
    expect(r.clampToPage).toBe(2)
    expect(r.content).toEqual([])
  })

  it('returns content when page is valid', () => {
    const row = { id: 'a', calculatedEnergyJoules: 3600 }
    const r = parseLedgerPagePayload({
      totalPages: 2,
      page: 0,
      content: [row],
      totalElements: 1,
    })
    expect(r.clampToPage).toBeNull()
    expect(r.content).toEqual([row])
    expect(r.totalElements).toBe(1)
    expect(r.totalPages).toBe(2)
  })

  it('handles missing fields', () => {
    const r = parseLedgerPagePayload({})
    expect(r.content).toEqual([])
    expect(r.totalElements).toBe(0)
    expect(r.clampToPage).toBeNull()
  })
})

describe('resolveLedgerDisplay', () => {
  const goodPage = {
    clampToPage: null,
    content: [{ id: '1' }],
    totalElements: 1,
    totalPages: 1,
  }

  it('returns empty page on error', () => {
    const r = resolveLedgerDisplay(goodPage, new Error('fail'), goodPage)
    expect(r.content).toEqual([])
    expect(r.totalElements).toBe(0)
  })

  it('returns last good when clamp requested', () => {
    const clamped = { clampToPage: 0, content: [], totalElements: 0, totalPages: 0 }
    const r = resolveLedgerDisplay(clamped, null, goodPage)
    expect(r).toEqual(goodPage)
  })

  it('returns current data when valid', () => {
    const r = resolveLedgerDisplay(goodPage, null, null)
    expect(r).toEqual(goodPage)
  })
})
