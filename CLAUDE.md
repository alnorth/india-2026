# CLAUDE.md - Technical Documentation

This document describes the technical architecture and structure of the India 2026 cycle tour static site for future reference by Claude or other developers.

## Architecture Overview

This is a **statically generated site** built with Astro. The site reads content from markdown files with frontmatter and generates static HTML pages at build time. No server-side rendering or API routes are needed.

### Key Technical Decisions

1. **Astro** - Pure static site generator, never used for SSR
2. **File-based content** - Markdown files with frontmatter, organized by day in the filesystem
3. **Static export** - Full static site generation (SSG), no Node.js server required at runtime
4. **React components** - Used for interactive features via Astro's React integration
5. **Tailwind CSS** - Utility-first CSS for styling
6. **Leaflet** - Client-side map rendering for GPX routes
7. **AWS Amplify** - Hosting with automatic deployments via GitHub integration

## Directory Structure

```
india-2026/
├── src/                          # Astro source files
│   ├── pages/                    # Astro pages (routes)
│   │   ├── index.astro           # Home page (listing all days)
│   │   └── day/
│   │       └── [slug].astro      # Dynamic route for individual day pages
│   │
│   ├── layouts/                  # Page layouts
│   │   └── BaseLayout.astro      # Root layout (wrapper for all pages)
│   │
│   ├── components/               # React components (interactive)
│   │   ├── GPXMap.tsx            # Client component wrapper for GPX map
│   │   ├── MapComponent.tsx      # Client component for Leaflet map
│   │   ├── PhotoGallery.tsx      # Photo grid with lightbox
│   │   └── StravaEmbed.tsx       # Strava activity iframe embed
│   │
│   ├── lib/                      # Utility functions
│   │   └── days.ts               # Functions to read/parse day content
│   │
│   └── styles/                   # Global styles
│       └── globals.css           # Tailwind directives and global CSS
│
├── content/                      # Content files (not in public/)
│   └── days/                     # Daily ride content
│       └── [day-slug]/           # Each day in its own directory
│           ├── index.md          # Frontmatter + markdown content
│           ├── route.gpx         # GPX file for route
│           └── photos/           # Photos for the day
│
├── public/                       # Static files served as-is
│
├── amplify.yml                   # AWS Amplify build configuration
├── astro.config.mjs              # Astro configuration
├── package.json                  # Dependencies
├── tailwind.config.js            # Tailwind CSS configuration
└── tsconfig.json                 # TypeScript configuration
```

## Component Architecture

### Page Components

#### `src/pages/index.astro` (Home Page)
- **Type**: Astro Component (server-rendered at build time)
- **Purpose**: Listing page showing all days
- **Data**: Calls `getAllDays()` to read all day content at build time
- **Rendering**: Maps over days to create summary cards
- **Features**:
  - Status badges (planned/in-progress/completed)
  - Date, distance, location
  - Links to individual day pages

#### `src/pages/day/[slug].astro` (Day Page)
- **Type**: Astro Component (server-rendered at build time)
- **Purpose**: Individual day detail page
- **Data**:
  - `getStaticPaths()` returns all slugs for static generation
  - `getDayBySlug(slug)` loads specific day content
- **Sections**:
  - Header with title, date, distance, location, status
  - GPX route map (if GPX file exists)
  - Strava embed (if Strava ID provided)
  - Markdown content (writeup)
  - Photo gallery (if photos exist)
- **Client directives**: Uses `client:load` for interactive React components

#### `src/layouts/BaseLayout.astro` (Root Layout)
- **Type**: Astro Component
- **Purpose**: Wrapper for all pages
- **Features**:
  - HTML structure
  - Global metadata
  - Imports global styles
  - Header and footer
  - Dark mode support via Tailwind classes

### Feature Components

#### `src/components/GPXMap.tsx`
- **Type**: React Client Component
- **Purpose**: Wrapper that fetches GPX file and dynamically loads MapComponent
- **Data Flow**:
  1. Receives `gpxPath` prop (e.g., `/content/days/day-01/route.gpx`)
  2. Fetches GPX file from client side
  3. Lazy loads MapComponent using React.lazy()
  4. Passes GPX data to MapComponent
