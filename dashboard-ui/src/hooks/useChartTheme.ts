import { useTheme } from '../context/ThemeContext'

/**
 * Read chart chrome colors from CSS variables so Recharts matches Bright/Dark and palette.
 */
function readVar(name: string, fallback: string): string {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

export interface ChartTheme {
  tick: string
  tickStrong: string
  grid: string
  accent: string
  tooltip: {
    backgroundColor: string
    border: string
    borderRadius: number
    color: string
  }
}

/**
 * @returns Chart colors derived from current theme CSS variables.
 */
export function useChartTheme(): ChartTheme {
  // Re-render when theme tokens change; colors are read from :root CSS variables.
  useTheme()

  return {
    tick: readVar('--rgf-text-subtle', '#64748b'),
    tickStrong: readVar('--rgf-text-muted', '#94a3b8'),
    grid: readVar('--rgf-chart-grid', 'rgba(51, 65, 85, 0.35)'),
    accent: readVar('--rgf-accent', '#22d3ee'),
    tooltip: {
      backgroundColor: readVar('--rgf-surface-nav', 'rgba(2, 6, 23, 0.92)'),
      border: `1px solid ${readVar('--rgf-border-cyan-mid', 'rgba(34, 211, 238, 0.25)')}`,
      borderRadius: 8,
      color: readVar('--rgf-text-primary', '#e2e8f0'),
    },
  }
}
