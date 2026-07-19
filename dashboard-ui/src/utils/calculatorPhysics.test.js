import { describe, expect, it } from 'vitest'
import {
  calculateActivationHarvest,
  calculateForceNewtons,
  joulesPerActivation,
  minimumActivationMassKg,
  suggestImpactMultiplier,
} from './calculatorPhysics.js'

const HARDWARE = {
  activationThresholdNewtons: 100,
  minRatedOutputJoules: 2,
  maxRatedOutputJoules: 5,
  maxScaleMassKg: 90,
  gravity: 9.81,
}

describe('calculateForceNewtons', () => {
  it('returns mass × gravity × impact multiplier', () => {
    expect(calculateForceNewtons(80, 1, 9.81)).toBeCloseTo(784.8, 1)
  })

  it('returns 0 for invalid inputs', () => {
    expect(calculateForceNewtons(0, 1, 9.81)).toBe(0)
    expect(calculateForceNewtons(80, 0.9, 9.81)).toBe(0)
  })
})

describe('joulesPerActivation', () => {
  it('returns ~4.62 J for 80 kg at 1.0× impact', () => {
    expect(joulesPerActivation(80, 1, HARDWARE)).toBeCloseTo(4.624, 2)
  })

  it('returns ~4.56 J for 65 kg at 1.2× impact', () => {
    expect(joulesPerActivation(65, 1.2, HARDWARE)).toBeCloseTo(4.56, 2)
  })

  it('returns ~2 J at threshold mass (~10.2 kg)', () => {
    expect(joulesPerActivation(10.2, 1, HARDWARE)).toBeCloseTo(2.0, 1)
  })

  it('caps at 5 J for heavy walkers', () => {
    expect(joulesPerActivation(95, 1, HARDWARE)).toBe(5)
  })

  it('reaches max J when effective load hits cap (80 kg, 1.5×)', () => {
    expect(joulesPerActivation(80, 1.5, HARDWARE)).toBe(5)
  })

  it('returns 0 J below threshold', () => {
    expect(joulesPerActivation(8, 1, HARDWARE)).toBe(0)
  })
})

describe('calculateActivationHarvest', () => {
  it('returns scaled J per compression when force meets threshold (80 kg)', () => {
    const r = calculateActivationHarvest(80, 1, 1000, HARDWARE)
    expect(r.activationSuccessful).toBe(true)
    expect(r.joulesPerActivation).toBeCloseTo(4.624, 2)
    expect(r.totalJoules).toBeCloseTo(4624, 0)
    expect(r.successfulActivations).toBe(1000)
    expect(r.totalCompressions).toBe(1000)
  })

  it('returns 0 J when below threshold (8 kg)', () => {
    const r = calculateActivationHarvest(8, 1, 100, HARDWARE)
    expect(r.activationSuccessful).toBe(false)
    expect(r.totalJoules).toBe(0)
    expect(r.successfulActivations).toBe(0)
  })

  it('1000 activations at 80 kg ≈ 1.29 Wh', () => {
    const r = calculateActivationHarvest(80, 1, 1000, HARDWARE)
    expect(r.totalJoules / 3600).toBeCloseTo(1.286, 2)
  })
})

describe('suggestImpactMultiplier', () => {
  it('grows with walking speed within the 1.0-1.5 band', () => {
    expect(suggestImpactMultiplier(5).multiplier).toBeCloseTo(1.12, 2)
    expect(suggestImpactMultiplier(6.5).multiplier).toBeCloseTo(1.21, 2)
    expect(suggestImpactMultiplier(8).multiplier).toBeCloseTo(1.3, 2)
  })

  it('never drops below 1.0 for a slow shuffle', () => {
    const r = suggestImpactMultiplier(1)
    expect(r.multiplier).toBe(1)
    expect(r.capped).toBe(false)
  })

  it('caps at 1.5 and flags running speeds', () => {
    const r = suggestImpactMultiplier(15)
    expect(r.multiplier).toBe(1.5)
    expect(r.capped).toBe(true)
  })

  it('returns null for invalid or non-positive speeds', () => {
    expect(suggestImpactMultiplier(0)).toBeNull()
    expect(suggestImpactMultiplier(-2)).toBeNull()
    expect(suggestImpactMultiplier('abc')).toBeNull()
  })
})

describe('minimumActivationMassKg', () => {
  it('returns threshold / (gravity × impact)', () => {
    expect(minimumActivationMassKg(HARDWARE, 1)).toBeCloseTo(10.19, 2)
  })

  it('requires less mass for a harder step', () => {
    expect(minimumActivationMassKg(HARDWARE, 1.5)).toBeCloseTo(6.8, 1)
  })

  it('returns Infinity for invalid inputs', () => {
    expect(minimumActivationMassKg(HARDWARE, 0)).toBe(Infinity)
    expect(minimumActivationMassKg({ activationThresholdNewtons: 0, gravity: 9.81 }, 1)).toBe(
      Infinity,
    )
  })
})
