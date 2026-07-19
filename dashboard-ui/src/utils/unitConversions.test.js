import { describe, expect, it } from 'vitest'
import { kgToPounds, metersPerSecondToKmh, mphToKmh, poundsToKg } from './unitConversions.js'

describe('poundsToKg', () => {
  it('converts pounds to kilograms using the exact factor', () => {
    expect(poundsToKg(1)).toBeCloseTo(0.45359237, 8)
    expect(poundsToKg(10)).toBeCloseTo(4.5359237, 7)
  })

  it('returns 0 for 0 lb', () => {
    expect(poundsToKg(0)).toBe(0)
  })

  it('coerces numeric strings', () => {
    expect(poundsToKg('2')).toBeCloseTo(0.90718474, 8)
  })

  it('returns NaN for non-numeric input', () => {
    expect(poundsToKg('abc')).toBeNaN()
  })
})

describe('kgToPounds', () => {
  it('converts kilograms to pounds (inverse of poundsToKg)', () => {
    expect(kgToPounds(1)).toBeCloseTo(2.20462262, 6)
    expect(kgToPounds(poundsToKg(10))).toBeCloseTo(10, 6)
  })

  it('returns 0 for 0 kg', () => {
    expect(kgToPounds(0)).toBe(0)
  })

  it('returns NaN for non-numeric input', () => {
    expect(kgToPounds('abc')).toBeNaN()
  })
})

describe('metersPerSecondToKmh', () => {
  it('converts m/s to km/h using the 3.6 factor', () => {
    expect(metersPerSecondToKmh(1)).toBeCloseTo(3.6, 6)
    expect(metersPerSecondToKmh(1.4)).toBeCloseTo(5.04, 6)
  })

  it('returns 0 for 0 m/s', () => {
    expect(metersPerSecondToKmh(0)).toBe(0)
  })

  it('returns NaN for non-numeric input', () => {
    expect(metersPerSecondToKmh('abc')).toBeNaN()
  })
})

describe('mphToKmh', () => {
  it('converts mph to km/h using the international mile factor', () => {
    expect(mphToKmh(1)).toBeCloseTo(1.609344, 6)
    expect(mphToKmh(3.1)).toBeCloseTo(4.9889664, 6)
  })

  it('returns 0 for 0 mph', () => {
    expect(mphToKmh(0)).toBe(0)
  })

  it('returns NaN for non-numeric input', () => {
    expect(mphToKmh('abc')).toBeNaN()
  })
})
