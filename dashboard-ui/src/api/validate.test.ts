import { describe, expect, it } from 'vitest'
import {
  ApiValidationError,
  parseEdgeDevice,
  parseEdgeDeviceList,
  parseHardwareConfig,
  parseLocationEnergyRow,
  parseLocationEnergyList,
  parseTimeseriesRow,
  parseTimeseriesRowList,
} from './validate'

describe('parseEdgeDevice', () => {
  it('accepts valid device', () => {
    expect(parseEdgeDevice({ id: 'led', name: 'LED', dailyRequiredWh: 6 })).toEqual({
      id: 'led',
      name: 'LED',
      dailyRequiredWh: 6,
    })
  })

  it('rejects missing fields', () => {
    expect(parseEdgeDevice({ id: 'x' })).toBeNull()
  })
})

describe('parseEdgeDeviceList', () => {
  it('parses array', () => {
    expect(parseEdgeDeviceList([{ id: 'a', name: 'A', dailyRequiredWh: 1 }])).toHaveLength(1)
  })

  it('throws on malformed array', () => {
    expect(() => parseEdgeDeviceList([{ bad: true }])).toThrow(ApiValidationError)
  })
})

describe('parseLocationEnergyRow', () => {
  it('accepts valid row', () => {
    expect(
      parseLocationEnergyRow({
        city: 'Tel Aviv',
        location: 'Station',
        totalJoules: 100,
        totalWattHours: 0.028,
        totalCompressions: 10,
        successfulActivations: 8,
      }),
    ).toMatchObject({ city: 'Tel Aviv', totalCompressions: 10 })
  })
})

describe('parseLocationEnergyList', () => {
  it('throws when any row invalid', () => {
    expect(() => parseLocationEnergyList([{ city: 'X' }])).toThrow(ApiValidationError)
  })
})

describe('parseTimeseriesRowList', () => {
  it('skips bad rows', () => {
    const rows = parseTimeseriesRowList([
      {
        bucketStart: '2025-01-01T00:00:00Z',
        city: 'A',
        totalJoules: 1,
        totalWattHours: 0.001,
        totalCompressions: 1,
        successfulActivations: 1,
      },
      { invalid: true },
    ])
    expect(rows).toHaveLength(1)
  })

  it('parseTimeseriesRow returns null for invalid', () => {
    expect(parseTimeseriesRow(null)).toBeNull()
  })
})

describe('parseHardwareConfig', () => {
  it('parses full config', () => {
    expect(
      parseHardwareConfig({
        activationThresholdNewtons: 100,
        minRatedOutputJoules: 2,
        maxRatedOutputJoules: 5,
        maxScaleMassKg: 90,
        maxDisplacementMeters: 0.0001,
        gravity: 9.81,
      }),
    ).toMatchObject({ activationThresholdNewtons: 100 })
  })

  it('throws on partial config', () => {
    expect(() => parseHardwareConfig({ activationThresholdNewtons: 100 })).toThrow(
      ApiValidationError,
    )
  })
})
