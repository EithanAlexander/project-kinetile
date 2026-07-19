import type { EdgeDevice } from '../api/types/devices'
import { formatDecimal, formatEquivalent } from '../utils/energyFormat'
import type { feasibilityBadge } from '../features/feasibility/locationAggregates'

type FeasibilityBadge = ReturnType<typeof feasibilityBadge>

export interface DeviceFeasibilityCardProps {
  readonly device: EdgeDevice
  readonly pct: number | null
  readonly badge: FeasibilityBadge
  readonly days?: number | null
  readonly coverageLabel?: string
  readonly onHide?: () => void
  readonly headingLevel?: 'h4' | 'h5'
}

/**
 * Shared edge-device feasibility card (badge, load coverage bar, optional runtime days).
 */
export default function DeviceFeasibilityCard({
  device,
  pct,
  badge,
  days = null,
  coverageLabel = 'Load coverage',
  onHide,
  headingLevel = 'h4',
}: DeviceFeasibilityCardProps) {
  const barWidth = pct === null ? 0 : Math.min(100, pct)
  const Heading = headingLevel

  return (
    <article className="rgf-card-loc">
      <div className={onHide ? 'flex items-start justify-between gap-2' : undefined}>
        <Heading className="rgf-h3">{device.name}</Heading>
        {onHide && (
          <button
            type="button"
            onClick={onHide}
            className="rgf-btn-sm-danger"
            title="Hide this device"
          >
            Hide
          </button>
        )}
      </div>
      <p className="mt-1 font-mono text-xs tabular-nums text-[var(--rgf-text-subtle)]">
        {formatDecimal(device.dailyRequiredWh, 3)} Wh/day required
      </p>

      <div className={`mt-3 ${badge.className}`}>{badge.label}</div>

      <div className="mt-3">
        <div className="rgf-progress-meta">
          <span>{coverageLabel}</span>
          <span className="rgf-progress-meta-value">
            {pct === null ? '—' : `${formatDecimal(pct, 2)}%`}
          </span>
        </div>
        <div className="rgf-progress-track">
          <progress
            className="absolute inset-0 h-full w-full opacity-0"
            max={100}
            value={barWidth}
            aria-label={`Feasibility for ${device.name}`}
          />
          <div className={badge.barClass} style={{ width: `${barWidth}%` }} />
        </div>
      </div>

      {days !== undefined && (
        <p className="rgf-card-note">
          Covers approximately{' '}
          <strong>{days == null ? '—' : `${formatEquivalent(days)} days`}</strong> of runtime.
        </p>
      )}
    </article>
  )
}
