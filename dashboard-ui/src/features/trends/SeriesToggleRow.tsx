import { LINE_COLORS } from './timeseriesConstants'

export interface SeriesToggleRowProps {
  readonly keys: string[]
  readonly visibility: Record<string, boolean | undefined>
  readonly onToggle: (key: string) => void
  readonly onSelectAll: () => void
  readonly onDeselectAll?: () => void
  readonly selectAllLabel?: string
  readonly deselectAllLabel?: string
}

export default function SeriesToggleRow({
  keys,
  visibility,
  onToggle,
  onSelectAll,
  onDeselectAll,
  selectAllLabel = 'Show all series',
  deselectAllLabel = 'Hide all series',
}: SeriesToggleRowProps) {
  const anyOff = keys.some((k) => visibility[k] === false)
  const anyOn = keys.some((k) => visibility[k] !== false)
  return (
    <div className="mb-4">
      {keys.length > 0 && (
        <div className="mb-2 flex flex-wrap gap-2">
          <button
            type="button"
            disabled={!anyOff}
            onClick={onSelectAll}
            className="rgf-btn-sm"
          >
            {selectAllLabel}
          </button>
          {onDeselectAll && (
            <button
              type="button"
              disabled={!anyOn}
              onClick={onDeselectAll}
              className="rgf-btn-sm"
            >
              {deselectAllLabel}
            </button>
          )}
        </div>
      )}
      <div className="flex flex-wrap gap-x-4 gap-y-2">
        {keys.map((key, i) => (
          <label
            key={key}
            className="rgf-check-label text-sm"
          >
            <input
              type="checkbox"
              className="size-4"
              checked={visibility[key] !== false}
              onChange={() => onToggle(key)}
            />
            <span
              className="inline-block size-2.5 shrink-0 rounded-full"
              style={{ backgroundColor: LINE_COLORS[i % LINE_COLORS.length] }}
              aria-hidden
            />
            <span>{key}</span>
          </label>
        ))}
      </div>
    </div>
  )
}
