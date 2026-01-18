import { MapContainer, TileLayer, Polyline, useMap } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { useEffect, useMemo } from 'react'
import type { LatLngTuple } from 'leaflet'
import { parseGPX, trackPointsToCoordinates } from '@/lib/gpxUtils'

interface ThumbnailMapComponentProps {
  gpxData: string
}

function FitBounds({ coordinates }: { coordinates: LatLngTuple[] }) {
  const map = useMap()

  useEffect(() => {
    if (coordinates.length > 0) {
      const bounds = coordinates.reduce(
        (bounds, coord) => bounds.extend(coord),
        new (window as any).L.LatLngBounds(coordinates[0], coordinates[0])
      )
      map.fitBounds(bounds, { padding: [20, 20] })
    }
  }, [coordinates, map])

  return null
}

export default function ThumbnailMapComponent({ gpxData }: ThumbnailMapComponentProps) {
  const trackPoints = useMemo(() => parseGPX(gpxData), [gpxData])
  const coordinates = useMemo(() => trackPointsToCoordinates(trackPoints), [trackPoints])

  const center: LatLngTuple = coordinates.length > 0
    ? coordinates[Math.floor(coordinates.length / 2)]
    : [8.0883, 77.5385] // Default to southern India

  return (
    <MapContainer
      center={center}
      zoom={10}
      style={{ height: '100%', width: '100%' }}
      className="z-0 rounded"
      zoomControl={false}
      dragging={false}
      scrollWheelZoom={false}
      doubleClickZoom={false}
      touchZoom={false}
      attributionControl={false}
    >
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {coordinates.length > 0 && (
        <>
          <Polyline positions={coordinates} color="#3b82f6" weight={3} />
          <FitBounds coordinates={coordinates} />
        </>
      )}
    </MapContainer>
  )
}
