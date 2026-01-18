import { useEffect, useState, useMemo } from 'react'
import { parseGPX, type TrackPoint } from '@/lib/gpxUtils'

interface ElevationProfileProps {
  gpxPath: string
}

interface ElevationPoint {
  distance: number // cumulative distance in km
  elevation: number // elevation in meters
}

function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
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

function processElevationData(trackPoints: TrackPoint[]): ElevationPoint[] {
  const points: ElevationPoint[] = []
  let cumulativeDistance = 0

  for (let i = 0; i < trackPoints.length; i++) {
    if (trackPoints[i].ele !== undefined) {
      if (i > 0) {
        const dist = calculateDistance(
          trackPoints[i - 1].lat,
          trackPoints[i - 1].lon,
          trackPoints[i].lat,
          trackPoints[i].lon
        )
        cumulativeDistance += dist
      }

      points.push({
        distance: cumulativeDistance,
        elevation: trackPoints[i].ele!,
      })
    }
  }

  return points
}

export default function ElevationProfile({ gpxPath }: ElevationProfileProps) {
  const [gpxData, setGpxData] = useState<string | null>(null)
  const [isClient, setIsClient] = useState(false)

  useEffect(() => {
    setIsClient(true)
    fetch(gpxPath)
      .then((res) => res.text())
      .then((data) => setGpxData(data))
      .catch((err) => console.error('Error loading GPX:', err))
  }, [gpxPath])

  const elevationData = useMemo(() => {
    if (!gpxData) return null
    const trackPoints = parseGPX(gpxData)
    return processElevationData(trackPoints)
  }, [gpxData])

  if (!isClient || !elevationData || elevationData.length === 0) {
    return (
      <div className="w-full h-64 flex items-center justify-center bg-gray-100 dark:bg-gray-800 rounded">
        <p className="text-gray-500">
          {!isClient || !gpxData ? 'Loading elevation data...' : 'No elevation data available'}
        </p>
      </div>
    )
  }

  const width = 800
  const height = 300
  const padding = { top: 20, right: 40, bottom: 40, left: 60 }
  const chartWidth = width - padding.left - padding.right
  const chartHeight = height - padding.top - padding.bottom

  const maxDistance = elevationData[elevationData.length - 1].distance
  const minElevation = Math.min(...elevationData.map(p => p.elevation))
  const maxElevation = Math.max(...elevationData.map(p => p.elevation))
  const elevationRange = maxElevation - minElevation

  // Add some padding to the elevation range for better visualization
  const elevationPadding = elevationRange * 0.1
  const minY = minElevation - elevationPadding
  const maxY = maxElevation + elevationPadding
  const yRange = maxY - minY

  // Generate SVG path
  const pathData = elevationData.map((point, index) => {
    const x = padding.left + (point.distance / maxDistance) * chartWidth
    const y = padding.top + chartHeight - ((point.elevation - minY) / yRange) * chartHeight
    return `${index === 0 ? 'M' : 'L'} ${x},${y}`
  }).join(' ')

  // Create area path (filled under the line)
  const areaPath = `${pathData} L ${padding.left + chartWidth},${padding.top + chartHeight} L ${padding.left},${padding.top + chartHeight} Z`

  // Calculate grid lines
  const numYGridLines = 5
  const yGridLines = Array.from({ length: numYGridLines }, (_, i) => {
    const elevation = minY + (yRange / (numYGridLines - 1)) * i
    const y = padding.top + chartHeight - ((elevation - minY) / yRange) * chartHeight
    return { y, elevation }
  })

  const numXGridLines = 5
  const xGridLines = Array.from({ length: numXGridLines }, (_, i) => {
    const distance = (maxDistance / (numXGridLines - 1)) * i
    const x = padding.left + (distance / maxDistance) * chartWidth
    return { x, distance }
  })

  return (
    <div className="w-full bg-white dark:bg-gray-800 rounded-lg shadow-md p-4">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full h-auto"
        style={{ maxHeight: '300px' }}
      >
        <defs>
          <linearGradient id="elevationGradient" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#3b82f6" stopOpacity="0.5" />
            <stop offset="100%" stopColor="#3b82f6" stopOpacity="0.1" />
          </linearGradient>
        </defs>

        {/* Grid lines */}
        {yGridLines.map((line, i) => (
          <g key={`y-grid-${i}`}>
            <line
              x1={padding.left}
              y1={line.y}
              x2={padding.left + chartWidth}
              y2={line.y}
              stroke="#e5e7eb"
              strokeWidth="1"
              className="dark:stroke-gray-700"
            />
            <text
              x={padding.left - 10}
              y={line.y + 4}
              textAnchor="end"
              className="text-xs fill-gray-600 dark:fill-gray-400"
            >
              {Math.round(line.elevation)}m
            </text>
          </g>
        ))}

        {xGridLines.map((line, i) => (
          <g key={`x-grid-${i}`}>
            <line
              x1={line.x}
              y1={padding.top}
              x2={line.x}
              y2={padding.top + chartHeight}
              stroke="#e5e7eb"
              strokeWidth="1"
              className="dark:stroke-gray-700"
            />
            <text
              x={line.x}
              y={padding.top + chartHeight + 20}
              textAnchor="middle"
              className="text-xs fill-gray-600 dark:fill-gray-400"
            >
              {line.distance.toFixed(1)}km
            </text>
          </g>
        ))}

        {/* Area fill */}
        <path
          d={areaPath}
          fill="url(#elevationGradient)"
        />

        {/* Elevation line */}
        <path
          d={pathData}
          fill="none"
          stroke="#3b82f6"
          strokeWidth="2"
        />

        {/* Axis labels */}
        <text
          x={padding.left + chartWidth / 2}
          y={height - 5}
          textAnchor="middle"
          className="text-sm fill-gray-700 dark:fill-gray-300 font-medium"
        >
          Distance
        </text>
        <text
          x={15}
          y={padding.top + chartHeight / 2}
          textAnchor="middle"
          transform={`rotate(-90, 15, ${padding.top + chartHeight / 2})`}
          className="text-sm fill-gray-700 dark:fill-gray-300 font-medium"
        >
          Elevation
        </text>
      </svg>
    </div>
  )
}
