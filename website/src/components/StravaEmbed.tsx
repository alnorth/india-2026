import { useEffect } from 'react'

interface StravaEmbedProps {
  // stravaId should be the numeric activity ID from the Strava URL
  // e.g. for strava.com/activities/13426073740 use "13426073740"
  stravaId: string
}

export default function StravaEmbed({ stravaId }: StravaEmbedProps) {
  useEffect(() => {
    // Load Strava embed script
    const script = document.createElement('script')
    script.src = 'https://strava-embeds.com/embed.js'
    script.async = true
    document.body.appendChild(script)

    return () => {
      // Cleanup on unmount
      document.body.removeChild(script)
    }
  }, [])

  return (
    <div className="w-full">
      <div
        className="strava-embed-placeholder"
        data-embed-type="activity"
        data-embed-id={stravaId}
        data-style="standard"
      />
    </div>
  )
}
