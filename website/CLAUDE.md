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
india-2026/                       # Monorepo root
├── website/                      # Website project
│   ├── src/                      # Astro source files
│   │   ├── pages/                # Astro pages (routes)
│   │   │   ├── index.astro       # Home page (listing all days)
│   │   │   └── day/
│   │   │       └── [slug].astro  # Dynamic route for individual day pages
│   │   │
│   │   ├── layouts/              # Page layouts
│   │   │   └── BaseLayout.astro  # Root layout (wrapper for all pages)
│   │   │
│   │   ├── components/           # React & Astro components
│   │   │   ├── GPXMap.tsx        # Client component wrapper for GPX map
│   │   │   ├── MapComponent.tsx  # Client component for Leaflet map
│   │   │   ├── PhotoGalleryOptimized.astro  # Photo gallery with Sharp optimization
│   │   │   ├── PhotoLightbox.tsx # Lightbox modal for photo viewing
│   │   │   └── StravaEmbed.tsx   # Strava activity iframe embed
│   │   │
│   │   ├── lib/                  # Utility functions
│   │   │   └── days.ts           # Functions to read/parse day content
│   │   │
│   │   └── styles/               # Global styles
│   │       └── globals.css       # Tailwind directives and global CSS
│   │
│   ├── content/                  # Content files (not in public/)
│   │   └── days/                 # Daily ride content
│   │       └── [day-slug]/       # Each day in its own directory
│   │           ├── index.md      # Frontmatter + markdown content
│   │           ├── route.gpx     # GPX file for route
│   │           └── photos/       # Photos for the day
│   │
│   ├── public/                   # Static files served as-is
│   │
│   ├── astro.config.mjs          # Astro configuration
│   ├── package.json              # Dependencies
│   ├── tailwind.config.js        # Tailwind CSS configuration
│   ├── tsconfig.json             # TypeScript configuration
│   ├── CLAUDE.md                 # This file - technical documentation
│   └── README.md                 # Website user guide
│
├── amplify.yml                   # AWS Amplify build configuration (at root)
└── README.md                     # Monorepo overview
```

**Note**: This is a monorepo. The Android app lives in `android/` at the repository root. See the root `CLAUDE.md` for monorepo-level documentation.

## Component Architecture

### Page Components

#### `src/pages/index.astro` (Home Page)
- **Type**: Astro Component (server-rendered at build time)
- **Purpose**: Listing page showing all days
- **Data**: Calls `getAllDays()` to read all day content at build time
- **Rendering**: Maps over days to create summary cards
- **Features**:
  - Status badges (planned/in-progress/completed)
  - Date, location
  - Links to individual day pages

#### `src/pages/day/[slug].astro` (Day Page)
- **Type**: Astro Component (server-rendered at build time)
- **Purpose**: Individual day detail page
- **Data**:
  - `getStaticPaths()` returns all slugs for static generation
  - `getDayBySlug(slug)` loads specific day content
- **Sections**:
  - Header with title, date, location, status
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

#### `src/components/PhotoGalleryOptimized.astro`
- **Type**: Astro Component (with embedded React component)
- **Purpose**: Photo gallery with automatic image optimization and caption support
- **Props**: `slug` (day slug to locate photos)
- **Implementation**:
  - Reads photos from `content/days/{slug}/photos/` directory at build time (filesystem is source of truth)
  - Fetches photo metadata (captions, alt text) from day frontmatter via `getDayBySlug()`
  - Merges filesystem photos with optional caption data
  - Uses Sharp to generate optimized WebP versions:
    - 400x400px thumbnails for grid (80% quality)
    - Max 1920px full-size for lightbox (85% quality)
  - Saves optimized images to `dist/_images/{slug}/`
  - Renders responsive grid layout with Tailwind CSS
  - Uses alt text (or caption as fallback) for accessibility
  - Includes PhotoLightbox component for full-screen viewing with captions
- **Why Astro Component**: Needs file system access at build time for image processing
- **Caption Architecture**: Filesystem determines which photos exist; frontmatter optionally augments with captions

#### `src/components/PhotoLightbox.tsx`
- **Type**: React Client Component
- **Purpose**: Full-screen lightbox modal for photo viewing with caption display
- **Props**:
  - `photos` (array of photo URLs)
  - `captions` (optional array of caption strings)
- **Implementation**:
  - Modal overlay with black background
  - Displays caption below image (if provided)
  - Click to close, or use ESC key
  - Navigate with arrow keys or on-screen buttons
  - Listens for custom events to open specific photos
  - State management with React hooks
- **Why Client Component**: Needs browser APIs (event listeners, keyboard input)

## Data Layer

### `src/lib/days.ts`

This module handles all content reading and parsing.

#### Data Types

```typescript
interface PhotoMetadata {
  file: string           // filename of the photo
  caption?: string       // optional caption text
  alt?: string          // optional alt text for accessibility
}

interface DayMetadata {
  date: string           // YYYY-MM-DD format
  title: string          // e.g., "Day 1: Kanyakumari to Nagercoil"
  location?: string      // e.g., "Tamil Nadu"
  status: 'planned' | 'in-progress' | 'completed'
  stravaId?: string      // Strava activity ID
  photos?: PhotoMetadata[] // optional photo metadata (captions, alt text)
}

