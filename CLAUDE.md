# CLAUDE.md - Technical Documentation

This document describes the technical architecture and structure of the India 2026 cycle tour static site for future reference by Claude or other developers.

## Architecture Overview

This is a **statically generated site** built with Next.js 14 using the App Router. The site reads content from markdown files with frontmatter and generates static HTML pages at build time. No server-side rendering or API routes are needed.

### Key Technical Decisions

1. **Next.js 14 with App Router** - Modern React framework with excellent static export capabilities
2. **File-based content** - Markdown files with frontmatter, organized by day in the filesystem
3. **Static export** - Full static site generation (SSG), no Node.js server required at runtime
4. **Tailwind CSS** - Utility-first CSS for styling
5. **Leaflet** - Client-side map rendering for GPX routes
6. **AWS Amplify** - Hosting with automatic deployments via GitHub integration

## Directory Structure

```
india-2026/
├── app/                          # Next.js App Router
│   ├── day/[slug]/
│   │   └── page.tsx              # Dynamic route for individual day pages
│   ├── globals.css               # Global styles with Tailwind directives
│   ├── layout.tsx                # Root layout (wrapper for all pages)
│   └── page.tsx                  # Home page (index route)
│
├── components/                   # React components
│   ├── GPXMap.tsx                # Server component wrapper for GPX map
│   ├── MapComponent.tsx          # Client component for Leaflet map
│   ├── PhotoGallery.tsx          # Photo grid with lightbox
│   └── StravaEmbed.tsx           # Strava activity iframe embed
│
├── content/                      # Content files (not in public/)
│   └── days/                     # Daily ride content
│       └── [day-slug]/           # Each day in its own directory
│           ├── index.md          # Frontmatter + markdown content
│           ├── route.gpx         # GPX file for route
│           └── photos/           # Photos for the day
│
├── lib/                          # Utility functions
│   └── days.ts                   # Functions to read/parse day content
│
├── public/                       # Static files served as-is
│
├── amplify.yml                   # AWS Amplify build configuration
├── next.config.js                # Next.js configuration (static export)
├── package.json                  # Dependencies
├── tailwind.config.js            # Tailwind CSS configuration
└── tsconfig.json                 # TypeScript configuration
```

## Component Architecture

### Page Components

#### `app/page.tsx` (Home Page)
- **Type**: Server Component
- **Purpose**: Listing page showing all days
- **Data**: Calls `getAllDays()` to read all day content
- **Rendering**: Maps over days to create summary cards
- **Features**:
  - Status badges (planned/in-progress/completed)
  - Date, distance, location
  - Links to individual day pages

#### `app/day/[slug]/page.tsx` (Day Page)
- **Type**: Server Component
- **Purpose**: Individual day detail page
- **Data**:
  - `generateStaticParams()` returns all slugs for static generation
  - `getDayBySlug(slug)` loads specific day content
- **Sections**:
  - Header with title, date, distance, location, status
  - GPX route map (if GPX file exists)
  - Strava embed (if Strava ID provided)
  - Markdown content (writeup)
  - Photo gallery (if photos exist)

#### `app/layout.tsx` (Root Layout)
- **Type**: Server Component
- **Purpose**: Wrapper for all pages
- **Features**:
  - HTML structure
  - Global metadata
  - Tailwind CSS classes
  - Dark mode support via Tailwind classes

### Feature Components

#### `components/GPXMap.tsx`
- **Type**: Server Component
- **Purpose**: Wrapper that fetches GPX file and passes to MapComponent
- **Data Flow**:
  1. Receives `gpxPath` prop (e.g., `/content/days/day-01/route.gpx`)
  2. Reads GPX file from filesystem
  3. Passes GPX data to client-side MapComponent
- **Implementation**: Uses `fs.readFileSync()` to read GPX file at build time

#### `components/MapComponent.tsx`
- **Type**: Client Component (`'use client'`)
- **Purpose**: Renders interactive Leaflet map with GPX route
- **Implementation**:
  - Parses GPX XML to extract trackpoints (lat/lon coordinates)
  - Uses React Leaflet for map rendering
  - Draws route as blue polyline
  - Auto-fits map bounds to route
  - OpenStreetMap tiles for base map
- **Why Client Component**: Leaflet requires browser APIs (DOM, window)

#### `components/StravaEmbed.tsx`
- **Type**: Client Component
- **Purpose**: Embeds Strava activity via iframe
- **Props**: `activityId` (string)
- **Implementation**:
  - Uses Strava's embed API
  - Requires embed token (user must add after getting from Strava)
  - Falls back to activity link if no token

