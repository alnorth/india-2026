import {
  MapContainer,
  TileLayer,
  Polyline,
  CircleMarker,
  useMap,
  Tooltip,
} from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { useEffect, useMemo, useCallback } from 'react'
import type { LatLngTuple } from 'leaflet'
import { parseGPX, trackPointsToCoordinates } from '@/lib/gpxUtils'
import type { DayRoute } from './FullRouteMap'

// Color palette for different days - distinct, colorblind-friendly colors
const DAY_COLORS = [
  '#e63946', // Red
  '#fb5607', // Vivid orange (more saturated for visibility)
  '#00b4d8', // Cyan (replaces yellow for better map contrast)
  '#2a9d8f', // Teal
  '#264653', // Dark blue
  '#7209b7', // Purple
  '#3a86ff', // Blue
  '#ff006e', // Pink
  '#8338ec', // Violet
  '#06d6a0', // Mint
  '#118ab2', // Ocean blue
  '#073b4c', // Navy
  '#ef476f', // Coral
]

interface FullRouteMapComponentProps {
  routes: DayRoute[]
  gpxDataMap: Map<number, string>
}

interface RouteData {
  dayNumber: number
  label: string
  slug: string
  coordinates: LatLngTuple[]
  color: string
}

interface MarkerData {
  dayNumber: number
  label: string
  slug: string
  position: LatLngTuple
  color: string
}

function FitAllBounds({ allCoordinates }: { allCoordinates: LatLngTuple[] }) {
  const map = useMap()

  useEffect(() => {
    if (allCoordinates.length > 0) {
      const bounds = allCoordinates.reduce(
        (bounds, coord) => bounds.extend(coord),
        new (window as any).L.LatLngBounds(allCoordinates[0], allCoordinates[0])
      )
      map.fitBounds(bounds, { padding: [30, 30] })
    }
  }, [allCoordinates, map])

  return null
}

export default function FullRouteMapComponent({
  routes,
  gpxDataMap,
}: FullRouteMapComponentProps) {
  // Parse all GPX data and assign colors
  const routeData: RouteData[] = useMemo(() => {
    return routes
      .filter(({ dayNumber }) => gpxDataMap.has(dayNumber))
      .map(({ dayNumber, label, slug }) => {
        const gpxData = gpxDataMap.get(dayNumber)!
        const trackPoints = parseGPX(gpxData)
        const coordinates = trackPointsToCoordinates(trackPoints)
        const colorIndex = (dayNumber - 1) % DAY_COLORS.length
        return {
          dayNumber,
          label,
          slug,
          coordinates,
          color: DAY_COLORS[colorIndex],
        }
      })
  }, [routes, gpxDataMap])

  // Create markers for days with coordinates but no GPX
  const markerData: MarkerData[] = useMemo(() => {
    return routes
      .filter(({ gpxPath, coordinates }) => !gpxPath && coordinates)
      .map(({ dayNumber, label, slug, coordinates }) => {
        const colorIndex = (dayNumber - 1) % DAY_COLORS.length
        return {
          dayNumber,
          label,
          slug,
          position: [coordinates!.lat, coordinates!.lng] as LatLngTuple,
          color: DAY_COLORS[colorIndex],
        }
      })
  }, [routes])

  // Handle route click to navigate to day page
  const handleRouteClick = useCallback((slug: string) => {
    window.location.href = `/day/${slug}/`
  }, [])

  // Combine all coordinates for fitting bounds (routes + markers)
  const allCoordinates = useMemo(() => {
    const routeCoords = routeData.flatMap((route) => route.coordinates)
    const markerCoords = markerData.map((marker) => marker.position)
    return [...routeCoords, ...markerCoords]
  }, [routeData, markerData])

  // Default center (southern India)
  const center: LatLngTuple =
    allCoordinates.length > 0
      ? allCoordinates[Math.floor(allCoordinates.length / 2)]
      : [10.0, 78.0]

  return (
    <div className="relative w-full h-full">
      <MapContainer
        center={center}
        zoom={8}
        style={{ height: '100%', width: '100%' }}
        className="z-0"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {routeData.map((route) => (
          <Polyline
            key={route.dayNumber}
            positions={route.coordinates}
            color={route.color}
            weight={4}
            opacity={0.9}
            eventHandlers={{
              click: () => handleRouteClick(route.slug),
              mouseover: (e) => {
                const layer = e.target
                layer.setStyle({ weight: 6, opacity: 1 })
                layer._path.style.cursor = 'pointer'
              },
              mouseout: (e) => {
                const layer = e.target
                layer.setStyle({ weight: 4, opacity: 0.9 })
              },
            }}
          >
            <Tooltip sticky>{route.label}</Tooltip>
          </Polyline>
        ))}
        {markerData.map((marker) => (
          <CircleMarker
            key={`marker-${marker.dayNumber}`}
            center={marker.position}
            radius={6}
            fillColor={marker.color}
            fillOpacity={0.9}
            color="#ffffff"
            weight={2}
            eventHandlers={{
              click: () => handleRouteClick(marker.slug),
              mouseover: (e) => {
                const layer = e.target
                layer.setStyle({ radius: 8, fillOpacity: 1 })
                layer._path.style.cursor = 'pointer'
              },
              mouseout: (e) => {
                const layer = e.target
                layer.setStyle({ radius: 6, fillOpacity: 0.9 })
              },
            }}
          >
            <Tooltip>{marker.label}</Tooltip>
          </CircleMarker>
        ))}
        {allCoordinates.length > 0 && (
          <FitAllBounds allCoordinates={allCoordinates} />
        )}
      </MapContainer>
    </div>
  )
}
