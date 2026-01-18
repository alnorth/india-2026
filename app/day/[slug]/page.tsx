import { getAllDays, getDayBySlug } from '@/lib/days'
import { format } from 'date-fns'
import Link from 'next/link'
import { GPXMap } from '@/components/GPXMap'
import { StravaEmbed } from '@/components/StravaEmbed'
import { PhotoGallery } from '@/components/PhotoGallery'

export async function generateStaticParams() {
  const days = await getAllDays()
  return days.map((day) => ({
    slug: day.slug,
  }))
}

export default async function DayPage({ params }: { params: { slug: string } }) {
  const day = await getDayBySlug(params.slug)

  if (!day) {
    return <div>Day not found</div>
  }

  return (
    <div>
      <Link href="/" className="text-blue-600 hover:underline mb-4 inline-block">
        ← Back to all days
      </Link>

      <article className="max-w-4xl">
        <header className="mb-8">
          <h1 className="text-4xl font-bold mb-2">{day.title}</h1>
          <div className="flex gap-4 text-gray-600 dark:text-gray-400">
            <time dateTime={day.date}>
              {format(new Date(day.date), 'EEEE, MMMM d, yyyy')}
            </time>
            {day.distance && <span>{day.distance} km</span>}
            {day.location && <span>{day.location}</span>}
          </div>
          <span className={`inline-block mt-2 px-3 py-1 text-sm rounded ${
            day.status === 'completed' ? 'bg-green-100 text-green-800' :
            day.status === 'in-progress' ? 'bg-yellow-100 text-yellow-800' :
            'bg-gray-100 text-gray-800'
          }`}>
            {day.status}
          </span>
        </header>

        {day.gpxPath && (
          <section className="mb-8">
            <h2 className="text-2xl font-bold mb-4">Route</h2>
            <div className="h-96 rounded-lg overflow-hidden shadow-md">
              <GPXMap gpxPath={day.gpxPath} />
            </div>
          </section>
        )}

        {day.stravaId && (
          <section className="mb-8">
            <h2 className="text-2xl font-bold mb-4">Ride Stats</h2>
            <StravaEmbed activityId={day.stravaId} />
          </section>
        )}

        <section className="mb-8 prose dark:prose-invert max-w-none">
          <div dangerouslySetInnerHTML={{ __html: day.content }} />
        </section>

        {day.photos && day.photos.length > 0 && (
          <section className="mb-8">
            <h2 className="text-2xl font-bold mb-4">Photos</h2>
            <PhotoGallery photos={day.photos} />
          </section>
        )}
      </article>
    </div>
  )
}
