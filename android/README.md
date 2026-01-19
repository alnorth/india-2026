# India 2026 Tour Updater - Android App

A private Android app for updating the India 2026 cycle tour website while on the road. The app allows editing existing day entries - adding photos, updating content, and changing status - directly from a phone.

## Features

- **Day Selection**: Browse and select existing day entries from the GitHub repository
- **Day Editing**: Update status, Strava activity ID, and markdown content
- **Photo Upload**: Select multiple photos from device gallery with captions
- **GitHub Integration**: Creates pull requests automatically with preview links
- **Amplify Preview**: Polls for and displays Amplify preview URLs

## Prerequisites

- Android device running Android 8.0 (API 26) or higher
- GitHub Personal Access Token configured in repository secrets
- APKs are built automatically via GitHub Actions

## Setup Instructions

### 1. Create GitHub Fine-Grained Personal Access Token

We use GitHub's **new fine-grained tokens** (not the older classic tokens) for better security and tighter scoping.

**Detailed Steps:**

1. **Navigate to Token Settings**
   - Go to GitHub.com and click your profile picture (top right)
   - Click **Settings**
   - Scroll down to **Developer settings** (bottom of left sidebar)
   - Click **Personal access tokens**
   - Click **Fine-grained tokens** (NOT "Tokens (classic)")

2. **Generate New Token**
   - Click **Generate new token** button
   - You may be asked to confirm your password

3. **Configure Token Details**
   - **Token name**: `India 2026 Android App` (or any descriptive name)
   - **Expiration**: Choose expiration (recommended: 90 days or 1 year)
   - **Description** (optional): `Token for India 2026 Android app to create PRs and upload photos`

4. **Set Repository Access**
   - Under **Repository access**, select **Only select repositories**
   - Click the **Select repositories** dropdown
   - Search for and select: `alnorth/india-2026`

5. **Set Repository Permissions**
   - Scroll down to **Permissions** section
   - Under **Repository permissions**, find and set:
     - **Contents**: `Read and write` (dropdown)
     - **Pull requests**: `Read and write` (dropdown)
     - **Metadata**: `Read-only` (automatically set)

6. **Generate and Copy Token**
   - Scroll to bottom and click **Generate token**
   - **IMPORTANT**: Copy the token immediately (starts with `github_pat_...`)
   - You won't be able to see it again!
   - Store it securely - you'll need it in the next step

**Note**: Fine-grained tokens are more secure than classic tokens because they:
- Can be scoped to specific repositories only
- Have granular permission controls
- Show up in repository security logs
- Can be revoked per repository

### 2. Configure GitHub Actions

Add the token as a repository secret:

1. Go to Repository Settings > Secrets and variables > Actions
2. Click "New repository secret"
3. Name: `APP_GITHUB_TOKEN`
4. Value: Your GitHub token from step 1
5. Click "Add secret"

### 3. Build the App via GitHub Actions

The app is automatically built when changes are pushed to the `android/` directory:

1. Make changes to the Android app (or trigger workflow manually)
2. Push to the repository
3. GitHub Actions will build the APK automatically
4. Download the APK from the workflow artifacts

### 4. Download and Install APK

1. Go to the **Actions** tab in GitHub
2. Select the latest successful workflow run
3. Scroll down to **Artifacts** section
4. Download either:
   - `debug-apk` - Debug build with logging
   - `release-apk` - Release build (optimized)
5. Extract the ZIP file
6. Transfer the APK to your Android device
7. Install the APK:
   - You may need to enable "Install from Unknown Sources" in Android Settings
   - Open the APK file to install

## GitHub Actions Workflow

The repository includes a GitHub Actions workflow (`.github/workflows/build-android.yml`) that automatically builds the APK.

### Workflow Triggers

The workflow runs when:
- Changes are pushed to the `android/` directory
- Pull requests modify the `android/` directory
- Manually triggered via "Run workflow" button in Actions tab

### Build Outputs

Each successful workflow run produces two artifacts:
- **debug-apk**: Debug build with logging enabled
- **release-apk**: Optimized release build

### Manual Workflow Trigger

To build the app without making changes:

1. Go to **Actions** tab in GitHub
2. Select **Build Android APK** workflow
3. Click **Run workflow** dropdown
4. Select branch (usually `master`)
5. Click **Run workflow** button
6. Wait for the build to complete (2-3 minutes)
7. Download the APK from artifacts

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

- **Hardcoded Token**: The GitHub token is injected into the APK at build time via BuildConfig. This is acceptable since it's a private app for personal use only.
- **No Analytics**: The app does not collect any data or include analytics.
- **Build-Time Injection**: The token is provided via GitHub Actions secrets and never committed to the repository.

## Troubleshooting

### Build fails with "GITHUB_TOKEN not found"

Ensure the `APP_GITHUB_TOKEN` secret is configured in GitHub repository settings (Settings > Secrets and variables > Actions).

### Photos not uploading

Check that you've granted the app permission to access photos in Android Settings:
- Settings > Apps > India 2026 Updater > Permissions > Photos

### API calls failing

Verify your GitHub token:
1. Has the correct permissions (Contents: Read/Write, Pull Requests: Read/Write)
2. Hasn't expired
3. Has access to the `alnorth/india-2026` repository

### GitHub Actions build fails

Check the workflow logs in the Actions tab:
1. Go to Actions > Failed workflow run
2. Click on the failed job
3. Review the error logs
4. Common issues:
   - Missing `APP_GITHUB_TOKEN` secret
   - Gradle dependency resolution failures
   - Insufficient permissions on token

## Making Changes to the App

To modify the app:

1. Edit source files in `android/app/src/main/`
2. Commit changes to the repository
3. Push to GitHub
4. GitHub Actions will automatically build and provide the updated APK
5. Download the new APK from workflow artifacts
6. Install on your device

## License

Private app for personal use only.
