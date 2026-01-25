import fs from 'fs'
import path from 'path'
import matter from 'gray-matter'
import { marked } from 'marked'
import { parseGPXServer, calculateGPXStats, type GPXStats } from './gpxServerUtils'

const daysDirectory = path.join(process.cwd(), 'content/days')

export interface PhotoMetadata {
  file: string
  caption?: string
  alt?: string
}

export interface DayMetadata {
  date: string
  title: string
  location?: string
  status: 'planned' | 'in-progress' | 'completed'
  stravaId?: string
  coordinates?: string // Format: "lat,lng" e.g. "12.6269,80.1927"
  photos?: PhotoMetadata[]
}

export interface ParsedCoordinates {
  lat: number
  lng: number
}

export interface Day extends DayMetadata {
  slug: string
  content: string
  gpxPath?: string
  photos?: string[]
  photoMetadata?: Map<string, PhotoMetadata>
  gpxStats?: GPXStats
  parsedCoordinates?: ParsedCoordinates
}

export function getAllDays(): Day[] {
  // Check if directory exists
  if (!fs.existsSync(daysDirectory)) {
    return []
  }

  const dayFolders = fs.readdirSync(daysDirectory)
  const days = dayFolders
    .filter((folder) => {
      const dayPath = path.join(daysDirectory, folder)
      return fs.statSync(dayPath).isDirectory()
    })
    .map((folder) => {
      return getDayBySlug(folder)
    })
    .filter((day): day is Day => day !== null)
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())

  return days
}

export function getDayBySlug(slug: string): Day | null {
  const dayPath = path.join(daysDirectory, slug)

  if (!fs.existsSync(dayPath)) {
    return null
  }

  const indexPath = path.join(dayPath, 'index.md')

  if (!fs.existsSync(indexPath)) {
    return null
  }

  const fileContents = fs.readFileSync(indexPath, 'utf8')
  const { data, content } = matter(fileContents)

  const htmlContent = marked(content)

  // Check for GPX file and parse stats
  const gpxPath = path.join(dayPath, 'route.gpx')
  const hasGpx = fs.existsSync(gpxPath)
  let gpxStats: GPXStats | undefined

  if (hasGpx) {
    try {
      const gpxContent = fs.readFileSync(gpxPath, 'utf8')
      const trackPoints = parseGPXServer(gpxContent)
      gpxStats = calculateGPXStats(trackPoints)
    } catch (error) {
      console.error(`Error parsing GPX for ${slug}:`, error)
    }
  }

  // Check for photos
  const photosPath = path.join(dayPath, 'photos')
  let photos: string[] = []
  if (fs.existsSync(photosPath)) {
    photos = fs
      .readdirSync(photosPath)
      .filter((file) => /\.(jpg|jpeg|png|gif|webp)$/i.test(file))
      .map((file) => `/content/days/${slug}/photos/${file}`)
  }

  // Create photo metadata map from frontmatter
  let photoMetadata: Map<string, PhotoMetadata> | undefined
  const metadata = data as DayMetadata
  if (metadata.photos && metadata.photos.length > 0) {
    photoMetadata = new Map()
    metadata.photos.forEach((photoMeta) => {
      photoMetadata!.set(photoMeta.file, photoMeta)
    })
  }

  // Parse coordinates string into lat/lng object
  let parsedCoordinates: ParsedCoordinates | undefined
  if (metadata.coordinates) {
    const parts = metadata.coordinates.split(',')
    if (parts.length === 2) {
      const lat = parseFloat(parts[0].trim())
      const lng = parseFloat(parts[1].trim())
      if (!isNaN(lat) && !isNaN(lng)) {
        parsedCoordinates = { lat, lng }
      }
    }
  }

  return {
    slug,
    ...(data as DayMetadata),
    content: htmlContent as string,
    gpxPath: hasGpx ? `/content/days/${slug}/route.gpx` : undefined,
    photos: photos.length > 0 ? photos : undefined,
    photoMetadata,
    gpxStats,
    parsedCoordinates,
  }
}

export function getAllSlugs(): string[] {
  if (!fs.existsSync(daysDirectory)) {
    return []
  }

  return fs
    .readdirSync(daysDirectory)
    .filter((folder) => {
      const dayPath = path.join(daysDirectory, folder)
      return fs.statSync(dayPath).isDirectory()
    })
}
