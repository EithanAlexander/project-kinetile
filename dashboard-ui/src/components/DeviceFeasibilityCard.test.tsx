import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import DeviceFeasibilityCard from './DeviceFeasibilityCard'
import { feasibilityBadge } from '../features/feasibility/locationAggregates'

describe('DeviceFeasibilityCard', () => {
  it('renders device name and coverage', () => {
    const badge = feasibilityBadge(75)
    render(
      <DeviceFeasibilityCard
        device={{ id: 'led', name: 'LED Marker', dailyRequiredWh: 6 }}
        pct={75}
        badge={badge}
        days={2.5}
      />,
    )
    expect(screen.getByText('LED Marker')).toBeInTheDocument()
    expect(screen.getByText('75%')).toBeInTheDocument()
    expect(screen.getByText(badge.label)).toBeInTheDocument()
  })
})
