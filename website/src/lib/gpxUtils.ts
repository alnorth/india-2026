import type { LatLngTuple } from 'leaflet'

export interface TrackPoint {
  lat: number
  lon: number
  ele?: number
}

export interface GPXStats {
  distance: number // in kilometers
  elevationGain: number // in meters
  elevationLoss: number // in meters
  minElevation?: number
  maxElevation?: number
}

/**
 * Parse GPX XML string to extract trackpoints with coordinates and elevation
 */
export function parseGPX(gpxString: string): TrackPoint[] {
  const parser = new DOMParser()
  const xmlDoc = parser.parseFromString(gpxString, 'text/xml')
  const trackPoints = xmlDoc.getElementsByTagName('trkpt')

  const points: TrackPoint[] = []

  for (let i = 0; i < trackPoints.length; i++) {
    const lat = parseFloat(trackPoints[i].getAttribute('lat') || '0')
    const lon = parseFloat(trackPoints[i].getAttribute('lon') || '0')

    const eleNode = trackPoints[i].getElementsByTagName('ele')[0]
    const ele = eleNode ? parseFloat(eleNode.textContent || '0') : undefined

    points.push({ lat, lon, ele })
  }

  return points
}

/**
 * Convert trackpoints to LatLngTuple format for Leaflet
 */
export function trackPointsToCoordinates(points: TrackPoint[]): LatLngTuple[] {
  return points.map(p => [p.lat, p.lon] as LatLngTuple)
}

/**
 * Calculate distance between two points using Haversine formula
 * Returns distance in kilometers
 */
function haversineDistance(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371 // Earth's radius in kilometers
  const dLat = toRadians(lat2 - lat1)
  const dLon = toRadians(lon2 - lon1)

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) *
      Math.cos(toRadians(lat2)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2)

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

function toRadians(degrees: number): number {
  return degrees * (Math.PI / 180)
}

/**
 * Calculate statistics from GPX trackpoints
 */
export function calculateGPXStats(points: TrackPoint[]): GPXStats {
  if (points.length === 0) {
    return {
      distance: 0,
      elevationGain: 0,
      elevationLoss: 0,
    }
  }

  // Calculate total distance
  let totalDistance = 0
  for (let i = 1; i < points.length; i++) {
    const dist = haversineDistance(
      points[i - 1].lat,
      points[i - 1].lon,
      points[i].lat,
      points[i].lon
    )
    totalDistance += dist
  }

  // Calculate elevation statistics
  let elevationGain = 0
  let elevationLoss = 0
  let minElevation: number | undefined
  let maxElevation: number | undefined

  const elevations = points.filter(p => p.ele !== undefined).map(p => p.ele!)

  if (elevations.length > 0) {
    minElevation = Math.min(...elevations)
    maxElevation = Math.max(...elevations)

    // Calculate cumulative gain/loss
    for (let i = 1; i < points.length; i++) {
      if (points[i - 1].ele !== undefined && points[i].ele !== undefined) {
        const elevChange = points[i].ele! - points[i - 1].ele!
        if (elevChange > 0) {
          elevationGain += elevChange
        } else {
          elevationLoss += Math.abs(elevChange)
        }
      }
    }
  }

  return {
    distance: Math.round(totalDistance * 10) / 10, // Round to 1 decimal
    elevationGain: Math.round(elevationGain),
    elevationLoss: Math.round(elevationLoss),
    minElevation: minElevation !== undefined ? Math.round(minElevation) : undefined,
    maxElevation: maxElevation !== undefined ? Math.round(maxElevation) : undefined,
  }
}
