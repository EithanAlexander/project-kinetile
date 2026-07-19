import { useRef, type Dispatch, type SetStateAction } from 'react'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { useChartTheme } from '../../hooks/useChartTheme'
import { formatDecimal } from '../../utils/energyFormat'
import ChartBrushFooter from './ChartBrushFooter'
import ChartDragSelectionOverlay from './ChartDragSelectionOverlay'
import {
  attachChartRangeDrag,
  normalizeBrushIndices,
  type BrushIndices,
  type ChartDataRow,
  type ChartId,
  type DragPreview,
  type RechartsChartInstance,
} from './chartRangeDrag'
import { CHART_ANIMATION, Y_AXIS_DOMAIN } from './timeseriesConstants'

export interface WhLineSeries {
  dataKey: string
  name: string
  stroke: string
  hidden?: boolean
  dot?: false | { r: number }
}

export interface DailyWhLineChartProps {
  readonly chartId: ChartId
  readonly ariaLabel: string
  readonly plotHeightClass: string
  readonly fullRows: ChartDataRow[]
  readonly zoomedData: ChartDataRow[]
  readonly brush: BrushIndices
  readonly setBrush: Dispatch<SetStateAction<BrushIndices>>
  readonly dragPreview: DragPreview | null
  readonly setDragPreview: Dispatch<SetStateAction<DragPreview | null>>
  readonly lines: WhLineSeries[]
  readonly tooltipFormatter?: (value: number) => [string, string]
}

function yTickWh(v: number): string {
  if (!Number.isFinite(v)) return ''
  const digits = v >= 100 ? 0 : 2
  return formatDecimal(v, digits)
}

/** Shared Recharts daily Wh line chart with drag-to-zoom and brush footer. */
export default function DailyWhLineChart({
  chartId,
  ariaLabel,
  plotHeightClass,
  fullRows,
  zoomedData,
  brush,
  setBrush,
  dragPreview,
  setDragPreview,
  lines,
  tooltipFormatter,
}: DailyWhLineChartProps) {
  const chartTheme = useChartTheme()
  const chartRef = useRef<RechartsChartInstance | null>(null)
  const plotHostRef = useRef<HTMLDivElement>(null)

  const resetBrush = () => {
    const len = fullRows.length
    setBrush({ startIndex: 0, endIndex: Math.max(0, len - 1) })
  }

  return (
    <div className="space-y-2">
      <div
        ref={plotHostRef}
        className={`relative w-full cursor-crosshair select-none ${plotHeightClass}`}
      >
        <ResponsiveContainer width="100%" height="100%">
          <LineChart
            ref={chartRef as never}
            data={zoomedData}
            margin={{ top: 8, right: 12, left: 0, bottom: 8 }}
            aria-label={ariaLabel}
            onMouseDown={(state, ev) =>
              attachChartRangeDrag({
                chartRef,
                chartId,
                fullRows,
                norm: normalizeBrushIndices(brush, fullRows.length),
                setBrush,
                setDragPreview,
                plotHostRef,
                state,
                e: ev,
              })
            }
          >
            <CartesianGrid stroke={chartTheme.grid} strokeDasharray="3 3" />
            <XAxis dataKey="label" tick={{ fill: chartTheme.tickStrong, fontSize: 12 }} />
            <YAxis
              domain={Y_AXIS_DOMAIN}
              tick={{ fill: chartTheme.tickStrong, fontSize: 12 }}
              tickFormatter={yTickWh}
              label={{
                value: 'Wh / day',
                angle: -90,
                position: 'insideLeft',
                fill: chartTheme.tick,
                fontSize: 12,
              }}
            />
            <Tooltip
              contentStyle={chartTheme.tooltip}
              formatter={
                tooltipFormatter
                  ? (value) => tooltipFormatter(Number(value))
                  : undefined
              }
            />
            <Legend wrapperStyle={{ color: chartTheme.tickStrong, fontSize: 12 }} />
            {lines.map((line) =>
              line.hidden ? null : (
                <Line
                  key={line.dataKey}
                  {...CHART_ANIMATION}
                  type="monotone"
                  dataKey={line.dataKey}
                  name={line.name}
                  stroke={line.stroke}
                  strokeWidth={2}
                  dot={line.dot ?? false}
                  connectNulls
                />
              ),
            )}
          </LineChart>
        </ResponsiveContainer>
        <ChartDragSelectionOverlay chartId={chartId} dragPreview={dragPreview} />
      </div>
      <ChartBrushFooter fullRows={fullRows} brush={brush} onReset={resetBrush} />
    </div>
  )
}
