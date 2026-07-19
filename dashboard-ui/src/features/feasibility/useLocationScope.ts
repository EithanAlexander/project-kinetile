/* eslint-disable react-hooks/set-state-in-effect -- scope pickers follow loaded catalog rows */
import { useEffect, useMemo, useState } from 'react'
import type { LocationEnergyRow } from '../../api/types/locations'
import {
  normalizeCity,
  normalizeSite,
  parseSiteOptionKey,
  siteOptionKey,
} from './locationAggregates'

export interface SiteOption {
  key: string
  label: string
}

export interface UseLocationScopeResult {
  scopeCity: string
  setScopeCity: (value: string) => void
  scopeSiteKey: string
  setScopeSiteKey: (value: string) => void
  cityOptions: string[]
  siteOptions: SiteOption[]
  scopedRows: LocationEnergyRow[]
  scopeLabel: string
}

/**
 * City / chokepoint scope shared by feasibility and activation views.
 */
export function useLocationScope(
  rows: LocationEnergyRow[],
  extraCities: string[] = [],
): UseLocationScopeResult {
  const [scopeCity, setScopeCity] = useState('')
  const [scopeSiteKey, setScopeSiteKey] = useState('')

  const cityOptions = useMemo(() => {
    const s = new Set<string>(extraCities)
    for (const r of rows) {
      const c = normalizeCity(r)
      if (c) s.add(c)
    }
    return [...s].sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
  }, [rows, extraCities])

  const siteOptions = useMemo(() => {
    const list: SiteOption[] = []
    const seen = new Set<string>()
    for (const r of rows) {
      const c = normalizeCity(r)
      if (scopeCity && c !== scopeCity) continue
      const site = normalizeSite(r)
      const key = siteOptionKey(c, site)
      if (seen.has(key)) continue
      seen.add(key)
      const label = site ? `${c || '—'} — ${site}` : c || '—'
      list.push({ key, label })
    }
    list.sort((a, b) => a.label.localeCompare(b.label, undefined, { sensitivity: 'base' }))
    return list
  }, [rows, scopeCity])

  useEffect(() => {
    if (!scopeSiteKey) return
    const { city, site } = parseSiteOptionKey(scopeSiteKey)
    const ok = rows.some(
      (r) =>
        normalizeCity(r) === city &&
        normalizeSite(r) === site &&
        (!scopeCity || normalizeCity(r) === scopeCity),
    )
    if (!ok) setScopeSiteKey('')
  }, [scopeSiteKey, scopeCity, rows])

  const scopedRows = useMemo(() => {
    let out = rows
    if (scopeCity) {
      out = out.filter((r) => normalizeCity(r) === scopeCity)
    }
    if (scopeSiteKey) {
      const { city, site } = parseSiteOptionKey(scopeSiteKey)
      out = out.filter((r) => normalizeCity(r) === city && normalizeSite(r) === site)
    }
    return out
  }, [rows, scopeCity, scopeSiteKey])

  const scopeLabel = useMemo(() => {
    if (scopeSiteKey) {
      const { city, site } = parseSiteOptionKey(scopeSiteKey)
      return site ? `${city || '—'} — ${site}` : city || '—'
    }
    if (scopeCity) return scopeCity
    return 'All cities'
  }, [scopeCity, scopeSiteKey])

  return {
    scopeCity,
    setScopeCity,
    scopeSiteKey,
    setScopeSiteKey,
    cityOptions,
    siteOptions,
    scopedRows,
    scopeLabel,
  }
}
