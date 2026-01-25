import { useState, useEffect } from 'react'

interface PhotoLightboxProps {
  photos: string[]
  captions?: (string | undefined)[]
}

export default function PhotoLightbox({ photos, captions }: PhotoLightboxProps) {
  const [selectedPhoto, setSelectedPhoto] = useState<string | null>(null)
  const [selectedIndex, setSelectedIndex] = useState<number>(0)

  const openPhoto = (photo: string, index: number) => {
    setSelectedPhoto(photo)
    setSelectedIndex(index)
  }

  const closePhoto = () => {
    setSelectedPhoto(null)
  }

  const nextPhoto = () => {
    const nextIndex = (selectedIndex + 1) % photos.length
    setSelectedPhoto(photos[nextIndex])
    setSelectedIndex(nextIndex)
  }

  const prevPhoto = () => {
    const prevIndex = (selectedIndex - 1 + photos.length) % photos.length
    setSelectedPhoto(photos[prevIndex])
    setSelectedIndex(prevIndex)
  }

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!selectedPhoto) return
      if (e.key === 'Escape') closePhoto()
      if (e.key === 'ArrowRight') nextPhoto()
      if (e.key === 'ArrowLeft') prevPhoto()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [selectedPhoto, selectedIndex])

  // Expose openPhoto function to parent via custom event
  useEffect(() => {
    const handleOpenPhoto = (e: CustomEvent) => {
      openPhoto(e.detail.photo, e.detail.index)
    }
    window.addEventListener('openPhotoLightbox', handleOpenPhoto as EventListener)
    return () => window.removeEventListener('openPhotoLightbox', handleOpenPhoto as EventListener)
  }, [])

  if (!selectedPhoto) return null

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-90 z-50 flex items-center justify-center p-4"
      onClick={closePhoto}
    >
      <button
        className="absolute top-4 right-4 text-white text-4xl hover:text-gray-300 z-10"
        onClick={closePhoto}
        aria-label="Close"
      >
        &times;
      </button>
      <button
        className="absolute left-0 top-1/2 -translate-y-1/2 text-white text-5xl hover:text-gray-300 hover:bg-white/10 z-10 px-6 py-16 rounded-r-lg transition-colors"
        onClick={(e) => {
          e.stopPropagation()
          prevPhoto()
        }}
        aria-label="Previous photo"
      >
        ‹
      </button>
      <button
        className="absolute right-0 top-1/2 -translate-y-1/2 text-white text-5xl hover:text-gray-300 hover:bg-white/10 z-10 px-6 py-16 rounded-l-lg transition-colors"
        onClick={(e) => {
          e.stopPropagation()
          nextPhoto()
        }}
        aria-label="Next photo"
      >
        ›
      </button>
      <div className="relative max-w-7xl max-h-full w-full h-full flex flex-col items-center justify-center px-16">
        <img
          src={selectedPhoto}
          alt="Selected photo"
          className="max-w-full max-h-full object-contain"
          onClick={(e) => e.stopPropagation()}
        />
        {captions && captions[selectedIndex] && (
          <p className="text-white text-center mt-4 text-lg max-w-3xl">
            {captions[selectedIndex]}
          </p>
        )}
      </div>
    </div>
  )
}
