import fs from 'fs'
import path from 'path'
import matter from 'gray-matter'
import { marked } from 'marked'

const daysDirectory = path.join(process.cwd(), 'content/days')

export interface DayMetadata {
  date: string
  title: string
  distance?: number
  location?: string
  status: 'planned' | 'in-progress' | 'completed'
  stravaId?: string
}

export interface Day extends DayMetadata {
  slug: string
  content: string
  gpxPath?: string
  photos?: string[]
}

export async function getAllDays(): Promise<Day[]> {
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
      return getDayBySlugSync(folder)
    })
    .filter((day): day is Day => day !== null)
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())

  return days
}

export async function getDayBySlug(slug: string): Promise<Day | null> {
  return getDayBySlugSync(slug)
}

function getDayBySlugSync(slug: string): Day | null {
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

  // Check for GPX file
  const gpxPath = path.join(dayPath, 'route.gpx')
  const hasGpx = fs.existsSync(gpxPath)

  // Check for photos
  const photosPath = path.join(dayPath, 'photos')
  let photos: string[] = []
  if (fs.existsSync(photosPath)) {
    photos = fs
      .readdirSync(photosPath)
      .filter((file) => /\.(jpg|jpeg|png|gif|webp)$/i.test(file))
      .map((file) => `/content/days/${slug}/photos/${file}`)
  }

  return {
    slug,
    ...(data as DayMetadata),
    content: htmlContent as string,
    gpxPath: hasGpx ? `/content/days/${slug}/route.gpx` : undefined,
    photos: photos.length > 0 ? photos : undefined,
  }
}