#### `components/PhotoGallery.tsx`
- **Type**: Client Component
- **Purpose**: Photo grid with lightbox view
- **Props**: `photos` (array of photo URLs)
- **Implementation**:
  - Grid layout using Tailwind CSS
  - Click to view full-size in modal overlay
  - Keyboard navigation (ESC to close, arrow keys to navigate)
  - State management with React hooks

## Data Layer

### `lib/days.ts`

This module handles all content reading and parsing.

#### Data Types

```typescript
interface DayMetadata {
  date: string           // YYYY-MM-DD format
  title: string          // e.g., "Day 1: Kanyakumari to Nagercoil"
  distance?: number      // kilometers
  location?: string      // e.g., "Tamil Nadu"
  status: 'planned' | 'in-progress' | 'completed'
  stravaId?: string      // Strava activity ID
}

interface Day extends DayMetadata {
  slug: string           // directory name, used for URL
  content: string        // HTML (converted from markdown)
  gpxPath?: string       // path to GPX file if exists
  photos?: string[]      // array of photo URLs if exist
}
```

#### Key Functions

**`getAllDays(): Promise<Day[]>`**
- Reads all directories in `content/days/`
- Filters for directories only
- Calls `getDayBySlugSync()` for each
- Sorts by date (earliest first)
- Returns array of Day objects

**`getDayBySlug(slug: string): Promise<Day | null>`**
- Wrapper for `getDayBySlugSync()`
- Returns single Day object or null if not found

**`getDayBySlugSync(slug: string): Day | null`**
- Reads `content/days/[slug]/index.md`
- Parses frontmatter with `gray-matter`
- Converts markdown to HTML with `marked`
- Checks for GPX file existence
- Scans photos directory for images
- Returns complete Day object

## Content File Format

### Frontmatter (YAML)

```yaml
---
date: 2026-01-20
title: "Day 1: Kanyakumari to Nagercoil"
distance: 45
location: "Kanyakumari, Tamil Nadu"
status: planned
stravaId: ""
---
```

### Markdown Content

Standard markdown below frontmatter:
- Headings
- Paragraphs
- Lists
- Links
- etc.

Converted to HTML with `marked` library.

## Static Generation Flow

1. **Build Time** (`npm run build`):
   - Next.js calls `generateStaticParams()` in `app/day/[slug]/page.tsx`
   - This calls `getAllDays()` which reads all content directories
   - Next.js generates static HTML for each day page
   - Static HTML includes map (client-side), Strava embed, photos, etc.
   - Output goes to `out/` directory

2. **Content Access**:
   - GPX files and photos are copied to `out/content/days/...`
   - Referenced via absolute paths in generated HTML
   - Served as static files by hosting provider

3. **Client Hydration**:
   - Static HTML loads in browser
   - React hydrates client components (maps, galleries)
   - Interactive features become active

## AWS Amplify Deployment

### Build Process

Defined in `amplify.yml`:

1. **Pre-build**: `npm ci` (clean install)
2. **Build**: `npm run build` (static export)
3. **Output**: `out/` directory
4. **Base directory**: `/` (project root)

### Automatic Deployments

- **Main branch**: Deploys to production (india-2026.alnorth.com)
- **Pull requests**: Creates preview deployment with unique URL
- **Comments**: Amplify bot adds preview URL to PR

### No GitHub Actions Required

AWS Amplify provides built-in GitHub integration:
- Webhook triggers on push/PR
- Builds in Amplify's infrastructure
- Manages SSL certificates
- Handles CDN distribution

## Styling Approach

### Tailwind CSS

- Utility-first approach
- Configured in `tailwind.config.js`
- Dark mode support via class strategy
- Responsive breakpoints

### Global Styles

`app/globals.css`:
- Tailwind directives (@tailwind)
- Custom CSS for prose (markdown content)
- Leaflet CSS import

### Component Styles

- Inline Tailwind classes
- No CSS modules or styled-components
- Dark mode variants using `dark:` prefix

## Client vs Server Components

### Server Components (Default)
- `app/page.tsx` - No interactivity needed
- `app/day/[slug]/page.tsx` - No interactivity needed
- `app/layout.tsx` - Just structure
- `components/GPXMap.tsx` - Reads files, wraps client component

