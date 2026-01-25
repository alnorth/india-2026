import { useEffect, useState, lazy, Suspense } from 'react'

const ThumbnailMapComponent = lazy(() => import('./ThumbnailMapComponent'))

interface RouteMapThumbnailProps {
  gpxPath?: string
  coordinates?: { lat: number; lng: number }
}

export default function RouteMapThumbnail({ gpxPath, coordinates }: RouteMapThumbnailProps) {
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
      <div className="w-full h-48 flex items-center justify-center bg-gray-100 dark:bg-gray-700 rounded">
        <p className="text-sm text-gray-500">Loading map...</p>
      </div>
    )
  }

  // If we have GPX path but data hasn't loaded yet, show loading
  if (gpxPath && !gpxData) {
    return (
      <div className="w-full h-48 flex items-center justify-center bg-gray-100 dark:bg-gray-700 rounded">
        <p className="text-sm text-gray-500">Loading route...</p>
      </div>
    )
  }

  // Need either gpxData or coordinates to render
  if (!gpxData && !coordinates) {
    return null
  }

  return (
    <Suspense fallback={
      <div className="w-full h-48 flex items-center justify-center bg-gray-100 dark:bg-gray-700 rounded">
        <p className="text-sm text-gray-500">Loading map...</p>
      </div>
    }>
      <ThumbnailMapComponent gpxData={gpxData} coordinates={coordinates} />
    </Suspense>
  )
}