interface Day extends DayMetadata {
  slug: string           // directory name, used for URL
  content: string        // HTML (converted from markdown)
  gpxPath?: string       // path to GPX file if exists
  photos?: string[]      // array of photo URLs if exist
  photoMetadata?: Map<string, PhotoMetadata> // map of filename to metadata
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
- Creates photoMetadata Map from frontmatter photos array (for fast lookups)
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
location: "Kanyakumari, Tamil Nadu"
status: planned
stravaId: ""
photos:
  - file: sunrise-beach.jpg
    caption: "Golden sunrise at Alamparai Fort ruins"
    alt: "Orange sunrise over ancient fort ruins on the beach"
  - file: coastal-road.jpg
    caption: "Cycling along the East Coast Road"
---
```

**Photo Metadata (Optional)**:
- The `photos` array is optional and used to add captions/alt text to photos
- The `file` field must match the actual filename in the `photos/` directory
- The filesystem is the source of truth - all photos in the directory will be displayed
- Photos without metadata entries display normally (no caption)
- Photos with metadata in frontmatter but no file on disk are ignored
- `caption` is displayed below the image in the lightbox
- `alt` is used for accessibility (falls back to caption if not provided)

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
  - Used for: GPXMap, PhotoLightbox, StravaEmbed
  - Best for: Components that need to be interactive immediately

Other available directives (not currently used):
- `client:idle` - Hydrate when browser is idle
- `client:visible` - Hydrate when component is visible
- `client:only` - Never run on server, only on client

## AWS Amplify Deployment

### Build Process

Defined in `amplify.yml` (located at repository root):

1. **App root**: `website/` (monorepo subdirectory)
2. **Pre-build**: `cd website && npm ci` (clean install in website directory)
3. **Build**: `npm run build` (Astro static export)
4. **Output**: `website/dist/` directory

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

**IMPORTANT**: The Android app also reads/writes frontmatter. If you add a field here but not in the Android app, the field will be **silently dropped** when users edit content from mobile. See root `CLAUDE.md` for the full checklist.

1. Update `DayMetadata` interface in `src/lib/days.ts`
2. Update `DayEntry` in `android/app/src/main/java/com/alnorth/india2026/model/DayEntry.kt`
3. Update `extractFrontmatter()` in `android/.../repository/GitHubRepository.kt`
4. Update `DayEntry.toMarkdown()` to serialize the new field
5. Update example frontmatter in content files
6. Use in `src/pages/day/[slug].astro` or `src/pages/index.astro`
7. Document in README.md and root CLAUDE.md

### Adding New Data Sources

1. Add parser function to `src/lib/days.ts`
2. Check file existence in `getDayBySlug()`
3. Add to Day interface
4. Create component to display it
5. Use in day page with appropriate client directive

## Performance Considerations

### Image Optimization

- **Automatic optimization enabled**: Photos are automatically optimized at build time using Sharp
- **How it works**:
  - Original photos (from phone, camera, etc.) are placed in `content/days/{slug}/photos/`
  - During build, Sharp generates two optimized versions:
    - **Thumbnail**: 400x400px WebP at 80% quality for gallery grid
    - **Full-size**: Max 1920px WebP at 85% quality for lightbox view
  - Optimized images saved to `dist/_images/{slug}/`
  - Typically reduces file sizes by 70-90%
- **Benefits**:
  - Fast page loads even with large phone photos (3-10MB originals → ~100-500KB optimized)
  - Automatic WebP conversion for modern browsers
  - Lazy loading for improved performance
  - No manual image processing required - just drop photos into the photos folder

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
# Navigate to website directory
cd website

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

### Updating Documentation

**IMPORTANT**: When making changes to the codebase, always update both documentation files:

1. **CLAUDE.md** (this file) - Technical documentation
   - Update component descriptions
   - Document new interfaces/types
   - Add to "Recent Additions" section
   - Update architecture diagrams if needed

2. **README.md** - User guide
   - Update examples if user-facing changes
   - Add new frontmatter fields to examples
   - Update usage instructions
   - Keep file paths current

Both files should stay in sync with the actual codebase. Out-of-date documentation can be more harmful than no documentation.

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

1. **GPX elevation profiles** - Parse and display elevation data
2. **Route statistics** - Calculate distance, elevation gain from GPX
3. **Map clustering** - Show all days on a single overview map
4. **RSS feed** - Generate feed of daily updates
5. **Search** - Filter days by location, distance, date
6. **Social sharing** - Meta tags for better social previews
7. **Comments** - Integration with commenting system
8. **Analytics** - Track visitor engagement
9. **PWA** - Make site installable/offline-capable

## Recent Additions

- **Photo captions** (2026-01-18) - Add optional captions and alt text to photos via frontmatter

## Related Documentation

- README.md - User guide and content management
- This file (CLAUDE.md) - Technical architecture
- Astro docs: https://docs.astro.build
- Leaflet docs: https://leafletjs.com/
- AWS Amplify docs: https://docs.amplify.aws/
