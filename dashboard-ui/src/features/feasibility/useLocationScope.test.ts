import { renderHook, act } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { LocationEnergyRow } from '../../api/types/locations'
import { useLocationScope } from './useLocationScope'

const rows: LocationEnergyRow[] = [
  {
    city: 'Alpha',
    location: 'Station',
    totalJoules: 100,
    totalWattHours: 0.028,
    totalCompressions: 10,
    successfulActivations: 8,
  },
  {
    city: 'Beta',
    location: 'Crossing',
    totalJoules: 200,
    totalWattHours: 0.056,
    totalCompressions: 20,
    successfulActivations: 15,
  },
]

describe('useLocationScope', () => {
  it('builds city options from rows', () => {
    const { result } = renderHook(() => useLocationScope(rows))
    expect(result.current.cityOptions).toEqual(['Alpha', 'Beta'])
  })

  it('filters scoped rows by city', () => {
    const { result } = renderHook(() => useLocationScope(rows))
    act(() => {
      result.current.setScopeCity('Alpha')
    })
    expect(result.current.scopedRows).toHaveLength(1)
    expect(result.current.scopedRows[0]?.city).toBe('Alpha')
  })

  it('clears stale site key when row disappears', () => {
    const { result, rerender } = renderHook(
      ({ data }: { data: LocationEnergyRow[] }) => useLocationScope(data),
      { initialProps: { data: rows } },
    )
    const siteKey = result.current.siteOptions.find((o) => o.label.includes('Station'))?.key
    expect(siteKey).toBeTruthy()
    act(() => {
      result.current.setScopeSiteKey(siteKey!)
    })
    rerender({ data: rows.filter((r) => r.city !== 'Alpha') })
    expect(result.current.scopeSiteKey).toBe('')
  })
})
