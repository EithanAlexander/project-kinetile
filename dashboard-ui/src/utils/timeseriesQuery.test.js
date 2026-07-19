import { describe, expect, it, vi, afterEach } from 'vitest'
import {
  addMonthsLocalYmd,
  appendRangeToParams,
  calendarRollingRangeDays,
  clampTimeRangeToMaxCalendarMonths,
  expandWideChartToTimeRange,
  formatChartDayLabel,
  formatYmdAsDMonY,
  formatYmdAsDmy,
  isValidCalendarYmd,
  listLocalYmdsInclusive,
  parseDmyToYmd,
  pivotDailyWh,
  rangeFromPreset,
  startOfLocalDayIso,
  endOfLocalDayIso,
  timeRangeFromLocalYmd,
  totalSeriesFromRows,
  withTimeRangeQuery,
} from './timeseriesQuery'

function localYmd(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

describe('rangeFromPreset', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('uses last 7 local calendar days for 7d', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-28T12:00:00.000Z'))
    const now = Date.now()
    expect(rangeFromPreset('7d', now)).toEqual(calendarRollingRangeDays(7, now))
  })

  it('uses six calendar months through today for 6m', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-28T12:00:00.000Z'))
    const now = Date.now()
    const r = rangeFromPreset('6m', now)
    const end = new Date(now)
    const endYmd = localYmd(end)
    const startYmd = addMonthsLocalYmd(endYmd, -6)
    expect(r).toEqual({
      since: startOfLocalDayIso(startYmd),
      until: endOfLocalDayIso(endYmd),
    })
  })
})

describe('listLocalYmdsInclusive', () => {
  it('lists each local day in range', () => {
    const since = startOfLocalDayIso('2026-03-01')
    const until = endOfLocalDayIso('2026-03-03')
    expect(listLocalYmdsInclusive(since, until)).toEqual(['2026-03-01', '2026-03-02', '2026-03-03'])
  })

  it('returns empty when bounds are not valid instants', () => {
    expect(listLocalYmdsInclusive('invalid', endOfLocalDayIso('2026-03-01'))).toEqual([])
  })
})

describe('expandWideChartToTimeRange', () => {
  it('fills missing days with zeros', () => {
    const range = timeRangeFromLocalYmd('2026-03-01', '2026-03-03')
    const chartData = [
      {
        bucketStart: startOfLocalDayIso('2026-03-02'),
        label: 'x',
        A: 5,
      },
    ]
    const expanded = expandWideChartToTimeRange(chartData, ['A'], range)
    expect(expanded).toHaveLength(3)
    expect(expanded[0].A).toBe(0)
    expect(expanded[1].A).toBe(5)
    expect(expanded[2].A).toBe(0)
  })
})

describe('formatYmdAsDmy and parseDmyToYmd', () => {
  it('round-trips canonical YMD', () => {
    expect(formatYmdAsDmy('2026-03-15')).toBe('15-03-2026')
    expect(parseDmyToYmd('15-03-2026')).toBe('2026-03-15')
    expect(parseDmyToYmd('5-3-2026')).toBe('2026-03-05')
  })

  it('parseDmyToYmd rejects impossible dates', () => {
    expect(parseDmyToYmd('31-02-2026')).toBe(null)
    expect(parseDmyToYmd('2026-03-15')).toBe(null)
    expect(parseDmyToYmd('')).toBe(null)
  })
})

describe('formatYmdAsDMonY', () => {
  it('formats canonical YMD as DD-MMM-YYYY', () => {
    expect(formatYmdAsDMonY('2025-09-20')).toBe('20-Sep-2025')
    expect(formatYmdAsDMonY('2026-01-05')).toBe('05-Jan-2026')
  })
})

describe('formatChartDayLabel', () => {
  it('uses DD-MMM-YYYY for a local midnight ISO from startOfLocalDayIso', () => {
    const iso = startOfLocalDayIso('2026-07-08')
    expect(formatChartDayLabel(iso)).toBe('08-Jul-2026')
  })
})

