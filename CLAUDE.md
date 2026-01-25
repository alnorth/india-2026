# India 2026 Monorepo

This repository contains:
- **website/** - Astro static site for the cycle tour blog
- **android/** - Android app for updating content from mobile

## Critical: Keeping Website and Android App in Sync

The Android app reads and writes markdown files with YAML frontmatter. When modifying the frontmatter schema, **both codebases must be updated together**:

### Frontmatter Fields (Source of Truth)

| Field | Type | Website Location | Android Location |
|-------|------|------------------|------------------|
| date | string | `DayMetadata` in `website/src/lib/days.ts` | `DayEntry` in `android/.../model/DayEntry.kt` |
| title | string | `DayMetadata` | `DayEntry` |
| location | string | `DayMetadata` | `DayEntry` |
| status | string | `DayMetadata` | `DayEntry` |
| stravaId | string? | `DayMetadata` | `DayEntry` |
| coordinates | string? | `DayMetadata` | `DayEntry` |
| photos | array | `PhotoMetadata[]` | `List<PhotoWithCaption>` |

### When Adding a New Frontmatter Field

1. **Website**: Update `DayMetadata` interface in `website/src/lib/days.ts`
2. **Android**: Update `DayEntry` data class in `android/app/src/main/java/com/alnorth/india2026/model/DayEntry.kt`
3. **Android**: Update `extractFrontmatter()` in `android/.../repository/GitHubRepository.kt` to parse the new field
4. **Android**: Update `DayEntry.toMarkdown()` to serialize the new field (otherwise it will be lost on save!)
5. **Documentation**: Update this file and `website/CLAUDE.md`

### Why This Matters

The Android app's `toMarkdown()` function rebuilds the entire frontmatter when saving. If a field exists in the markdown but isn't in `DayEntry`, **it will be silently dropped**. Always update both codebases when changing the schema.

## Development

### Website
```bash
cd website
npm install
npm run dev
```

### Android
Open `android/` in Android Studio. The app requires a GitHub token configured in the build.

## Content Location

Day content lives in `website/content/days/`:
```
website/content/days/
├── day--1-mahabalipuram/
│   ├── index.md          # Frontmatter + markdown content
│   ├── route.gpx         # Optional GPX route file
│   └── photos/           # Photos for the day
├── day-01-alamparai-pondicherry/
│   └── ...
```

## See Also

- `website/CLAUDE.md` - Detailed website technical documentation
- `android/README.md` - Android app documentation
- `docs/android-app-brief.md` - Full Android app specification
