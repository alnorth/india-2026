# India 2026 Tour Updater - Android App

A private Android app for updating the India 2026 cycle tour website while on the road. The app allows editing existing day entries - adding photos, updating content, and changing status - directly from a phone.

## Features

- **Day Selection**: Browse and select existing day entries from the GitHub repository
- **Day Editing**: Update status, Strava activity ID, and markdown content
- **Photo Upload**: Select multiple photos from device gallery with captions
- **Automatic Compression**: Images are automatically compressed for web delivery
- **GitHub Integration**: Creates pull requests automatically with preview links
- **Amplify Preview**: Polls for and displays Amplify preview URLs

## Prerequisites

- Android Studio (latest version recommended)
- JDK 17
- GitHub Personal Access Token with `repo` permissions
- Android device or emulator running Android 8.0 (API 26) or higher

## Setup Instructions

### 1. Create GitHub Personal Access Token

1. Go to GitHub Settings > Developer settings > Personal access tokens > Fine-grained tokens
2. Click "Generate new token"
3. Configure:
   - Repository access: `alnorth/india-2026`
   - Permissions:
     - Contents: Read and write
     - Pull requests: Read and write
     - Metadata: Read-only
4. Copy the generated token

### 2. Configure Local Development

Create a `local.properties` file in the `android/` directory:

```properties
GITHUB_TOKEN=ghp_your_token_here
```

**Important**: This file is gitignored and will not be committed.

### 3. Build the App

#### Using Android Studio:
1. Open the `android/` directory in Android Studio
2. Wait for Gradle sync to complete
3. Build > Make Project
4. Run > Run 'app'

#### Using Command Line:
```bash
cd android
./gradlew assembleDebug
```

The APK will be located at: `android/app/build/outputs/apk/debug/app-debug.apk`

### 4. Install on Device

#### Via Android Studio:
- Connect your device via USB
- Enable USB debugging on your device
- Click Run in Android Studio

#### Via ADB:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions CI/CD

The repository includes a GitHub Actions workflow that automatically builds the APK when changes are pushed to the `android/` directory.

### Required GitHub Secrets

Add the following secret to your repository (Settings > Secrets and variables > Actions):

| Secret Name | Description |
|-------------|-------------|
| `APP_GITHUB_TOKEN` | GitHub Personal Access Token for API calls |

### Downloading Built APKs

1. Go to Actions tab in GitHub
2. Select the latest workflow run
3. Download artifacts:
   - `debug-apk`: Debug build
   - `release-apk`: Release build (unsigned)

## App Architecture

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/alnorth/india2026/
│   │   │   ├── api/              # GitHub API integration
│   │   │   ├── repository/       # Data layer
│   │   │   ├── model/            # Data models
│   │   │   ├── ui/
│   │   │   │   ├── screens/      # Compose screens
│   │   │   │   ├── composables/  # Reusable UI components
│   │   │   │   └── theme/        # Material3 theming
│   │   │   └── MainActivity.kt
│   │   ├── res/                  # Android resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── local.properties              # Local config (gitignored)
```

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with ViewModels and StateFlow
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Navigation**: Jetpack Navigation Compose
- **Async**: Kotlin Coroutines

## Usage

1. **Launch the app** - Opens to day list screen
2. **Select a day** - Tap any day to edit
3. **Edit details**:
   - Change status (planned/in-progress/completed)
   - Add Strava activity ID
   - Update markdown content
4. **Add photos**:
   - Tap "Add Photos"
   - Select images from gallery
   - Add captions for each photo
5. **Submit**:
   - Tap "Create Pull Request"
   - Wait for submission (uploads photos and creates PR)
6. **Review**:
   - Open PR on GitHub to review changes
   - Wait for Amplify preview URL
   - Open preview to see changes live

## Permissions

The app requires the following permissions:

- `INTERNET`: For GitHub API communication
- `READ_MEDIA_IMAGES` (Android 13+): For accessing photos
- `READ_EXTERNAL_STORAGE` (Android 12 and below): For accessing photos

## Security Considerations

- **Hardcoded Token**: The GitHub token is hardcoded in the APK via BuildConfig. This is acceptable since it's a private app for personal use only.
- **No Analytics**: The app does not collect any data or include analytics.
- **Local Development**: The `local.properties` file containing the token is gitignored and never committed.

## Troubleshooting

### Build fails with "GITHUB_TOKEN not found"

Ensure you've created `android/local.properties` with your GitHub token.

### Photos not uploading

Check that you've granted the app permission to access photos in Android Settings.

### API calls failing

Verify your GitHub token has the correct permissions and hasn't expired.

### Gradle sync issues

Try:
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

## Development

To modify the app:

1. Make changes to source files in `android/app/src/main/`
2. Test locally using Android Studio
3. Commit changes to the repository
4. GitHub Actions will automatically build the APK

## License

Private app for personal use only.
