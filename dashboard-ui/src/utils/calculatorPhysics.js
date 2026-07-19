/**
 * Threshold-activation calculator math with weight-scaled rated output band.
 */

/** Lowest impact multiplier the walking model accepts (a gentle step). */
export const MIN_IMPACT_MULTIPLIER = 1
/**
 * Highest impact multiplier the walking model accepts.
 * Kashem et al. (Acta Energetica 2/43, 2020) measured the ground force of a
 * walking person at 1x–1.5x body weight, so 1.5 is the realistic ceiling for
 * walking. Faster gaits (running) exceed this, but fall outside the walking
 * model and are clamped here.
 */
export const MAX_IMPACT_MULTIPLIER = 1.5

/**
 * Suggests an impact multiplier from walking speed.
 *
 * The multiplier is the peak downward force as a multiple of body weight. It
 * grows with pace and is clamped to the walking range [1.0, 1.5] documented by
 * Kashem et al. (Acta Energetica 2/43, 2020). When the speed implies a force
 * above that range, the suggestion is capped and `capped` is set to true.
 *
 * @param {number} speedKmh Walking speed in kilometres per hour.
 * @returns {{ multiplier: number, capped: boolean } | null} Suggestion, or null for invalid speed.
 */
export function suggestImpactMultiplier(speedKmh) {
  const v = Number(speedKmh)
  if (!Number.isFinite(v) || v <= 0) return null
  const raw = 1 + 0.06 * (v - 3)
  const clamped = Math.min(MAX_IMPACT_MULTIPLIER, Math.max(MIN_IMPACT_MULTIPLIER, raw))
  return {
    multiplier: Math.round(clamped * 100) / 100,
    capped: raw > MAX_IMPACT_MULTIPLIER,
  }
}

/**
 * Smallest mass that still presses the tile hard enough to fire.
 *
 * The tile only activates once the downward force reaches its threshold, so a
 * load lighter than this produces no energy. A harder step (higher impact
 * multiplier) lowers the required mass.
 *
 * @param {{ activationThresholdNewtons: number, gravity: number }} hardware
 * @param {number} impactMultiplier
 * @returns {number} Minimum activating mass in kilograms, or Infinity if inputs are invalid.
 */
export function minimumActivationMassKg(hardware, impactMultiplier) {
  const threshold = Number(hardware?.activationThresholdNewtons)
  const g = Number(hardware?.gravity)
  const im = Number(impactMultiplier)
  if (!Number.isFinite(threshold) || threshold <= 0) return Infinity
  if (!Number.isFinite(g) || g <= 0 || !Number.isFinite(im) || im <= 0) return Infinity
  return threshold / (g * im)
}

/**
 * @param {number} massKg
 * @param {number} impactMultiplier
 * @param {number} gravity
 * @returns {number}
 */
export function calculateForceNewtons(massKg, impactMultiplier, gravity) {
  const m = Number(massKg)
  const im = Number(impactMultiplier)
  const g = Number(gravity)
  if (!Number.isFinite(m) || m <= 0 || !Number.isFinite(im) || im < 1 || im > 1.5) return 0
  if (!Number.isFinite(g) || g <= 0) return 0
  return m * g * im
}

/**
 * Joules per successful activation, scaled by effective load.
 *
 * @param {number} massKg
 * @param {number} impactMultiplier
 * @param {{
 *   activationThresholdNewtons: number,
 *   minRatedOutputJoules: number,
 *   maxRatedOutputJoules: number,
 *   maxScaleMassKg: number,
 *   gravity: number,
 * }} hardware
 * @returns {number}
 */
export function joulesPerActivation(massKg, impactMultiplier, hardware) {
  const m = Number(massKg)
  const im = Number(impactMultiplier)
  const threshold = Number(hardware?.activationThresholdNewtons)
  const minJoules = Number(hardware?.minRatedOutputJoules)
  const maxJoules = Number(hardware?.maxRatedOutputJoules)
  const maxScaleMassKg = Number(hardware?.maxScaleMassKg)
  const g = Number(hardware?.gravity)

  if (
    !Number.isFinite(m) ||
    m <= 0 ||
    !Number.isFinite(im) ||
    im < MIN_IMPACT_MULTIPLIER ||
    im > MAX_IMPACT_MULTIPLIER
  ) {
    return 0
  }
  if (
    !Number.isFinite(threshold) ||
    threshold <= 0 ||
    !Number.isFinite(minJoules) ||
    !Number.isFinite(maxJoules) ||
    !Number.isFinite(maxScaleMassKg) ||
    maxScaleMassKg <= 0 ||
    !Number.isFinite(g) ||
    g <= 0
  ) {
    return 0
  }

  const force = m * g * im
  if (force < threshold) return 0

  const minMass = threshold / (g * im)
  if (maxScaleMassKg <= minMass) return minJoules

  const effectiveMass = Math.min(maxScaleMassKg, m * im)
  const span = maxScaleMassKg - minMass
  const t = Math.max(0, Math.min(1, (effectiveMass - minMass) / span))
  const joules = minJoules + t * (maxJoules - minJoules)
  return Math.min(maxJoules, joules)
}

/**
 * @param {number} massKg
 * @param {number} impactMultiplier
 * @param {number} compressionCount
 * @param {{
 *   activationThresholdNewtons: number,
 *   minRatedOutputJoules: number,
 *   maxRatedOutputJoules: number,
 *   maxScaleMassKg: number,
 *   gravity: number,
 * }} hardware
 */
export function calculateActivationHarvest(massKg, impactMultiplier, compressionCount, hardware) {
  const count = Math.max(0, Math.trunc(Number(compressionCount)))
  const force = calculateForceNewtons(massKg, impactMultiplier, hardware.gravity)
  const activated = force >= hardware.activationThresholdNewtons
  const joulesPer = activated ? joulesPerActivation(massKg, impactMultiplier, hardware) : 0
  const successfulActivations = activated ? count : 0
  return {
    forceNewtons: force,
    activationSuccessful: activated,
    joulesPerActivation: joulesPer,
    totalJoules: joulesPer * count,
    totalCompressions: count,
    successfulActivations,
  }
}