- **Implementation**: Uses `fetch()` to load GPX file on client side
- **Why Client Component**: Needs React hooks (useState, useEffect) and browser APIs

#### `src/components/MapComponent.tsx`
- **Type**: React Client Component (lazy loaded)
- **Purpose**: Renders interactive Leaflet map with GPX route
- **Implementation**:
  - Parses GPX XML to extract trackpoints (lat/lon coordinates)
  - Uses React Leaflet for map rendering
  - Draws route as blue polyline
  - Auto-fits map bounds to route
  - OpenStreetMap tiles for base map
- **Why Client Component**: Leaflet requires browser APIs (DOM, window)
- **Lazy loading**: Prevents SSR issues with Leaflet

#### `src/components/StravaEmbed.tsx`
- **Type**: React Client Component
- **Purpose**: Embeds Strava activity via iframe
- **Props**: `activityId` (string)
- **Implementation**:
  - Uses Strava's embed API
  - Requires embed token (user must add after getting from Strava)
  - Falls back to activity link if no token

#### `src/components/PhotoGallery.tsx`
- **Type**: React Client Component
- **Purpose**: Photo grid with lightbox view
- **Props**: `photos` (array of photo URLs)
- **Implementation**:
  - Grid layout using Tailwind CSS
  - Click to view full-size in modal overlay
  - Keyboard navigation (ESC to close, arrow keys to navigate)
  - State management with React hooks

## Data Layer

### `src/lib/days.ts`

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

**`getAllDays(): Day[]`**
- Reads all directories in `content/days/`
- Filters for directories only
- Calls `getDayBySlug()` for each
- Sorts by date (earliest first)
- Returns array of Day objects
- **Note**: Now synchronous (no async needed in Astro)

**`getDayBySlug(slug: string): Day | null`**
- Reads `content/days/[slug]/index.md`
- Parses frontmatter with `gray-matter`
- Converts markdown to HTML with `marked`
- Checks for GPX file existence
- Scans photos directory for images
- Returns complete Day object

**`getAllSlugs(): string[]`**
- Returns array of all day slugs
- Used by `getStaticPaths()` in dynamic route

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
   - Astro calls `getStaticPaths()` in `src/pages/day/[slug].astro`
   - This calls `getAllSlugs()` which reads all content directories
   - Astro generates static HTML for each day page
   - Static HTML includes React component placeholders
   - Output goes to `dist/` directory

2. **Content Access**:
   - GPX files and photos are copied to `dist/content/days/...`
   - Referenced via absolute paths in generated HTML
   - Served as static files by hosting provider

3. **Client Hydration**:
   - Static HTML loads in browser
   - React components with `client:load` directive hydrate
   - Interactive features become active (maps, galleries)
   - Components fetch their own data (e.g., GPX files)

## Astro Client Directives

The site uses Astro's client directives to control when React components load:

- `client:load` - Hydrate component immediately on page load
  - Used for: GPXMap, PhotoGallery, StravaEmbed
  - Best for: Components that need to be interactive immediately

Other available directives (not currently used):
- `client:idle` - Hydrate when browser is idle
- `client:visible` - Hydrate when component is visible
- `client:only` - Never run on server, only on client

## AWS Amplify Deployment

### Build Process

Defined in `amplify.yml`:

1. **Pre-build**: `npm ci` (clean install)
2. **Build**: `npm run build` (Astro static export)
3. **Output**: `dist/` directory
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

### Why Astro Works Better with Amplify

- Astro is purely for static site generation (never SSR)
- No ambiguity about deployment mode
- Simpler build process
- No Next.js server detection issues

## Styling Approach

### Tailwind CSS

- Utility-first approach
- Configured in `tailwind.config.js`
- Content paths: `./src/**/*.{astro,html,js,jsx,md,mdx,ts,tsx,vue}`
- Dark mode support via class strategy
- Responsive breakpoints

### Global Styles

`src/styles/globals.css`:
- Tailwind directives (@tailwind)
- Custom CSS for prose (markdown content)
- Leaflet CSS imported in components

