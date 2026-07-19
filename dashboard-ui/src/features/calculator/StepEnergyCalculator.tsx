import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { fetchHardwareConfig } from '../../api/hardwareConfig'
import { fetchDevices } from '../../api/devices'
import type { HardwareConfig } from '../../api/types/hardware'
import DeviceFeasibilityCard from '../../components/DeviceFeasibilityCard'
import ContextNotice from '../../components/ContextNotice'
import { DEFAULT_HARDWARE_CONFIG } from '../../config/hardwareDefaults'
import { formatDecimal, formatEquivalent, wattHoursFromJoules } from '../../utils/energyFormat'
import {
  feasibilityBadge,
  feasibilityPercent,
  runtimeDays,
} from '../feasibility/locationAggregates'
import {
  calculateActivationHarvest,
  minimumActivationMassKg,
  suggestImpactMultiplier,
} from '../../utils/calculatorPhysics.js'
import {
  kgToPounds,
  metersPerSecondToKmh,
  mphToKmh,
  poundsToKg,
} from '../../utils/unitConversions.js'
import { parseNonNegativeInt, parsePositiveNumber } from './calculatorParsers'

/** Tile compression harvest calculator using the hardware threshold model. */
export default function StepEnergyCalculator() {
  const {
    data: hardwareFromApi,
    isPending: hardwareLoading,
    isError: hardwareError,
  } = useQuery({
    queryKey: ['hardware', 'config'],
    queryFn: ({ signal }) => fetchHardwareConfig(signal),
    staleTime: Infinity,
    retry: 1,
  })

  const hardwareForCalc: HardwareConfig | null = hardwareError
    ? DEFAULT_HARDWARE_CONFIG
    : (hardwareFromApi ?? null)

  const displayHardware: HardwareConfig | null = hardwareError
    ? DEFAULT_HARDWARE_CONFIG
    : hardwareFromApi ?? null

  const { data: devices = [] } = useQuery({
    queryKey: ['devices', 'catalog'],
    queryFn: ({ signal }) => fetchDevices(signal),
    staleTime: Infinity,
  })

  const [massRaw, setMassRaw] = useState('80')
  const [massUnit, setMassUnit] = useState<'kg' | 'lb'>('kg')
  const [impactRaw, setImpactRaw] = useState('1.0')
  const [compressionsRaw, setCompressionsRaw] = useState('5000')
  const [speedRaw, setSpeedRaw] = useState('')
  const [speedUnit, setSpeedUnit] = useState<'kmh' | 'mph' | 'mps'>('kmh')

  const inputsDisabled = hardwareLoading

  const speedSuggestion = useMemo(() => {
    const speed = parsePositiveNumber(speedRaw)
    if (speed === null) return null
    let speedKmh = speed
    if (speedUnit === 'mps') speedKmh = metersPerSecondToKmh(speed)
    else if (speedUnit === 'mph') speedKmh = mphToKmh(speed)
    return suggestImpactMultiplier(speedKmh)
  }, [speedRaw, speedUnit])

  const minMassDisplay = useMemo(() => {
    if (hardwareLoading) return null
    const impact = parsePositiveNumber(impactRaw) ?? 1
    const minKg = minimumActivationMassKg(hardwareForCalc ?? DEFAULT_HARDWARE_CONFIG, impact)
    if (!Number.isFinite(minKg)) return null
    const value = massUnit === 'lb' ? kgToPounds(minKg) : minKg
    const floored = Math.floor(value * 100) / 100
    return `${formatDecimal(floored, 2)} ${massUnit}`
  }, [hardwareForCalc, hardwareLoading, impactRaw, massUnit])

  const parsed = useMemo(() => {
    const w = parsePositiveNumber(massRaw)
    if (w === null) return null
    const massKg = massUnit === 'lb' ? poundsToKg(w) : w
    const impact = parsePositiveNumber(impactRaw)
    if (impact === null || impact < 1 || impact > 1.5) return null
    const compressions = parseNonNegativeInt(compressionsRaw)
    if (compressions === null) return null
    return { massKg, impactMultiplier: impact, compressions }
  }, [massRaw, massUnit, impactRaw, compressionsRaw])

  const result = useMemo(() => {
    if (!parsed || !hardwareForCalc) return null
    return calculateActivationHarvest(
      parsed.massKg,
      parsed.impactMultiplier,
      parsed.compressions,
      hardwareForCalc,
    )
  }, [parsed, hardwareForCalc])

  const totalWh = result ? wattHoursFromJoules(result.totalJoules) : 0

  const deviceCards = useMemo(() => {
    return devices
      .map((d) => {
        const pct = feasibilityPercent(totalWh, d)
        return {
          device: d,
          pct,
          badge: feasibilityBadge(pct),
          days: runtimeDays(totalWh, d),
        }
      })
      .sort((a, b) => (b.pct ?? -Infinity) - (a.pct ?? -Infinity))
  }, [devices, totalWh])

  return (
    <div className="w-full space-y-6">
      <header>
        <h2 className="rgf-h2-section">Tile activation calculator</h2>
        <p className="rgf-lead">
          Estimate generated energy when compressions meet the{' '}
          {hardwareLoading ? (
            <span className="text-[var(--rgf-text-subtle)]">loading threshold…</span>
          ) : displayHardware ? (
            <>
              {formatDecimal(displayHardware.activationThresholdNewtons, 0)} N threshold (
              {formatDecimal(displayHardware.minRatedOutputJoules, 1)}–
              {formatDecimal(displayHardware.maxRatedOutputJoules, 1)} J per activation depending on weight
              and step intensity).
            </>
          ) : null}
        </p>
        <p className="rgf-lead">
          The <strong>impact multiplier</strong> is how hard you step on one tile:
          <br />
          1.0× for a normal walk, up to 1.5× when you stomp, hurry, or jog.
          <br />
          A harder step lowers the weight needed to fire the tile and pushes effective load toward the
          max joules cap.
        </p>
      </header>

      {hardwareError && (
        <ContextNotice title="Using documented hardware defaults">
          Hardware config could not be loaded from the server; calculations use documented defaults (
          {formatDecimal(DEFAULT_HARDWARE_CONFIG.activationThresholdNewtons, 0)} N threshold,{' '}
          {formatDecimal(DEFAULT_HARDWARE_CONFIG.minRatedOutputJoules, 1)}–
          {formatDecimal(DEFAULT_HARDWARE_CONFIG.maxRatedOutputJoules, 1)} J per activation).
        </ContextNotice>
      )}

      <div className="rgf-panel space-y-4">
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <div>
            <label htmlFor="calc-mass" className="rgf-label">
              Mass (kg or lb)
            </label>
            <div className="flex gap-2">
              <input
                id="calc-mass"
                className="rgf-input flex-1"
                value={massRaw}
                onChange={(e) => setMassRaw(e.target.value)}
                disabled={inputsDisabled}
              />
              <select
                className="rgf-input"
                style={{ width: 'auto', flex: '0 0 auto' }}
                value={massUnit}
                onChange={(e) => setMassUnit(e.target.value as 'kg' | 'lb')}
                disabled={inputsDisabled}
              >
                <option value="kg">kg</option>
                <option value="lb">lb</option>
              </select>
            </div>
            {minMassDisplay && (
              <p className="rgf-hint">
                There&apos;s a minimum weight: anything lighter than about{' '}
                <strong>{minMassDisplay}</strong> won&apos;t press the tile hard enough, so it
                won&apos;t fire and makes no energy.
              </p>
            )}
          </div>
          <div>
            <label htmlFor="calc-impact" className="rgf-label">
              Impact multiplier (1.0–1.5×)
            </label>
            <input
              id="calc-impact"
              className="rgf-input w-full"
              value={impactRaw}
              onChange={(e) => setImpactRaw(e.target.value)}
              disabled={inputsDisabled}
            />
          </div>
          <div className="sm:col-span-2 xl:col-span-3">
            <label htmlFor="calc-speed" className="rgf-label">
              Not sure about the impact multiplier? Enter your walking speed and we&apos;ll suggest
              a multiplier
            </label>
            <div className="flex flex-wrap items-center gap-2">
              <input
                id="calc-speed"
                className="rgf-input flex-1"
                placeholder="e.g. 5"
                value={speedRaw}
                onChange={(e) => setSpeedRaw(e.target.value)}
                disabled={inputsDisabled}
              />
              <select
                className="rgf-input"
                style={{ width: 'auto', flex: '0 0 auto' }}
                value={speedUnit}
                onChange={(e) => setSpeedUnit(e.target.value as 'kmh' | 'mph' | 'mps')}
                disabled={inputsDisabled}
              >
                <option value="kmh">kmh</option>
                <option value="mph">mph</option>
                <option value="mps">ms</option>
              </select>
              {speedSuggestion && !inputsDisabled && (
                <button
                  type="button"
                  className="rgf-chip-btn"
                  onClick={() => setImpactRaw(String(speedSuggestion.multiplier))}
                >
                  Use {formatDecimal(speedSuggestion.multiplier, 2)}×
                </button>
              )}
            </div>
            {speedSuggestion && !inputsDisabled && (
              <p className="rgf-hint">
                Suggested multiplier:{' '}
                <strong>{formatDecimal(speedSuggestion.multiplier, 2)}×</strong>.
                {speedSuggestion.capped
                  ? " That's our maximum — running pushes harder than this, but the tile only counts up to a fast walk, so we cap it at 1.5×."
                  : ' Faster walking means a harder step and more energy.'}
              </p>
            )}
          </div>
          <div className="sm:col-span-2 xl:col-span-3">
            <label htmlFor="calc-compressions" className="rgf-label">
              Number of compressions (steps)
            </label>
            <input
              id="calc-compressions"
              className="rgf-input w-full"
              value={compressionsRaw}
              onChange={(e) => setCompressionsRaw(e.target.value)}
              disabled={inputsDisabled}
            />
          </div>
        </div>
      </div>

      {result && (
        <div className="rgf-panel space-y-3">
          <h3 className="rgf-h3">Results</h3>
          <dl className="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
            <div>
              <dt className="rgf-label">Force (N)</dt>
              <dd className="text-lg tabular-nums">{formatDecimal(result.forceNewtons, 1)}</dd>
            </div>
            <div>
              <dt className="rgf-label">Threshold met?</dt>
              <dd>{result.activationSuccessful ? 'Yes — tile fires' : 'No — 0 J per compression'}</dd>
            </div>
            <div>
              <dt className="rgf-label">Successful activations</dt>
              <dd>{formatDecimal(result.successfulActivations, 0)}</dd>
            </div>
            <div>
              <dt className="rgf-label">Total compressions</dt>
              <dd>{formatDecimal(result.totalCompressions, 0)}</dd>
            </div>
            <div>
              <dt className="rgf-label">Generated energy</dt>
              <dd className="rgf-metric-value--accent">
                {formatDecimal(result.totalJoules, 1)} J ({formatEquivalent(totalWh)} Wh)
              </dd>
            </div>
            <div>
              <dt className="rgf-label">Per activation</dt>
              <dd>
                {formatDecimal(result.joulesPerActivation, 2)} J
                {result.joulesPerActivation >= (hardwareForCalc?.maxRatedOutputJoules ?? 5) - 0.01
                  ? ' (max rated output)'
                  : ''}
              </dd>
            </div>
          </dl>

          {deviceCards.length > 0 && (
            <section className="rgf-section-divider space-y-4">
              <div>
                <h4 className="rgf-h3">What could this power?</h4>
                <p className="rgf-lead mt-2">
                  What if every step you took had landed on tiles like these?
                  <br />
                  Here&apos;s the fun part — a peek at how far your foot-powered spark could stretch
                  for the gadgets around you.
                </p>
              </div>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                {deviceCards.map(({ device, pct, badge, days }) => (
                  <DeviceFeasibilityCard
                    key={device.id}
                    device={device}
                    pct={pct}
                    badge={badge}
                    days={days}
                    headingLevel="h5"
                  />
                ))}
              </div>
            </section>
          )}
        </div>
      )}
    </div>
  )
}
