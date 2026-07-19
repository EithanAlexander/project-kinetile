import type { ChartId, DragPreview } from './chartRangeDrag'

export interface ChartDragSelectionOverlayProps {
  readonly chartId: ChartId
  readonly dragPreview: DragPreview | null
}

/** High-contrast HTML layer above the chart while dragging (pixel band follows the pointer). */
export default function ChartDragSelectionOverlay({ chartId, dragPreview }: ChartDragSelectionOverlayProps) {
  const overlay = dragPreview?.chartId === chartId ? dragPreview.overlay : undefined
  if (overlay == null) return null
  const { left, width } = overlay
  const w = Math.max(width, 3)
  return (
    <div
      aria-hidden
      className="rgf-chart-brush-preview"
      style={{
        left,
        width: w,
        height: '100%',
        boxSizing: 'border-box',
      }}
    />
  )
}
