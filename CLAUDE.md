# India 2026 Monorepo

This repository contains a documentation website and mobile app for a cycle tour through South India.

## Project Overview

- **website/** - Astro static site for the cycle tour blog (deployed at india-2026.alnorth.com)
- **android/** - Android app for updating content from mobile while on the road

### Current Status

The tour consists of 15 days (day -1 through day 13), covering Tamil Nadu and Kerala. Content is updated in real-time during the tour via the Android app or direct commits.

## Repository Structure

```
india-2026/
├── website/                      # Astro static website
│   ├── src/
│   │   ├── pages/               # Astro pages (routes)
│   │   ├── components/          # React & Astro components
│   │   ├── lib/                 # Utility functions
│   │   └── styles/              # Global CSS
│   ├── content/days/            # Day content (markdown + photos + GPX)
│   ├── public/                  # Static assets
│   ├── CLAUDE.md               # Website technical documentation
│   └── README.md               # Website user guide
│
├── android/                      # Android companion app
│   ├── app/src/main/java/com/alnorth/india2026/
│   │   ├── api/                 # GitHub API client
│   │   ├── model/               # Data models (DayEntry, etc.)
│   │   ├── repository/          # Data layer
│   │   └── ui/                  # Jetpack Compose UI
│   └── README.md               # Android setup guide
│
├── .github/workflows/           # GitHub Actions
│   └── build-android.yml       # Automated APK builds
│
├── amplify.yml                  # AWS Amplify build config
├── CLAUDE.md                    # This file (monorepo docs)
└── README.md                    # Project overview
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| Website Framework | Astro 5.x (static site generation) |
| UI Components | React 18 (for interactive features) |
| Styling | Tailwind CSS |
| Maps | Leaflet + React Leaflet |
| Hosting | AWS Amplify |
| Android UI | Jetpack Compose + Material Design 3 |
| Android Language | Kotlin |
| Content Format | Markdown with YAML frontmatter |

## Critical: Keeping Website and Android App in Sync

The Android app reads and writes markdown files with YAML frontmatter. When modifying the frontmatter schema, **both codebases must be updated together**.

### Frontmatter Fields (Source of Truth)

| Field | Type | Required | Website Location | Android Location |
|-------|------|----------|------------------|------------------|
| date | string | Yes | `DayMetadata` in `website/src/lib/days.ts` | `DayEntry` in `android/.../model/DayEntry.kt` |
| title | string | Yes | `DayMetadata` | `DayEntry` |
| location | string | No | `DayMetadata` | `DayEntry` |
| status | string | Yes | `DayMetadata` (`'planned' \| 'in-progress' \| 'completed'`) | `DayEntry` |
| stravaId | string | No | `DayMetadata` | `DayEntry` |
| coordinates | string | No | `DayMetadata` (format: `"lat,lng"`) | `DayEntry` |
| photos | array | No | `PhotoMetadata[]` | `List<PhotoWithCaption>` |

### Photo Metadata Structure

```yaml
photos:
  - file: "photo-1.jpg"
    caption: "Description shown in lightbox"
    alt: "Accessibility text (optional, falls back to caption)"
```

### When Adding a New Frontmatter Field

1. **Website**: Update `DayMetadata` interface in `website/src/lib/days.ts`
2. **Website**: Update any components that need the new field
3. **Android**: Update `DayEntry` data class in `android/app/src/main/java/com/alnorth/india2026/model/DayEntry.kt`
4. **Android**: Update `extractFrontmatter()` in `android/.../repository/GitHubRepository.kt` to parse the new field
5. **Android**: Update `DayEntry.toMarkdown()` to serialize the new field (otherwise it will be lost on save!)
6. **Documentation**: Update this file and `website/CLAUDE.md`

### Why This Matters

The Android app's `toMarkdown()` function rebuilds the entire frontmatter when saving. If a field exists in the markdown but isn't in `DayEntry`, **it will be silently dropped**. Always update both codebases when changing the schema.

## Development

### Website

```bash
cd website
npm install
npm run dev      # Development server at localhost:4321
npm run build    # Production build to dist/
npm run preview  # Preview production build
```

### Android

Open `android/` in Android Studio. The app requires a GitHub token configured in repository secrets. See `android/README.md` for detailed setup instructions.

APKs are automatically built via GitHub Actions when changes are pushed to the `android/` directory.

## Content Location

Day content lives in `website/content/days/`:

```
website/content/days/
├── day--1-mahabalipuram/        # Planning day (note: double dash for -1)
│   ├── index.md                 # Frontmatter + markdown content
│   ├── route.gpx               # Optional GPX route file
│   └── photos/                 # Photos for the day
│       └── *.jpg
├── day-0-mahabalipuram/
├── day-01-alamparai-pondicherry/
├── day-02-pondicherry-to-lakshmi-villas/
└── ...through day-13-fort-kochi/
```

### Day Directory Naming

- Use format: `day-XX-slug-name` (e.g., `day-01-alamparai-pondicherry`)
- Negative days use double dash: `day--1-mahabalipuram`
- Slugs should be lowercase with hyphens

### Example Frontmatter

```yaml
---
date: 2026-01-26
title: "Day 1: Alamparai to Pondicherry"
location: "Tamil Nadu"
status: completed
stravaId: "17180591077"
photos:
  - file: "photo-1.jpg"
    caption: "My steed for the next two weeks."
  - file: "photo-2.jpg"
    caption: "A temple along the route."
---

Markdown content goes here...
```

## Website Components

### Interactive Components (React)

| Component | Purpose |
|-----------|---------|
| `GPXMap.tsx` | Fetches GPX and renders route map |
| `MapComponent.tsx` | Leaflet map with route polyline |
| `ElevationProfile.tsx` | Elevation chart from GPX data |
| `RouteMapThumbnail.tsx` | Small map preview for day cards |
| `ThumbnailMapComponent.tsx` | Lightweight map for thumbnails |
| `FullRouteMap.tsx` | Overview map showing all days |
| `FullRouteMapComponent.tsx` | Full route map rendering |
| `PhotoLightbox.tsx` | Full-screen photo viewer with captions |
| `StravaEmbed.tsx` | Embedded Strava activity |

### Static Components (Astro)

| Component | Purpose |
|-----------|---------|
| `PhotoGalleryOptimized.astro` | Photo grid with automatic image optimization |
| `BaseLayout.astro` | Root HTML layout wrapper |

## Deployment

### Website (AWS Amplify)

- **Automatic deployments**: Pushes to main branch deploy to production
- **Preview deployments**: Pull requests get unique preview URLs
- **Build config**: `amplify.yml` at repository root

### Android (GitHub Actions)

- **Automatic builds**: Changes to `android/` trigger APK builds
- **Artifacts**: Debug and release APKs available in workflow artifacts
- **Manual trigger**: Can trigger builds manually from Actions tab

## Key Files Reference

| File | Purpose |
|------|---------|
| `website/src/lib/days.ts` | Content parsing, DayMetadata interface |
| `website/src/lib/gpxServerUtils.ts` | GPX parsing and stats calculation |
| `website/src/pages/day/[slug].astro` | Individual day page template |
| `website/src/pages/index.astro` | Home page with day listing |
| `android/.../model/DayEntry.kt` | Android data model for days |
| `android/.../repository/GitHubRepository.kt` | GitHub API integration |

## See Also

- `website/CLAUDE.md` - Detailed website technical documentation
- `android/README.md` - Android app documentation
- `docs/android-app-brief.md` - Full Android app specification
