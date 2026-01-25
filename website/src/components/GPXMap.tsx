import { useEffect, useState, lazy, Suspense } from 'react'

const MapComponent = lazy(() => import('./MapComponent'))

interface GPXMapProps {
  gpxPath?: string
  coordinates?: { lat: number; lng: number }
}

export default function GPXMap({ gpxPath, coordinates }: GPXMapProps) {
  const [gpxData, setGpxData] = useState<string | null>(null)
  const [isClient, setIsClient] = useState(false)

  useEffect(() => {
    setIsClient(true)
    if (gpxPath) {
      fetch(gpxPath)
        .then((res) => res.text())
        .then((data) => setGpxData(data))
        .catch((err) => console.error('Error loading GPX:', err))
    }
  }, [gpxPath])

  if (!isClient) {
    return (
      <div className="w-full h-full flex items-center justify-center bg-gray-100">
        <p>Loading map...</p>
      </div>
    )
  }

  // If we have GPX path but data hasn't loaded yet, show loading
  if (gpxPath && !gpxData) {
    return (
      <div className="w-full h-full flex items-center justify-center bg-gray-100">
        <p>Loading route...</p>
      </div>
    )
  }

  // Need either gpxData or coordinates to render
  if (!gpxData && !coordinates) {
    return null
  }

  return (
    <Suspense fallback={
      <div className="w-full h-full flex items-center justify-center bg-gray-100">
        <p>Loading map...</p>
      </div>
    }>
      <MapComponent gpxData={gpxData} coordinates={coordinates} />
    </Suspense>
  )
}
