/** Preset windows for daily time-series queries (whole local calendar days through today). */
export const TIME_PRESETS = [
  { id: '7d', label: 'Past Week' },
  { id: '30d', label: 'Past Month' },
  { id: '6m', label: 'Past 6 Months' },
  { id: '365d', label: 'Past Year' },
] as const

export type TimePresetId = (typeof TIME_PRESETS)[number]['id']

export const PRESET_IDS = new Set<string>(TIME_PRESETS.map((p) => p.id))

export const LINE_COLORS = [
  '#22d3ee',
  '#a78bfa',
  '#34d399',
  '#fb923c',
  '#f472b6',
  '#38bdf8',
  '#fbbf24',
  '#4ade80',
] as const

export const CHART_ANIMATION = { isAnimationActive: false, animationDuration: 0 } as const

export const Y_AXIS_DOMAIN: [number, 'auto'] = [0, 'auto']