### Client Components (`'use client'`)
- `components/MapComponent.tsx` - Leaflet needs browser APIs
- `components/StravaEmbed.tsx` - Could be server, but kept client for consistency
- `components/PhotoGallery.tsx` - Interactive lightbox with state

**Rule of Thumb**: Use server components by default. Only use client components when you need:
- Browser APIs (window, document)
- React hooks (useState, useEffect)
- Event handlers
- Third-party libraries that require browser environment

## Dependencies

### Runtime Dependencies
- `react` + `react-dom` - Core framework
- `next` - Framework and static generator
- `leaflet` + `react-leaflet` - Map rendering
- `date-fns` - Date formatting
- `gray-matter` - Parse frontmatter
- `marked` - Markdown to HTML

### Development Dependencies
- `typescript` - Type safety
- `@types/*` - Type definitions
- `tailwindcss` - CSS framework
- `autoprefixer` + `postcss` - CSS processing

## Adding New Features

### Adding a New Day Component Section

1. Create component in `components/`
2. Import in `app/day/[slug]/page.tsx`
3. Add to page layout
4. Update Day interface in `lib/days.ts` if new data needed
5. Update frontmatter documentation in README.md

### Adding New Metadata Fields

1. Update `DayMetadata` interface in `lib/days.ts`
2. Update example frontmatter in `content/days/example-day-01/index.md`
3. Use in `app/day/[slug]/page.tsx` or `app/page.tsx`
4. Document in README.md

### Adding New Data Sources

1. Add parser function to `lib/days.ts`
2. Check file existence in `getDayBySlugSync()`
3. Add to Day interface
4. Create component to display it
5. Use in day page

## Performance Considerations

### Image Optimization

- Currently uses standard `<img>` tags
- **Future improvement**: Use Next.js `<Image>` component for:
  - Automatic optimization
  - Responsive sizes
  - Lazy loading
  - WebP conversion

### GPX File Size

- GPX files can be large
- Consider simplifying tracks (fewer points) for web display
- Could add build-time GPX simplification

### Build Time

- Increases linearly with number of days
- Currently minimal (example day only)
- With 20-30 days, still very fast
- No API calls, just file reads

## Testing Locally

```bash
# Install dependencies
npm install

# Development mode (hot reload)
npm run dev

# Production build (static export)
npm run build

# Serve static export
npx serve out
```

## Common Tasks

### Adding a Day
1. `mkdir -p content/days/day-XX-name/photos`
2. Create `content/days/day-XX-name/index.md` with frontmatter
3. Add `content/days/day-XX-name/route.gpx`
4. Commit and push

### Updating a Day After Riding
1. Edit `content/days/day-XX-name/index.md`:
   - Change status to `completed`
   - Add Strava ID
   - Write about the experience
2. Add photos to `content/days/day-XX-name/photos/`
3. Commit and push

### Customizing Styles
1. Edit Tailwind classes in components
2. For global changes, edit `app/globals.css`
3. For theme colors, edit `tailwind.config.js`

## Troubleshooting

### Map Not Showing
- Check that GPX file exists at correct path
- Check browser console for Leaflet errors
- Ensure `'use client'` directive in MapComponent.tsx

### Build Fails
- Check all frontmatter is valid YAML
- Ensure all required fields present (date, title, status)
- Check for markdown parsing errors

### Photos Not Showing
- Verify photos are in `photos/` subdirectory
- Check file extensions (jpg, jpeg, png, gif, webp)
- Ensure paths are correct in generated HTML

### Strava Embed Not Working
- Add embed token to StravaEmbed.tsx
- Verify Strava ID is correct
- Check Strava activity is public

## Future Enhancements

Potential improvements to consider:

1. **Image optimization** - Use Next.js Image component
2. **GPX elevation profiles** - Parse and display elevation data
3. **Route statistics** - Calculate distance, elevation gain from GPX
4. **Map clustering** - Show all days on a single overview map
5. **RSS feed** - Generate feed of daily updates
6. **Search** - Filter days by location, distance, date
7. **Social sharing** - Meta tags for better social previews
8. **Comments** - Integration with commenting system
9. **Analytics** - Track visitor engagement
10. **PWA** - Make site installable/offline-capable

## Related Documentation

- README.md - User guide and content management
- This file (CLAUDE.md) - Technical architecture
- Next.js docs: https://nextjs.org/docs
- Leaflet docs: https://leafletjs.com/
- AWS Amplify docs: https://docs.amplify.aws/
