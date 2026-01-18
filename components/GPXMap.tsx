'use client'

import { useEffect, useState } from 'react'
import dynamic from 'next/dynamic'

// Dynamically import map component to avoid SSR issues with Leaflet
const MapComponent = dynamic(() => import('./MapComponent'), {
  ssr: false,
  loading: () => (
    <div className="w-full h-full flex items-center justify-center bg-gray-100">
      <p>Loading map...</p>
    </div>
  ),
})

interface GPXMapProps {
  gpxPath: string
}

export function GPXMap({ gpxPath }: GPXMapProps) {
  const [gpxData, setGpxData] = useState<string | null>(null)

  useEffect(() => {
    fetch(gpxPath)
      .then((res) => res.text())
      .then((data) => setGpxData(data))
      .catch((err) => console.error('Error loading GPX:', err))
  }, [gpxPath])

  if (!gpxData) {
    return (
      <div className="w-full h-full flex items-center justify-center bg-gray-100">
        <p>Loading route...</p>
      </div>
    )
  }

  return <MapComponent gpxData={gpxData} />
}
