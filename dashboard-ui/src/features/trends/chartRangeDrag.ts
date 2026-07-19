import type { Dispatch, MouseEvent as ReactMouseEvent, SetStateAction } from 'react'

export type ChartId = 'location' | 'city' | 'network'

export interface BrushIndices {
  startIndex: number
  endIndex: number
}

export interface DragOverlay {
  left: number
  width: number
}

export interface DragPreview {
  chartId: ChartId
  i0: number
  i1: number
  overlay: DragOverlay
}

export interface ChartDataRow {
  label?: string
  bucketStart?: string
  [key: string]: string | number | undefined
}

export interface RechartsChartInstance {
  container?: HTMLElement | null
  getMouseInfo?: (pointer: { pageX: number; pageY: number }) => { activeTooltipIndex?: number } | null
}

type PointerLike = MouseEvent | Touch | TouchEvent | ReactMouseEvent

/**
 * Stable key so brush zoom resets when the loaded window or axis changes, not on every render.
 */
export function brushResetKey(rows: ChartDataRow[]): string {
  if (!rows?.length) return '0'
  const first = rows[0]
  const last = rows.at(-1)
  const a = String(first?.label ?? '')
  const b = String(last?.label ?? '')
  return `${rows.length}:${a}:${b}`
}

export function normalizeBrushIndices(next: Partial<BrushIndices>, dataLength: number): BrushIndices {
  if (dataLength <= 0) return { startIndex: 0, endIndex: 0 }
  const max = dataLength - 1
  let s = Number(next?.startIndex)
  let e = Number(next?.endIndex)
  if (!Number.isFinite(s)) s = 0
  if (!Number.isFinite(e)) e = max
  s = Math.max(0, Math.min(s, max))
  e = Math.max(0, Math.min(e, max))
  if (s > e) [s, e] = [e, s]
  return { startIndex: s, endIndex: e }
}

/**
 * Map Recharts tooltip index (relative to the current `LineChart` `data` slice) to a full-series index.
 */
export function fullIndexFromActiveTooltip(
  activeTooltipIndex: number | undefined,
  sliceStart: number,
  fullLength: number,
): number | null {
  if (typeof activeTooltipIndex !== 'number' || activeTooltipIndex < 0) return null
  const fullIdx = sliceStart + activeTooltipIndex
  if (fullIdx < 0 || fullIdx >= fullLength) return null
  return fullIdx
}

function pointerClientX(ev: PointerLike): number | null {
  if (ev && typeof ev === 'object' && 'clientX' in ev && typeof ev.clientX === 'number') {
    return ev.clientX
  }
  if (ev && typeof ev === 'object' && 'touches' in ev) {
    const t = ev.touches?.[0]
    if (t) return t.clientX
  }
  if (ev && typeof ev === 'object' && 'changedTouches' in ev) {
    const t = ev.changedTouches?.[0]
    if (t) return t.clientX
  }
  return null
}

/**
 * Horizontal band in **plot host** coordinates (the `position: relative` div that stacks the overlay on the chart).
 */
function dragOverlayFromPointer(
  chartRef: { current: RechartsChartInstance | null },
  plotHostRef: { current: HTMLElement | null },
  startXInChart: number,
  ev: PointerLike,
): DragOverlay {
  const wrap = chartRef.current?.container
  const host = plotHostRef?.current
  if (!wrap?.getBoundingClientRect || !host?.getBoundingClientRect) {
    return { left: 0, width: 0 }
  }
  const wrapRect = wrap.getBoundingClientRect()
  const hostRect = host.getBoundingClientRect()
  const cx = pointerClientX(ev)
  if (typeof cx !== 'number') return { left: 0, width: 0 }

  const xInChart = Math.max(0, Math.min(cx - wrapRect.left, wrapRect.width))
  const leftInChart = Math.min(startXInChart, xInChart)
  const widthInChart = Math.abs(xInChart - startXInChart)

  const left = wrapRect.left - hostRect.left + leftInChart
  const width = widthInChart
  return { left, width }
}

export interface AttachChartRangeDragOptions {
  chartRef: { current: RechartsChartInstance | null }
  chartId: ChartId
  fullRows: ChartDataRow[]
  norm: BrushIndices
  setBrush: Dispatch<SetStateAction<BrushIndices>>
  setDragPreview: Dispatch<SetStateAction<DragPreview | null>>
  plotHostRef: { current: HTMLElement | null }
  state: { activeTooltipIndex?: number } | null | undefined
  e: ReactMouseEvent | Touch
}

/**
 * Left-button drag (or touch) on the plot: track `mousemove`/`mouseup` on `window` because Recharts
 * overrides `onMouseMove` on the chart wrapper.
 */
export function attachChartRangeDrag({
  chartRef,
  chartId,
  fullRows,
  norm,
  setBrush,
  setDragPreview,
  plotHostRef,
  state,
  e,
}: AttachChartRangeDragOptions): void {
  if (e && 'button' in e && e.button !== 0) return
  const fullLen = fullRows.length
  if (fullLen === 0) return

  const idx = fullIndexFromActiveTooltip(state?.activeTooltipIndex, norm.startIndex, fullLen)
  if (idx == null) return
  if ('preventDefault' in e && typeof e.preventDefault === 'function') {
    e.preventDefault()
  }

  const wrap = chartRef.current?.container
  const rect0 = wrap?.getBoundingClientRect?.()
  const cx0 = pointerClientX(e)
  const startXInChart =
    rect0 && typeof cx0 === 'number'
      ? Math.max(0, Math.min(cx0 - rect0.left, rect0.width))
      : 0

  const startIdx = idx
  const endRef = { current: idx }

  const onMove = (ev: PointerLike) => {
    const inst = chartRef.current
    const overlay = dragOverlayFromPointer(chartRef, plotHostRef, startXInChart, ev)

    if (inst?.getMouseInfo) {
      const m = inst.getMouseInfo(ev as { pageX: number; pageY: number })
      if (m) {
        const j = fullIndexFromActiveTooltip(m.activeTooltipIndex, norm.startIndex, fullLen)
        if (j != null) endRef.current = j
      }
    }

    setDragPreview({
      chartId,
      i0: startIdx,
      i1: endRef.current,
      overlay,
    })
  }

  const onUp = () => {
    globalThis.removeEventListener('mousemove', onMove)
    globalThis.removeEventListener('mouseup', onUp)
    globalThis.removeEventListener('touchmove', onTouchMove)
    globalThis.removeEventListener('touchend', onTouchEnd)
    setDragPreview(null)
    const lo = Math.min(startIdx, endRef.current)
    const hi = Math.max(startIdx, endRef.current)
    setBrush({ startIndex: lo, endIndex: hi })
  }

  const onTouchMove = (ev: TouchEvent) => {
    const t = ev.touches?.[0]
    if (t) onMove(t)
  }
  const onTouchEnd = () => {
    onUp()
  }

  const hostRect0 = plotHostRef?.current?.getBoundingClientRect?.()
  const overlayLeft0 =
    rect0 && hostRect0 ? rect0.left - hostRect0.left + startXInChart : 0

  setDragPreview({
    chartId,
    i0: startIdx,
    i1: startIdx,
    overlay: { left: overlayLeft0, width: 0 },
  })
  globalThis.addEventListener('mousemove', onMove)
  globalThis.addEventListener('mouseup', onUp)
  globalThis.addEventListener('touchmove', onTouchMove, { passive: true })
  globalThis.addEventListener('touchend', onTouchEnd)
}
