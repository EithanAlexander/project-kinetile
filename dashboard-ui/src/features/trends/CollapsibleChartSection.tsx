import type { ReactNode } from 'react'

export interface CollapsibleChartSectionProps {
  readonly title: string
  readonly description: string
  readonly open: boolean
  readonly onToggle: (open: boolean) => void
  readonly empty: boolean
  readonly emptyMessage: string
  readonly children?: ReactNode
}

export default function CollapsibleChartSection({
  title,
  description,
  open,
  onToggle,
  empty,
  emptyMessage,
  children,
}: CollapsibleChartSectionProps) {
  return (
    <details
      className="rgf-filter-details shadow-[var(--rgf-shadow-panel)]"
      open={open}
      onToggle={(e) => onToggle(e.currentTarget.open)}
    >
      <summary>{title}</summary>
      <div className="rgf-filter-details-body">
        <p className="rgf-hint text-sm">{description}</p>
        <div className="mt-4 min-h-[200px]">
          {empty ? (
            <div className="rgf-empty py-12 text-center">{emptyMessage}</div>
          ) : (
            children
          )}
        </div>
      </div>
    </details>
  )
}
