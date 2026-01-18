import { MapContainer, TileLayer, Polyline, useMap } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { useEffect, useMemo } from 'react'
import type { LatLngTuple } from 'leaflet'

interface MapComponentProps {
  gpxData: string
}

function parseGPX(gpxString: string): LatLngTuple[] {
  const parser = new DOMParser()
  const xmlDoc = parser.parseFromString(gpxString, 'text/xml')
  const trackPoints = xmlDoc.getElementsByTagName('trkpt')

  const coordinates: LatLngTuple[] = []

  for (let i = 0; i < trackPoints.length; i++) {
    const lat = parseFloat(trackPoints[i].getAttribute('lat') || '0')
    const lon = parseFloat(trackPoints[i].getAttribute('lon') || '0')
    coordinates.push([lat, lon])
  }

  return coordinates
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

export default function MapComponent({ gpxData }: MapComponentProps) {
  const coordinates = useMemo(() => parseGPX(gpxData), [gpxData])

  const center: LatLngTuple = coordinates.length > 0
    ? coordinates[Math.floor(coordinates.length / 2)]
    : [8.0883, 77.5385] // Default to southern India

  return (
    <MapContainer
      center={center}
      zoom={10}
      style={{ height: '100%', width: '100%' }}
      className="z-0"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {coordinates.length > 0 && (
        <>
          <Polyline positions={coordinates} color="blue" weight={4} />
          <FitBounds coordinates={coordinates} />
        </>
      )}
    </MapContainer>
  )
}
