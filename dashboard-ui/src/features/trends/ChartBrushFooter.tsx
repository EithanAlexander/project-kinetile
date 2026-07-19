import { normalizeBrushIndices, type BrushIndices, type ChartDataRow } from './chartRangeDrag'

export interface ChartBrushFooterProps {
  readonly fullRows: ChartDataRow[]
  readonly brush: BrushIndices
  readonly onReset: () => void
}

/** Shows the focused date span and a reset control when the chart is zoomed. */
export default function ChartBrushFooter({ fullRows, brush, onReset }: ChartBrushFooterProps) {
  const len = fullRows?.length ?? 0
  if (len === 0) return null
  const { startIndex, endIndex } = normalizeBrushIndices(brush, len)
  const zoomed = len > 1 && (startIndex > 0 || endIndex < len - 1)
  const startLabel = fullRows[startIndex]?.label ?? '—'
  const endLabel = fullRows[endIndex]?.label ?? '—'
  return (
    <div className="rgf-chart-footer">
      <span>
        Chart focus:{' '}
        <strong>
          {startLabel} → {endLabel}
        </strong>
      </span>
      {zoomed && (
        <button type="button" className="rgf-btn-sm" onClick={onReset}>
          Show full range
        </button>
      )}
    </div>
  )
}
