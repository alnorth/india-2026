/**
 * Convert kilometers to miles
 */
export function kmToMiles(km: number): number {
  return Math.round(km * 0.621371 * 10) / 10 // Round to 1 decimal
}

/**
 * Convert meters to feet
 */
export function metersToFeet(meters: number): number {
  return Math.round(meters * 3.28084)
}

/**
 * Format distance with both metric and imperial
 */
export function formatDistance(km: number): string {
  return `${km} km / ${kmToMiles(km)} mi`
}

/**
 * Format elevation with both metric and imperial
 */
export function formatElevation(meters: number): string {
  return `${meters} m / ${metersToFeet(meters)} ft`
}
