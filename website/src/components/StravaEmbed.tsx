interface StravaEmbedProps {
  activityId: string
}

export default function StravaEmbed({ activityId }: StravaEmbedProps) {
  return (
    <div className="w-full">
      <iframe
        height="405"
        width="100%"
        frameBorder="0"
        allowTransparency={true}
        scrolling="no"
        src={`https://strava-embeds.com/activity/${activityId}`}
        className="rounded-lg shadow-md"
      />
    </div>
  )
}
