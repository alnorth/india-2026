import { useEffect, useState, lazy, Suspense } from 'react'

const FullRouteMapComponent = lazy(() => import('./FullRouteMapComponent'))

export interface DayRoute {
  dayNumber: number
  label: string
  gpxPath: string
  slug: string
}

interface FullRouteMapProps {
  routes: DayRoute[]
}

export default function FullRouteMap({ routes }: FullRouteMapProps) {
  const [gpxDataMap, setGpxDataMap] = useState<Map<number, string>>(new Map())
  const [isClient, setIsClient] = useState(false)
  const [loadingCount, setLoadingCount] = useState(0)

  useEffect(() => {
    setIsClient(true)
    setLoadingCount(routes.length)

    // Fetch all GPX files in parallel
    routes.forEach(({ dayNumber, gpxPath }) => {
      fetch(gpxPath)
        .then((res) => res.text())
        .then((data) => {
          setGpxDataMap((prev) => {
            const newMap = new Map(prev)
            newMap.set(dayNumber, data)
            return newMap
          })
          setLoadingCount((prev) => prev - 1)
        })
        .catch((err) => {
          console.error(`Error loading GPX for day ${dayNumber}:`, err)
          setLoadingCount((prev) => prev - 1)
        })
    })
  }, [routes])

  if (!isClient || loadingCount > 0) {
    return (
      <div className="w-full h-full flex items-center justify-center bg-sand-100 dark:bg-earth-800">
        <p className="text-sand-600 dark:text-sand-400">
          Loading routes... ({routes.length - loadingCount}/{routes.length})
        </p>
      </div>
    )
  }

  return (
    <Suspense
      fallback={
        <div className="w-full h-full flex items-center justify-center bg-sand-100 dark:bg-earth-800">
          <p className="text-sand-600 dark:text-sand-400">Loading map...</p>
        </div>
      }
    >
      <FullRouteMapComponent routes={routes} gpxDataMap={gpxDataMap} />
    </Suspense>
  )
}
