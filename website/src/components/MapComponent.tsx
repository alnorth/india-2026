import { MapContainer, TileLayer, Polyline, CircleMarker, useMap } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { useEffect, useMemo } from 'react'
import type { LatLngTuple } from 'leaflet'
import { parseGPX, trackPointsToCoordinates } from '@/lib/gpxUtils'

interface MapComponentProps {
  gpxData?: string | null
  coordinates?: { lat: number; lng: number }
}

function FitBounds({ coordinates }: { coordinates: LatLngTuple[] }) {
  const map = useMap()

  useEffect(() => {
    if (coordinates.length > 0) {
      const bounds = coordinates.reduce(
        (bounds, coord) => bounds.extend(coord),
        new (window as any).L.LatLngBounds(coordinates[0], coordinates[0])
      )
      map.fitBounds(bounds, { padding: [50, 50] })
    }
  }, [coordinates, map])

  return null
}

function SetView({ center, zoom }: { center: LatLngTuple; zoom: number }) {
  const map = useMap()

  useEffect(() => {
    map.setView(center, zoom)
  }, [center, zoom, map])

  return null
}

export default function MapComponent({ gpxData, coordinates }: MapComponentProps) {
  const trackPoints = useMemo(() => gpxData ? parseGPX(gpxData) : [], [gpxData])
  const routeCoordinates = useMemo(() => trackPointsToCoordinates(trackPoints), [trackPoints])

  // Determine whether we're showing a route or a marker
  const hasRoute = routeCoordinates.length > 0
  const hasMarker = !hasRoute && coordinates

  const center: LatLngTuple = hasRoute
    ? routeCoordinates[Math.floor(routeCoordinates.length / 2)]
    : coordinates
      ? [coordinates.lat, coordinates.lng]
      : [8.0883, 77.5385] // Default to southern India

  return (
    <MapContainer
      center={center}
      zoom={hasMarker ? 13 : 10}
      style={{ height: '100%', width: '100%' }}
      className="z-0"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {hasRoute && (
        <>
          <Polyline positions={routeCoordinates} color="#38824f" weight={4} />
          <FitBounds coordinates={routeCoordinates} />
        </>
      )}
      {hasMarker && (
        <>
          <CircleMarker
            center={[coordinates.lat, coordinates.lng]}
            radius={10}
            fillColor="#38824f"
            fillOpacity={0.9}
            color="#ffffff"
            weight={2}
          />
          <SetView center={[coordinates.lat, coordinates.lng]} zoom={13} />
        </>
      )}
    </MapContainer>
  )
}
