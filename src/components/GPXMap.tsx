import { useEffect, useState, lazy, Suspense } from 'react'

const MapComponent = lazy(() => import('./MapComponent'))

interface GPXMapProps {
  gpxPath: string
}

export default function GPXMap({ gpxPath }: GPXMapProps) {
  const [gpxData, setGpxData] = useState<string | null>(null)
  const [isClient, setIsClient] = useState(false)

  useEffect(() => {
    setIsClient(true)
    fetch(gpxPath)
      .then((res) => res.text())
      .then((data) => setGpxData(data))
      .catch((err) => console.error('Error loading GPX:', err))
  }, [gpxPath])

  if (!isClient || !gpxData) {
    return (
      <div className="w-full h-full flex items-center justify-center bg-gray-100">
        <p>Loading route...</p>
      </div>
    )
  }

  return (
    <Suspense fallback={
      <div className="w-full h-full flex items-center justify-center bg-gray-100">
        <p>Loading map...</p>
      </div>
    }>
      <MapComponent gpxData={gpxData} />
    </Suspense>
  )
}