### Component Styles

- Inline Tailwind classes
- No CSS modules or styled-components
- Dark mode variants using `dark:` prefix

## Astro vs React Components

### Astro Components (.astro files)
- Server-rendered at build time
- No JavaScript sent to client (by default)
- Can use frontmatter for data fetching
- Use for: Pages, layouts, static content

### React Components (.tsx files)
- Only run on client when using client directives
- JavaScript sent to client
- Use for: Interactive features, state management
- All React components in this project are client-side only

**Rule of Thumb**: Use Astro components by default. Only use React components when you need:
- Browser APIs (window, document)
- React hooks (useState, useEffect)
- Event handlers
- Third-party libraries that require browser environment

## Dependencies

### Runtime Dependencies
- `astro` - Static site generator
- `@astrojs/react` - React integration for Astro
- `@astrojs/tailwind` - Tailwind CSS integration
- `@astrojs/mdx` - MDX support (optional, for future use)
- `react` + `react-dom` - React library
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

### Adding a New Component Section

1. Create React component in `src/components/`
2. Import in `src/pages/day/[slug].astro`
3. Add with `client:load` directive if interactive
4. Update Day interface in `src/lib/days.ts` if new data needed
5. Update frontmatter documentation in README.md

### Adding New Metadata Fields

1. Update `DayMetadata` interface in `src/lib/days.ts`
2. Update example frontmatter in `content/days/example-day-01/index.md`
3. Use in `src/pages/day/[slug].astro` or `src/pages/index.astro`
4. Document in README.md

### Adding New Data Sources

1. Add parser function to `src/lib/days.ts`
2. Check file existence in `getDayBySlug()`
3. Add to Day interface
4. Create component to display it
5. Use in day page with appropriate client directive

## Performance Considerations

### Image Optimization

- Currently uses standard `<img>` tags
- **Future improvement**: Use Astro's `<Image>` component for:
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

### Bundle Size

- React components are code-split automatically
- Leaflet lazy loaded to prevent SSR issues
- Only interactive components send JavaScript to client

## Testing Locally

```bash
# Install dependencies
npm install

# Development mode (hot reload)
npm run dev

# Production build (static export)
npm run build

# Preview production build
npm run preview
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
2. For global changes, edit `src/styles/globals.css`
3. For theme colors, edit `tailwind.config.js`

## Troubleshooting

### Map Not Showing
- Check that GPX file exists at correct path
- Check browser console for Leaflet errors
- Ensure `client:load` directive on GPXMap component
- Verify MapComponent is lazy loaded

### Build Fails
- Check all frontmatter is valid YAML
- Ensure all required fields present (date, title, status)
- Check for markdown parsing errors
- Verify Tailwind content paths in `tailwind.config.js`

### Photos Not Showing
- Verify photos are in `photos/` subdirectory
- Check file extensions (jpg, jpeg, png, gif, webp)
- Ensure paths are correct in generated HTML

### Strava Embed Not Working
- Add embed token to StravaEmbed.tsx
- Verify Strava ID is correct
- Check Strava activity is public

### SSR Errors with Client Components
- Ensure browser-dependent components use `client:load`
- Use React.lazy() for heavy libraries like Leaflet
- Check that imports don't run at module level

## Migration Notes

### From Next.js to Astro

This project was migrated from Next.js to Astro for better Amplify compatibility:

**Why Astro?**
- Pure static site generator (never SSR)
- No ambiguity about deployment mode
- Simpler configuration
- Better integration with Amplify's static hosting

**Key Changes:**
- `app/` → `src/pages/`
- `.tsx` pages → `.astro` pages
- `generateStaticParams()` → `getStaticPaths()`
- `async` components → synchronous (data loaded at build time)
- Client components need explicit `client:*` directives
- `out/` build directory → `dist/`

## Future Enhancements

Potential improvements to consider:

1. **Image optimization** - Use Astro Image component
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
- Astro docs: https://docs.astro.build
- Leaflet docs: https://leafletjs.com/
- AWS Amplify docs: https://docs.amplify.aws/
