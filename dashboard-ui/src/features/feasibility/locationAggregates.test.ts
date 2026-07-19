import { describe, expect, it } from 'vitest'
import { feasibilityPercent, runtimeDays, sumRows } from './locationAggregates'

const rows = [
  {
    city: 'Tel Aviv',
    location: 'Dizengoff St',
    totalJoules: 100,
    totalWattHours: 0.5,
    totalCompressions: 40,
    successfulActivations: 30,
  },
  {
    city: 'Tel Aviv',
    location: 'Rothschild Blvd',
    totalJoules: 50,
    totalWattHours: 0.25,
    totalCompressions: 20,
    successfulActivations: 10,
  },
  {
    city: 'Haifa',
    location: 'Carmel Center',
    totalJoules: 25,
    totalWattHours: 0.125,
    totalCompressions: 8,
    successfulActivations: 5,
  },
]

describe('sumRows', () => {
  it('returns zeros for an empty list', () => {
    expect(sumRows([])).toEqual({
      totalCompressions: 0,
      successfulActivations: 0,
      totalJoules: 0,
      totalWattHours: 0,
    })
  })

  it('sums energy and compression metrics across rows', () => {
    expect(sumRows(rows)).toEqual({
      totalCompressions: 68,
      successfulActivations: 45,
      totalJoules: 175,
      totalWattHours: 0.875,
    })
  })

  it('sums a single-city scope correctly', () => {
    const telAviv = rows.filter((r) => r.city === 'Tel Aviv')
    expect(sumRows(telAviv)).toEqual({
      totalCompressions: 60,
      successfulActivations: 40,
      totalJoules: 150,
      totalWattHours: 0.75,
    })
  })

  it('ignores non-finite and missing energy values without throwing', () => {
    const messy = [
      { totalJoules: 'oops', totalWattHours: null },
      { totalJoules: 10, totalWattHours: 1, totalCompressions: -5, successfulActivations: 3 },
      null,
    ]
    expect(sumRows(messy)).toEqual({
      totalCompressions: 0,
      successfulActivations: 3,
      totalJoules: 10,
      totalWattHours: 1,
    })
  })
})

describe('runtimeDays', () => {
  const ledMarker = { id: 'led', name: 'LED marker', dailyRequiredWh: 0.01 }

  it('returns total Wh divided by daily requirement', () => {
    expect(runtimeDays(0.05, ledMarker)).toBeCloseTo(5, 6)
  })

  it('returns null when daily requirement is missing or invalid', () => {
    expect(runtimeDays(1, null)).toBeNull()
    expect(runtimeDays(1, { dailyRequiredWh: 0 })).toBeNull()
  })
})

describe('feasibilityPercent', () => {
  it('returns load coverage as a percentage of daily need', () => {
    expect(feasibilityPercent(0.05, { dailyRequiredWh: 0.01 })).toBeCloseTo(500, 6)
  })
})
