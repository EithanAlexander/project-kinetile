/** Converts pounds to kilograms. */
export function poundsToKg(lb) {
  return Number(lb) * 0.45359237
}

/** Converts kilograms to pounds. */
export function kgToPounds(kg) {
  return Number(kg) / 0.45359237
}

/** Converts metres per second to kilometres per hour. */
export function metersPerSecondToKmh(mps) {
  return Number(mps) * 3.6
}

/** Converts miles per hour to kilometres per hour (international mile). */
export function mphToKmh(mph) {
  return Number(mph) * 1.609344
}
