interface StravaEmbedProps {
  // stravaId should be in format "activityId/embedToken" e.g. "13426073740/abc123xyz789"
  // Get this from Strava: Share → Embed Activity → copy the ID and token from the iframe URL
  stravaId: string
}

export default function StravaEmbed({ stravaId }: StravaEmbedProps) {
  // stravaId format: "activityId/embedToken"
  // Results in URL: https://www.strava.com/activities/activityId/embed/embedToken
  return (
    <div className="w-full">
      <iframe
        height="405"
        width="100%"
        frameBorder="0"
        allowTransparency={true}
        scrolling="no"
        src={`https://www.strava.com/activities/${stravaId.replace('/', '/embed/')}`}
        className="rounded-lg shadow-md"
      />
    </div>
  )
}
