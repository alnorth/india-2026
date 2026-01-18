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
        src={`https://www.strava.com/activities/${activityId}/embed/[EMBED_TOKEN]`}
        className="rounded-lg shadow-md"
      />
      <div className="mt-2 text-sm text-gray-600 dark:text-gray-400">
        <p>
          Note: You'll need to replace [EMBED_TOKEN] in the component with your actual Strava embed token.
          Get this from the Strava activity page by clicking Share → Embed Activity.
        </p>
      </div>
    </div>
  )
}
