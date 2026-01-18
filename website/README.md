# India 2026 - Coast to Coast Cycle Tour

A static website to track my coast to coast cycle tour across southern India, built with Astro and hosted on AWS Amplify.

## Live Site

- **Production**: [india-2026.alnorth.com](https://india-2026.alnorth.com)
- **Amplify Console**: (Add your Amplify URL here after setup)

## Project Structure

```
india-2026/
├── src/                          # Astro source files
│   ├── pages/                    # Astro pages (routes)
│   │   ├── index.astro           # Home page (listing all days)
│   │   └── day/
│   │       └── [slug].astro      # Dynamic route for individual day pages
│   ├── layouts/                  # Page layouts
│   │   └── BaseLayout.astro      # Root layout (wrapper for all pages)
│   ├── components/               # React components (interactive)
│   │   ├── GPXMap.tsx            # GPX route visualization
│   │   ├── MapComponent.tsx      # Leaflet map wrapper
│   │   ├── PhotoGallery.tsx      # Photo grid and lightbox
│   │   └── StravaEmbed.tsx       # Strava activity embed
│   ├── lib/                      # Utility functions
│   │   └── days.ts               # Functions to read/parse day content
│   └── styles/                   # Global styles
│       └── globals.css           # Tailwind directives and global CSS
├── content/                      # Content files (not in public/)
│   └── days/                     # Daily ride content
│       └── [day-name]/           # Each day has its own folder
│           ├── index.md          # Day metadata and writeup
│           ├── route.gpx         # Planned route (GPX file)
│           └── photos/           # Photos from the day
├── public/                       # Static files served as-is
├── amplify.yml                   # AWS Amplify build configuration
├── astro.config.mjs              # Astro configuration
├── package.json                  # Dependencies
├── tailwind.config.js            # Tailwind CSS configuration
└── tsconfig.json                 # TypeScript configuration
```

## Content File Structure

### Directory Structure for Each Day

Each day of your tour should have its own directory under `content/days/`. The directory name becomes the URL slug for that day.

```
content/days/day-01-kanyakumari-to-nagercoil/
├── index.md              # Required: Day metadata and writeup
├── route.gpx             # Required initially: Planned route
└── photos/               # Optional: Photos from the day
    ├── photo1.jpg
    ├── photo2.jpg
    └── photo3.jpg
```

### Day Metadata (index.md)

Each day's `index.md` file should have frontmatter with metadata followed by your writeup:

```markdown
---
date: 2026-01-20
title: "Day 1: Kanyakumari to Nagercoil"
distance: 45
location: "Kanyakumari, Tamil Nadu"
status: planned
stravaId: ""
---

Your writeup about the day goes here. Before the ride, this might just be a brief description of the planned route. After the ride, you can add:

- How the ride went
- Interesting things you saw
- Challenges faced
- Memorable moments
- etc.
```

#### Frontmatter Fields

- **date** (required): Date in YYYY-MM-DD format
- **title** (required): Title for the day (e.g., "Day 1: Kanyakumari to Nagercoil")
- **distance** (optional): Distance in kilometers
- **location** (optional): Location or region
- **status** (required): One of:
  - `planned` - Route is planned but not yet ridden
  - `in-progress` - Currently riding this day
  - `completed` - Ride completed
- **stravaId** (optional): Strava activity ID (just the number from the URL)

### GPX Files

Before each day, add your planned route as `route.gpx` in the day's directory. The site will automatically display this on a map.

You can export GPX files from:
- Komoot
- Ride with GPS
- Strava routes
- Garmin Connect
- Or any other route planning tool

### Photos

Add photos to the `photos/` directory within each day's folder. Supported formats:
- `.jpg` / `.jpeg`
- `.png`
- `.gif`
- `.webp`

Photos will automatically appear in a gallery at the bottom of each day's page.

### Strava Integration

After completing a ride:

1. Go to your Strava activity
2. Click "Share" → "Embed Activity"
3. Copy the activity ID from the embed code
4. Update your day's `index.md` frontmatter with the `stravaId`

Note: You'll need to update `src/components/StravaEmbed.tsx` to include your embed token. Get this from the Strava embed code.

## Adding a New Day

### Before the Ride

1. Create a new directory in `content/days/` with a descriptive name:
   ```bash
   mkdir -p content/days/day-02-nagercoil-to-trivandrum/photos
   ```

2. Add your GPX route file:
   ```bash
   cp ~/Downloads/day-02-route.gpx content/days/day-02-nagercoil-to-trivandrum/route.gpx
   ```

3. Create the `index.md` file with frontmatter:
   ```bash
   cat > content/days/day-02-nagercoil-to-trivandrum/index.md << 'EOF'
   ---
   date: 2026-01-21
   title: "Day 2: Nagercoil to Trivandrum"
   distance: 85
   location: "Tamil Nadu to Kerala"
   status: planned
   stravaId: ""
   ---

   Planned route description here.
   EOF
   ```

4. Commit and push - Amplify will automatically deploy

### After the Ride

1. Update the status to `completed` in `index.md`
2. Add your Strava activity ID to the frontmatter
3. Write about your experience in the markdown content
4. Add photos to the `photos/` directory
5. Commit and push

## Local Development

### Prerequisites

- Node.js 18+ and npm

### Setup

1. Clone the repository:
   ```bash
   git clone <your-repo-url>
   cd india-2026
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Run the development server:
   ```bash
   npm run dev
   ```

4. Open [http://localhost:4321](http://localhost:4321) in your browser

### Building for Production

```bash
npm run build
```

This generates a static export in the `dist/` directory.

## AWS Amplify Setup

### Initial Setup

1. **Connect Repository**
   - Go to [AWS Amplify Console](https://console.aws.amazon.com/amplify)
   - Click "New app" → "Host web app"
   - Select "GitHub" and authorize Amplify to access your repository
   - Select this repository and the main branch

2. **Configure Build Settings**
   - Amplify should auto-detect the `amplify.yml` file
   - Verify the build settings look correct
   - Click "Next" and "Save and deploy"

3. **Set Up Custom Domain**
   - In your Amplify app, go to "Domain management"
   - Click "Add domain"
   - Enter `india-2026.alnorth.com`
   - Follow the instructions to update your DNS settings
   - Amplify will automatically provision an SSL certificate

### Automatic Deployments

Once set up, Amplify will automatically:
- Build and deploy when you push to the main branch
- Create preview deployments for pull requests
- Update the live site within a few minutes of pushing changes

### Preview Deployments

When you create a pull request:
1. Amplify automatically creates a preview deployment
2. A comment is added to the PR with the preview URL
3. You can review changes before merging
4. Preview deployments are deleted when the PR is closed

## Technologies Used

- **Astro** - Static site generator with React integration
- **TypeScript** - Type-safe JavaScript
- **Tailwind CSS** - Utility-first CSS framework
- **React** - Interactive components (via Astro's React integration)
- **Leaflet** - Interactive maps for GPX route visualization
- **React Leaflet** - React wrapper for Leaflet
- **Gray Matter** - Parse frontmatter from markdown files
- **Marked** - Markdown parser
- **date-fns** - Date formatting

## Tips for the Road

### Publishing Updates

If you have internet access during your tour:
1. Write your daily update in a text editor
2. Update the `index.md` file for that day
3. Upload photos to the `photos/` directory
4. Commit and push - the site will update automatically

### Offline Workflow

If you're offline:
1. Keep daily notes in a text file or journal
2. Save photos locally
3. When you have internet, batch update multiple days at once

### Photo Optimization

Consider optimizing photos before uploading to keep the site fast:
```bash
# Using ImageMagick to resize
mogrify -resize 1920x1920\> -quality 85 content/days/day-*/photos/*.jpg
```

## Support

For issues or questions about the site, create an issue in this repository.

Safe travels! 🚴‍♂️