describe('isValidCalendarYmd', () => {
  it('accepts real calendar days', () => {
    expect(isValidCalendarYmd('2026-03-01')).toBe(true)
    expect(isValidCalendarYmd(' 2026-03-01 ')).toBe(true)
  })

  it('rejects impossible and malformed dates', () => {
    expect(isValidCalendarYmd('2026-02-31')).toBe(false)
    expect(isValidCalendarYmd('not-a-date')).toBe(false)
    expect(isValidCalendarYmd('2026-13-01')).toBe(false)
    expect(isValidCalendarYmd('NaN-NaN-NaN')).toBe(false)
  })
})

describe('timeRangeFromLocalYmd', () => {
  it('returns null bounds when either YMD is invalid', () => {
    expect(timeRangeFromLocalYmd('2026-02-31', '2026-03-01')).toEqual({ since: null, until: null })
    expect(timeRangeFromLocalYmd('2026-03-01', 'hello')).toEqual({ since: null, until: null })
  })

  it('orders inverted YMD bounds', () => {
    const r = timeRangeFromLocalYmd('2026-01-12', '2026-01-10')
    expect(r.since).toBe(startOfLocalDayIso('2026-01-10'))
    expect(r.until).toBe(endOfLocalDayIso('2026-01-12'))
  })
})

describe('clampTimeRangeToMaxCalendarMonths', () => {
  it('pulls since forward when span exceeds max months', () => {
    const raw = timeRangeFromLocalYmd('2020-01-01', '2026-06-15')
    const c = clampTimeRangeToMaxCalendarMonths(raw, 13)
    const endYmd = '2026-06-15'
    const limitStart = addMonthsLocalYmd(endYmd, -13)
    expect(c.since).toBe(startOfLocalDayIso(limitStart))
    expect(c.until).toBe(endOfLocalDayIso(endYmd))
  })

  it('leaves short ranges unchanged', () => {
    const raw = timeRangeFromLocalYmd('2026-01-01', '2026-01-31')
    expect(clampTimeRangeToMaxCalendarMonths(raw, 13)).toEqual(raw)
  })

  it('returns null bounds when ISO instants are not parseable', () => {
    expect(
      clampTimeRangeToMaxCalendarMonths(
        { since: 'not-an-iso-timestamp', until: '2026-01-01T00:00:00.000Z' },
        13,
      ),
    ).toEqual({ since: null, until: null })
  })
})

describe('appendRangeToParams and withTimeRangeQuery', () => {
  it('omits params when unbounded', () => {
    const p = appendRangeToParams({ since: null, until: null })
    expect(p.toString()).toBe('')
    expect(withTimeRangeQuery('/api/x', { since: null, until: null })).toBe('/api/x')
  })

  it('appends since and until', () => {
    const url = withTimeRangeQuery('/api/x', {
      since: '2026-01-01T00:00:00.000Z',
      until: '2026-01-31T23:59:59.999Z',
    })
    expect(url).toContain('since=')
    expect(url).toContain('until=')
  })
})

describe('totalSeriesFromRows', () => {
  it('maps and sorts by bucketStart', () => {
    const rows = [
      { bucketStart: '2026-03-02T00:00:00Z', totalWattHours: 2 },
      { bucketStart: '2026-03-01T00:00:00Z', totalWattHours: 1 },
    ]
    const s = totalSeriesFromRows(rows)
    expect(s.map((x) => x.bucketStart)).toEqual(['2026-03-01T00:00:00Z', '2026-03-02T00:00:00Z'])
    expect(s[0].totalWh).toBe(1)
  })
})

describe('pivotDailyWh', () => {
  it('builds wide rows per day and series key', () => {
    const rows = [
      { bucketStart: '2026-03-01T00:00:00Z', city: 'A', totalWattHours: 1 },
      { bucketStart: '2026-03-01T00:00:00Z', city: 'B', totalWattHours: 2 },
      { bucketStart: '2026-03-02T00:00:00Z', city: 'A', totalWattHours: 4 },
    ]
    const { chartData, seriesKeys } = pivotDailyWh(rows, (r) => String(r.city ?? ''))
    expect(seriesKeys).toEqual(['A', 'B'])
    expect(chartData).toHaveLength(2)
    expect(chartData[0].A).toBe(1)
    expect(chartData[0].B).toBe(2)
    expect(chartData[1].A).toBe(4)
    expect(chartData[1].B).toBe(0)
  })
})
