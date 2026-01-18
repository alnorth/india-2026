# Android App Product Brief: India 2026 Tour Updater

## Overview

A private Android app for updating the India 2026 cycle tour website while on the road. The app allows the rider to edit existing day entries - adding photos, updating content, and changing status - directly from their phone. Updates are submitted via GitHub Pull Requests, with Amplify preview links for verification before merging.

## Problem Statement

While cycling through India, the rider needs a simple way to update the tour website from their phone without:
- Needing a laptop or desktop computer
- Understanding Git commands or GitHub's web interface
- Manually formatting markdown files
- Dealing with complex file upload processes

## Target User

Single user (the cyclist) - this is a private app, not for public distribution.

## Core Features

### 1. Day Selection
- Fetch list of existing days from GitHub repository
- Display days with title, date, and current status
- Select a day to edit

### 2. Day Entry Editing
- Edit existing day entries:
  - Status (planned/in-progress/completed)
  - Strava activity ID
  - Written content (markdown supported)
- View current values before editing

### 3. Photo Selection & Upload
- Pick multiple photos from device gallery
- Add caption for each photo
- Preview selected photos before upload
- Automatic image compression for web
- Photos added to day's `photos/` directory
- Photo captions stored in frontmatter

### 4. GitHub Integration
- Hardcoded GitHub token (single-user private app)
- Create feature branch automatically
- Commit updated markdown and new photos
- Create Pull Request with descriptive title/body

### 5. Preview & Status
- Show link to created PR
- Poll for Amplify preview URL
- Display preview link when available
- Option to open links in browser

## User Flow

```
┌─────────────────┐
│   Launch App    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Loading Days   │
│  (from GitHub)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Select Day     │
│  to Edit        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Edit Details   │
│  - Status       │
│  - Strava ID    │
│  - Content      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Add Photos     │
│  (from gallery) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│    Preview      │
│   & Submit      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Creating PR... │
│  (progress UI)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Success!       │
│  - PR Link      │
│  - Preview Link │
└─────────────────┘
```

## Technical Requirements

### Platform
- Android 8.0+ (API level 26+)
- Kotlin with Jetpack Compose
- Material Design 3

### Permissions Required
- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` (Android 13+)
- `INTERNET`

### Dependencies
- Retrofit for GitHub API calls
- Coil for image loading/compression
- Kotlin Coroutines for async operations
- Jetpack Navigation

### Monorepo Structure
The Android app lives in the same repository as the website:
```
india-2026/
├── android/          # Android app source code
├── website/          # Astro website source
│   └── content/
│       └── days/     # Day entries (markdown + photos)
└── docs/             # Documentation
```

### GitHub API Integration
- Repository: `alnorth/india-2026`
- Base branch: `master`
- Content path: `website/website/content/days/`
- Feature branch naming: `app/day-XX-YYYY-MM-DD`
- Required scopes: `repo` (for private repo access)
- **Token**: Hardcoded in app (single-user private app)

### Build & Distribution
- APK built via GitHub Actions (triggered by changes in `android/`)
- Artifact available for download from workflow runs
- No Play Store distribution (private app)

## Security Considerations

1. **GitHub Token**
   - Hardcoded in BuildConfig (not committed to public repo)
   - Token stored in `local.properties` (gitignored)
   - Injected at build time via Gradle

2. **No Analytics**
   - No tracking or data collection
   - Completely offline-capable (draft saving)

## Non-Goals

- iOS version
- Multi-user support
- Offline GitHub sync (requires connectivity to submit)
- Photo editing features
- GPX recording (use dedicated cycling app)

## Success Criteria

1. Can create a complete day entry in under 5 minutes
2. Photos upload without quality loss issues
3. PR and preview links accessible within 2 minutes of submission
4. Works reliably on mobile data connection
5. APK size under 20MB

---

# Implementation Guide

## Step 1: Project Setup

### 1.1 Create Android Project

```bash
# Using Android Studio or command line
# Project name: India2026Updater
# Package: com.alnorth.india2026
# Minimum SDK: 26 (Android 8.0)
# Language: Kotlin
# Build: Gradle with Kotlin DSL
```

### 1.2 Configure build.gradle.kts (app level)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.alnorth.india2026"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.alnorth.india2026"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image loading & compression
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
}
```

### 1.3 Configure GitHub Token (via local.properties)

Add to `android/local.properties` (this file is gitignored):

```properties
GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

Update `android/app/build.gradle.kts` to inject the token:

```kotlin
android {
    // ... existing config ...

    defaultConfig {
        // ... existing config ...

        // Load token from local.properties
        val localProperties = java.util.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }

        buildConfigField(
            "String",
            "GITHUB_TOKEN",
            "\"${localProperties.getProperty("GITHUB_TOKEN", "")}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig generation
    }
}
```

For GitHub Actions builds, the token is passed via secrets (see Step 9).

## Step 2: GitHub API Service

### 2.1 Create API Interface

```kotlin
// app/src/main/java/com/alnorth/india2026/api/GitHubApi.kt

interface GitHubApi {

    // Fetch list of days (directories in website/content/days/)
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getDirectoryContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String
    ): List<GitHubContent>

    // Fetch a specific file's contents
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String
    ): GitHubContent

    @GET("repos/{owner}/{repo}/branches/{branch}")
    suspend fun getBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): BranchResponse

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateBranchRequest
    ): RefResponse

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body request: UpdateFileRequest
    ): FileResponse

    @POST("repos/{owner}/{repo}/pulls")
    suspend fun createPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreatePullRequestRequest
    ): PullRequestResponse

    @GET("repos/{owner}/{repo}/pulls/{pull_number}/comments")
    suspend fun getPullRequestComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int
    ): List<PullRequestComment>
}
```

### 2.2 Data Classes

```kotlin
// app/src/main/java/com/alnorth/india2026/api/Models.kt

// For fetching existing content from GitHub
data class GitHubContent(
    val name: String,
    val path: String,
    val sha: String,
    val type: String,  // "file" or "dir"
    val content: String?,  // Base64 encoded (only for files)
    val encoding: String?
)

data class BranchResponse(
    val name: String,
    val commit: CommitRef
)

data class CommitRef(
    val sha: String
)

data class CreateBranchRequest(
    val ref: String,  // "refs/heads/branch-name"
    val sha: String   // SHA of commit to branch from
)

data class RefResponse(
    val ref: String,
    val url: String
)

// For updating existing files (requires sha) or creating new ones
data class UpdateFileRequest(
    val message: String,
    val content: String,  // Base64 encoded
    val branch: String,
    val sha: String? = null  // Required when updating existing file
)

data class FileResponse(
    val content: FileContent,
    val commit: CommitInfo
)

data class FileContent(
    val path: String,
    val sha: String
)

data class CommitInfo(
    val sha: String
)

data class CreatePullRequestRequest(
    val title: String,
    val body: String,
    val head: String,  // Branch name
    val base: String   // Target branch (main)
)

data class PullRequestResponse(
    val number: Int,
    val html_url: String,
    val head: PullRequestHead
)

data class PullRequestHead(
    val ref: String
)

data class PullRequestComment(
    val body: String,
    val user: GitHubUser
)

data class GitHubUser(
    val login: String
)
```

## Step 3: Repository Layer

### 3.1 GitHub Repository

```kotlin
// app/src/main/java/com/alnorth/india2026/repository/GitHubRepository.kt

class GitHubRepository(
    private val api: GitHubApi
) {
    private val owner = "alnorth"
    private val repo = "india-2026"
    private val baseBranch = "master"

    // Fetch all existing days from the repository
    suspend fun getAllDays(): Result<List<DaySummary>> = runCatching {
        val contents = api.getDirectoryContents(owner, repo, "website/content/days")
        contents
            .filter { it.type == "dir" }
            .map { dir ->
                // Fetch the index.md for each day to get metadata
                val indexContent = api.getFileContent(owner, repo, "${dir.path}/index.md")
                val markdown = String(Base64.decode(indexContent.content, Base64.DEFAULT))
                parseDaySummary(dir.name, markdown, indexContent.sha)
            }
            .sortedBy { it.date }
    }

    // Fetch a specific day's full content
    suspend fun getDayBySlug(slug: String): Result<DayEntry> = runCatching {
        val indexContent = api.getFileContent(owner, repo, "website/content/days/$slug/index.md")
        val markdown = String(Base64.decode(indexContent.content, Base64.DEFAULT))
        parseDayEntry(slug, markdown, indexContent.sha)
    }

    // Update an existing day entry
    suspend fun updateDayEntry(
        dayEntry: DayEntry,
        newPhotos: List<SelectedPhoto>,
        context: Context
    ): Result<SubmissionResult> = runCatching {

        // 1. Get latest commit SHA from master
        val masterBranch = api.getBranch(owner, repo, baseBranch)
        val baseSha = masterBranch.commit.sha

        // 2. Create feature branch
        val branchName = "app/${dayEntry.slug}-${System.currentTimeMillis()}"
        api.createBranch(
            owner, repo,
            CreateBranchRequest(
                ref = "refs/heads/$branchName",
                sha = baseSha
            )
        )

        // 3. Upload new photos first (to get filenames)
        val existingPhotoCount = getExistingPhotoCount(dayEntry.slug)
        val uploadedPhotos = mutableListOf<PhotoWithCaption>()

        newPhotos.forEachIndexed { index, selectedPhoto ->
            val photoBytes = compressImage(context, selectedPhoto.uri)
            val photoNum = existingPhotoCount + index + 1
            val filename = "photo-${photoNum}.jpg"
            val photoPath = "website/content/days/${dayEntry.slug}/photos/$filename"

            api.createOrUpdateFile(
                owner, repo, photoPath,
                UpdateFileRequest(
                    message = "Add photo $photoNum",
                    content = Base64.encodeToString(photoBytes, Base64.NO_WRAP),
                    branch = branchName
                )
            )

            uploadedPhotos.add(PhotoWithCaption(filename, selectedPhoto.caption))
        }

        // 4. Update markdown file with photo captions
        val markdownContent = dayEntry.toMarkdown(uploadedPhotos)
        val markdownPath = "website/content/days/${dayEntry.slug}/index.md"
        api.createOrUpdateFile(
            owner, repo, markdownPath,
            UpdateFileRequest(
                message = "Update ${dayEntry.title}",
                content = Base64.encodeToString(
                    markdownContent.toByteArray(),
                    Base64.NO_WRAP
                ),
                branch = branchName,
                sha = dayEntry.fileSha  // Required to update existing file
            )
        )

        // 5. Create Pull Request
        val pr = api.createPullRequest(
            owner, repo,
            CreatePullRequestRequest(
                title = "Update ${dayEntry.title}",
                body = buildPrBody(dayEntry, newPhotos.size),
                head = branchName,
                base = baseBranch
            )
        )

        SubmissionResult(
            prNumber = pr.number,
            prUrl = pr.html_url,
            branchName = branchName
        )
    }

    private suspend fun getExistingPhotoCount(slug: String): Int {
        return try {
            val contents = api.getDirectoryContents(owner, repo, "website/content/days/$slug/photos")
            contents.count { it.type == "file" && it.name.endsWith(".jpg") }
        } catch (e: Exception) {
            0  // No photos directory yet
        }
    }

    suspend fun getAmplifyPreviewUrl(prNumber: Int): String? {
        val comments = api.getPullRequestComments(owner, repo, prNumber)
        val amplifyComment = comments.find {
            it.user.login == "aws-amplify-us-east-1" ||
            it.body.contains("amplifyapp.com")
        }
        return amplifyComment?.body?.let { body ->
            val regex = Regex("""https://[a-z0-9]+\.amplifyapp\.com[^\s\)]*""")
            regex.find(body)?.value
        }
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val maxDimension = 1920
        val scale = if (bitmap.width > bitmap.height) {
            maxDimension.toFloat() / bitmap.width
        } else {
            maxDimension.toFloat() / bitmap.height
        }

        val scaledBitmap = if (scale < 1) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }

    private fun parseDaySummary(slug: String, markdown: String, sha: String): DaySummary {
        // Parse frontmatter to extract summary info
        val frontmatter = extractFrontmatter(markdown)
        return DaySummary(
            slug = slug,
            title = frontmatter["title"] ?: slug,
            date = frontmatter["date"] ?: "",
            status = frontmatter["status"] ?: "planned",
            distance = frontmatter["distance"]?.toIntOrNull() ?: 0,
            location = frontmatter["location"] ?: ""
        )
    }

    private fun parseDayEntry(slug: String, markdown: String, sha: String): DayEntry {
        val frontmatter = extractFrontmatter(markdown)
        val photos = parsePhotos(markdown)
        val content = markdown.substringAfter("---").substringAfter("---").trim()
        return DayEntry(
            slug = slug,
            fileSha = sha,
            date = frontmatter["date"] ?: "",
            title = frontmatter["title"] ?: slug,
            distance = frontmatter["distance"]?.toIntOrNull() ?: 0,
            location = frontmatter["location"] ?: "",
            status = frontmatter["status"] ?: "planned",
            stravaId = frontmatter["stravaId"]?.ifEmpty { null },
            content = content,
            photos = photos
        )
    }

    private fun extractFrontmatter(markdown: String): Map<String, String> {
        val frontmatterSection = markdown
            .substringAfter("---")
            .substringBefore("---")
            .trim()

        return frontmatterSection.lines()
            .filter { it.contains(":") && !it.startsWith("  ") && !it.startsWith("-") }
            .associate { line ->
                val key = line.substringBefore(":").trim()
                val value = line.substringAfter(":").trim().removeSurrounding("\"")
                key to value
            }
    }

    private fun parsePhotos(markdown: String): List<PhotoWithCaption> {
        val frontmatterSection = markdown
            .substringAfter("---")
            .substringBefore("---")

        // Simple YAML list parser for photos
        val photos = mutableListOf<PhotoWithCaption>()
        var currentFilename: String? = null

        val inPhotosSection = frontmatterSection.contains("photos:")
        if (!inPhotosSection) return emptyList()

        val photosSection = frontmatterSection
            .substringAfter("photos:")
            .substringBefore("\n---")

        photosSection.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("- filename:") -> {
                    currentFilename = trimmed
                        .substringAfter("filename:")
                        .trim()
                        .removeSurrounding("\"")
                }
                trimmed.startsWith("caption:") && currentFilename != null -> {
                    val caption = trimmed
                        .substringAfter("caption:")
                        .trim()
                        .removeSurrounding("\"")
                    photos.add(PhotoWithCaption(currentFilename!!, caption))
                    currentFilename = null
                }
            }
        }
        return photos
    }

    private fun buildPrBody(entry: DayEntry, newPhotoCount: Int): String {
        return buildString {
            appendLine("## ${entry.title}")
            appendLine()
            appendLine("**Date:** ${entry.date}")
            appendLine("**Distance:** ${entry.distance} km")
            appendLine("**Location:** ${entry.location}")
            appendLine("**Status:** ${entry.status}")
            appendLine()
            appendLine("### Content Preview")
            appendLine(entry.content.take(500))
            if (entry.content.length > 500) appendLine("...")
            appendLine()
            if (newPhotoCount > 0) {
                appendLine("### New Photos Added: $newPhotoCount")
                appendLine()
            }
            appendLine("---")
            appendLine("*Submitted via India 2026 Android App*")
        }
    }
}

// Summary for day list display
data class DaySummary(
    val slug: String,
    val title: String,
    val date: String,
    val status: String,
    val distance: Int,
    val location: String
)

data class SubmissionResult(
    val prNumber: Int,
    val prUrl: String,
    val branchName: String
)
```

### 3.2 API Client Setup

```kotlin
// app/src/main/java/com/alnorth/india2026/api/ApiClient.kt

object ApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.GITHUB_TOKEN}")
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val gitHubApi: GitHubApi = retrofit.create(GitHubApi::class.java)
    val repository = GitHubRepository(gitHubApi)
}
```

## Step 4: Day Entry Model

```kotlin
// app/src/main/java/com/alnorth/india2026/model/DayEntry.kt

data class PhotoWithCaption(
    val filename: String,
    val caption: String
)

data class DayEntry(
    val slug: String,           // Directory name (e.g., "day-01-alamparai-pondicherry")
    val fileSha: String,        // Git SHA of index.md (required for updates)
    val date: String,           // YYYY-MM-DD (read-only, from existing file)
    val title: String,          // Read-only, from existing file
    val distance: Int,          // Read-only, from existing file
    val location: String,       // Read-only, from existing file
    val status: String,         // Editable: planned, in-progress, completed
    val stravaId: String?,      // Editable: Strava activity ID
    val content: String,        // Editable: markdown content
    val photos: List<PhotoWithCaption>  // Existing photos with captions
) {
    fun toMarkdown(newPhotos: List<PhotoWithCaption> = emptyList()): String = buildString {
        appendLine("---")
        appendLine("date: $date")
        appendLine("title: \"$title\"")
        appendLine("distance: $distance")
        appendLine("location: \"$location\"")
        appendLine("status: $status")
        stravaId?.let { if (it.isNotEmpty()) appendLine("stravaId: \"$it\"") }

        // Combine existing photos with new photos
        val allPhotos = photos + newPhotos
        if (allPhotos.isNotEmpty()) {
            appendLine("photos:")
            allPhotos.forEach { photo ->
                appendLine("  - filename: \"${photo.filename}\"")
                appendLine("    caption: \"${photo.caption}\"")
            }
        }

        appendLine("---")
        appendLine()
        appendLine(content)
    }
}
```

### Frontmatter Photo Format

Photos are stored in the frontmatter as a YAML list:

```yaml
---
date: 2026-01-20
title: "Day 1: Alamparai to Pondicherry"
distance: 45
location: "Tamil Nadu"
status: completed
stravaId: "1234567890"
photos:
  - filename: "photo-1.jpg"
    caption: "Starting point at Alamparai Fort"
  - filename: "photo-2.jpg"
    caption: "Lunch stop at a roadside dhaba"
  - filename: "photo-3.jpg"
    caption: "Arriving in Pondicherry"
---
```

## Step 5: Photo Picker with Captions

```kotlin
// app/src/main/java/com/alnorth/india2026/ui/composables/PhotoPicker.kt

// Represents a photo selected from gallery with its caption
data class SelectedPhoto(
    val uri: Uri,
    val caption: String = ""
)

@Composable
fun PhotoPickerSection(
    selectedPhotos: List<SelectedPhoto>,
    onPhotosChanged: (List<SelectedPhoto>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        val newPhotos = uris.map { SelectedPhoto(uri = it, caption = "") }
        onPhotosChanged(selectedPhotos + newPhotos)
    }

    // Fallback for older Android versions
    val legacyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val newPhotos = uris.map { SelectedPhoto(uri = it, caption = "") }
        onPhotosChanged(selectedPhotos + newPhotos)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "New Photos (${selectedPhotos.size})",
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        legacyPickerLauncher.launch("image/*")
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add Photos")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (selectedPhotos.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                itemsIndexed(selectedPhotos) { index, photo ->
                    PhotoWithCaptionCard(
                        photo = photo,
                        onCaptionChanged = { newCaption ->
                            val updated = selectedPhotos.toMutableList()
                            updated[index] = photo.copy(caption = newCaption)
                            onPhotosChanged(updated)
                        },
                        onRemove = {
                            onPhotosChanged(selectedPhotos - photo)
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No photos selected",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun PhotoWithCaptionCard(
    photo: SelectedPhoto,
    onCaptionChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Photo thumbnail
            AsyncImage(
                model = photo.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(Modifier.width(12.dp))

            // Caption input
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = photo.caption,
                    onValueChange = onCaptionChanged,
                    label = { Text("Caption") },
                    placeholder = { Text("Describe this photo...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )
            }

            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

## Step 6: Day Selection Screen

```kotlin
// app/src/main/java/com/alnorth/india2026/ui/screens/DayListScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayListScreen(
    viewModel: DayListViewModel = viewModel(),
    onDaySelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDays()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("India 2026 - Select Day") }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DayListUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is DayListUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.days) { day ->
                        DayCard(
                            day = day,
                            onClick = { onDaySelected(day.slug) }
                        )
                    }
                }
            }

            is DayListUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadDays() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCard(
    day: DaySummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = day.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${day.date} • ${day.distance} km • ${day.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "completed" -> MaterialTheme.colorScheme.primary to "Done"
        "in-progress" -> MaterialTheme.colorScheme.tertiary to "Today"
        else -> MaterialTheme.colorScheme.outline to "Planned"
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

class DayListViewModel : ViewModel() {
    private val repository = ApiClient.repository

    private val _uiState = MutableStateFlow<DayListUiState>(DayListUiState.Loading)
    val uiState: StateFlow<DayListUiState> = _uiState.asStateFlow()

    fun loadDays() {
        viewModelScope.launch {
            _uiState.value = DayListUiState.Loading
            repository.getAllDays()
                .onSuccess { days ->
                    _uiState.value = DayListUiState.Success(days)
                }
                .onFailure { e ->
                    _uiState.value = DayListUiState.Error(e.message ?: "Failed to load days")
                }
        }
    }
}

sealed class DayListUiState {
    object Loading : DayListUiState()
    data class Success(val days: List<DaySummary>) : DayListUiState()
    data class Error(val message: String) : DayListUiState()
}
```

## Step 7: Edit Day Screen

```kotlin
// app/src/main/java/com/alnorth/india2026/ui/screens/EditDayScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDayScreen(
    slug: String,
    viewModel: EditDayViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToResult: (SubmissionResult) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(slug) {
        viewModel.loadDay(slug)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Day") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is EditDayUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is EditDayUiState.Editing -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Read-only header info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = state.dayEntry.title,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${state.dayEntry.date} • ${state.dayEntry.distance} km • ${state.dayEntry.location}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Status dropdown (editable)
                    StatusDropdown(
                        selectedStatus = state.status,
                        onStatusSelected = viewModel::updateStatus
                    )

                    // Strava ID (editable)
                    OutlinedTextField(
                        value = state.stravaId,
                        onValueChange = viewModel::updateStravaId,
                        label = { Text("Strava Activity ID") },
                        placeholder = { Text("e.g., 1234567890") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Content (editable)
                    OutlinedTextField(
                        value = state.content,
                        onValueChange = viewModel::updateContent,
                        label = { Text("Content (Markdown)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 10
                    )

                    HorizontalDivider()

                    // Photo picker
                    PhotoPickerSection(
                        selectedPhotos = state.newPhotos,
                        onPhotosSelected = viewModel::updatePhotos
                    )

                    Spacer(Modifier.height(16.dp))

                    // Submit button
                    Button(
                        onClick = { viewModel.submit(context) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.hasChanges
                    ) {
                        Text("Create Pull Request")
                    }
                }
            }

            is EditDayUiState.Submitting -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(state.message)
                    }
                }
            }

            is EditDayUiState.Success -> {
                LaunchedEffect(state.result) {
                    onNavigateToResult(state.result)
                }
            }

            is EditDayUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadDay(slug) }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusDropdown(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("planned", "in-progress", "completed")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedStatus.replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onStatusSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

class EditDayViewModel : ViewModel() {
    private val repository = ApiClient.repository

    private val _uiState = MutableStateFlow<EditDayUiState>(EditDayUiState.Loading)
    val uiState: StateFlow<EditDayUiState> = _uiState.asStateFlow()

    private var originalEntry: DayEntry? = null

    fun loadDay(slug: String) {
        viewModelScope.launch {
            _uiState.value = EditDayUiState.Loading
            repository.getDayBySlug(slug)
                .onSuccess { entry ->
                    originalEntry = entry
                    _uiState.value = EditDayUiState.Editing(
                        dayEntry = entry,
                        status = entry.status,
                        stravaId = entry.stravaId ?: "",
                        content = entry.content,
                        newPhotos = emptyList(),
                        hasChanges = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = EditDayUiState.Error(e.message ?: "Failed to load day")
                }
        }
    }

    fun updateStatus(status: String) {
        updateEditingState { it.copy(status = status) }
    }

    fun updateStravaId(stravaId: String) {
        updateEditingState { it.copy(stravaId = stravaId) }
    }

    fun updateContent(content: String) {
        updateEditingState { it.copy(content = content) }
    }

    fun updatePhotos(photos: List<SelectedPhoto>) {
        updateEditingState { it.copy(newPhotos = photos) }
    }

    private fun updateEditingState(update: (EditDayUiState.Editing) -> EditDayUiState.Editing) {
        val current = _uiState.value
        if (current is EditDayUiState.Editing) {
            val updated = update(current)
            val hasChanges = originalEntry?.let { orig ->
                updated.status != orig.status ||
                updated.stravaId != (orig.stravaId ?: "") ||
                updated.content != orig.content ||
                updated.newPhotos.isNotEmpty()
            } ?: false
            _uiState.value = updated.copy(hasChanges = hasChanges)
        }
    }

    fun submit(context: Context) {
        val current = _uiState.value
        if (current !is EditDayUiState.Editing) return

        viewModelScope.launch {
            _uiState.value = EditDayUiState.Submitting("Updating day entry...")

            val updatedEntry = current.dayEntry.copy(
                status = current.status,
                stravaId = current.stravaId.ifEmpty { null },
                content = current.content
            )

            repository.updateDayEntry(updatedEntry, current.newPhotos, context)
                .onSuccess { result ->
                    _uiState.value = EditDayUiState.Success(result)
                }
                .onFailure { e ->
                    _uiState.value = EditDayUiState.Error(e.message ?: "Failed to create PR")
                }
        }
    }
}

sealed class EditDayUiState {
    object Loading : EditDayUiState()
    data class Editing(
        val dayEntry: DayEntry,
        val status: String,
        val stravaId: String,
        val content: String,
        val newPhotos: List<SelectedPhoto>,  // Photos with captions
        val hasChanges: Boolean
    ) : EditDayUiState()
    data class Submitting(val message: String) : EditDayUiState()
    data class Success(val result: SubmissionResult) : EditDayUiState()
    data class Error(val message: String) : EditDayUiState()
}
```

## Step 8: Result Screen with PR and Preview Links

```kotlin
// app/src/main/java/com/alnorth/india2026/ui/screens/ResultScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: SubmissionResult,
    viewModel: ResultViewModel = viewModel()
) {
    val previewUrl by viewModel.previewUrl.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(result.prNumber) {
        viewModel.pollForPreviewUrl(result.prNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submission Complete") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Success icon
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
            }

            Text(
                "Pull Request Created!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // PR Link Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Pull Request #${result.prNumber}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.prUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open PR on GitHub")
                    }
                }
            }

            // Preview Link Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Amplify Preview",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (previewUrl == null) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (previewUrl != null) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(previewUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Preview, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Preview")
                        }
                    } else {
                        Text(
                            "Waiting for Amplify to build preview...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "This usually takes 1-2 minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Create another button
            Button(
                onClick = { /* Navigate back to create screen */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Another Entry")
            }
        }
    }
}

class ResultViewModel : ViewModel() {
    private val repository: GitHubRepository = /* inject */

    private val _previewUrl = MutableStateFlow<String?>(null)
    val previewUrl: StateFlow<String?> = _previewUrl.asStateFlow()

    fun pollForPreviewUrl(prNumber: Int) {
        viewModelScope.launch {
            // Poll every 15 seconds for up to 5 minutes
            repeat(20) {
                val url = repository.getAmplifyPreviewUrl(prNumber)
                if (url != null) {
                    _previewUrl.value = url
                    return@launch
                }
                delay(15_000)
            }
        }
    }
}
```

## Step 9: GitHub Actions Workflow for APK Build

Create the workflow file:

```yaml
# .github/workflows/build-android.yml

name: Build Android APK

on:
  push:
    branches: [main]
    paths:
      - 'android/**'
      - '.github/workflows/build-android.yml'
  pull_request:
    branches: [main]
    paths:
      - 'android/**'
  workflow_dispatch:  # Allow manual trigger

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x android/gradlew

      - name: Inject GitHub Token
        run: echo "GITHUB_TOKEN=${{ secrets.APP_GITHUB_TOKEN }}" >> android/local.properties

      - name: Build Debug APK
        working-directory: android
        run: ./gradlew assembleDebug

      - name: Build Release APK
        working-directory: android
        run: ./gradlew assembleRelease

      - name: Sign Release APK
        uses: r0adkll/sign-android-release@v1
        id: sign_app
        with:
          releaseDirectory: android/app/build/outputs/apk/release
          signingKeyBase64: ${{ secrets.SIGNING_KEY }}
          alias: ${{ secrets.KEY_ALIAS }}
          keyStorePassword: ${{ secrets.KEY_STORE_PASSWORD }}
          keyPassword: ${{ secrets.KEY_PASSWORD }}
        env:
          BUILD_TOOLS_VERSION: "34.0.0"

      - name: Upload Debug APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: android/app/build/outputs/apk/debug/app-debug.apk
          retention-days: 30

      - name: Upload Signed Release APK
        uses: actions/upload-artifact@v4
        with:
          name: release-apk
          path: ${{ steps.sign_app.outputs.signedReleaseFile }}
          retention-days: 90

      - name: Create Release (on tag)
        if: startsWith(github.ref, 'refs/tags/v')
        uses: softprops/action-gh-release@v1
        with:
          files: ${{ steps.sign_app.outputs.signedReleaseFile }}
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Step 10: Setting Up GitHub Secrets for APK Signing

### 10.1 Generate a Keystore

```bash
# Run locally (not in CI)
keytool -genkey -v -keystore india2026-release.keystore \
  -alias india2026 -keyalg RSA -keysize 2048 -validity 10000
```

### 10.2 Convert Keystore to Base64

```bash
base64 -i india2026-release.keystore -o keystore-base64.txt
```

### 10.3 Add GitHub Secrets

In your repository settings, add these secrets:

| Secret Name | Value |
|-------------|-------|
| `APP_GITHUB_TOKEN` | GitHub Personal Access Token for API calls |
| `SIGNING_KEY` | Contents of keystore-base64.txt |
| `KEY_ALIAS` | `india2026` (or your alias) |
| `KEY_STORE_PASSWORD` | Your keystore password |
| `KEY_PASSWORD` | Your key password |

## Step 11: Project Directory Structure

```
india-2026/
├── android/                          # Android app source (NEW)
│   ├── app/
│   │   ├── src/
│   │   │   └── main/
│   │   │       ├── java/com/alnorth/india2026/
│   │   │       │   ├── api/
│   │   │       │   │   ├── GitHubApi.kt
│   │   │       │   │   ├── ApiClient.kt
│   │   │       │   │   └── Models.kt
│   │   │       │   ├── repository/
│   │   │       │   │   └── GitHubRepository.kt
│   │   │       │   ├── model/
│   │   │       │   │   └── DayEntry.kt
│   │   │       │   ├── ui/
│   │   │       │   │   ├── screens/
│   │   │       │   │   │   ├── DayListScreen.kt
│   │   │       │   │   │   ├── EditDayScreen.kt
│   │   │       │   │   │   └── ResultScreen.kt
│   │   │       │   │   ├── composables/
│   │   │       │   │   │   └── PhotoPicker.kt
│   │   │       │   │   └── theme/
│   │   │       │   │       └── Theme.kt
│   │   │       │   └── MainActivity.kt
│   │   │       ├── res/
│   │   │       └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   └── local.properties              # Contains GITHUB_TOKEN (gitignored)
│
├── website/                          # Astro website (existing)
│   ├── src/                          # Astro source files
│   ├── content/
│   │   └── days/                     # Day entries edited by the app
│   │       ├── day-01-alamparai-pondicherry/
│   │       │   ├── index.md          # Frontmatter + content + photo captions
│   │       │   ├── route.gpx
│   │       │   └── photos/
│   │       │       ├── photo-1.jpg
│   │       │       └── photo-2.jpg
│   │       └── day-02-laxmivillas/
│   │           └── ...
│   ├── package.json
│   └── astro.config.mjs
│
├── .github/
│   └── workflows/
│       └── build-android.yml         # APK build workflow
│
└── docs/
    └── android-app-brief.md          # This document
```

## Step 12: Token Setup

### 12.1 Create GitHub Personal Access Token

1. Go to GitHub Settings > Developer settings > Personal access tokens > Fine-grained tokens
2. Create new token with:
   - Repository access: `alnorth/india-2026`
   - Permissions:
     - Contents: Read and write
     - Pull requests: Read and write
     - Metadata: Read-only
3. Copy the token

### 12.2 Configure Local Development

Add the token to `android/local.properties`:

```properties
GITHUB_TOKEN=ghp_your_token_here
```

This file is gitignored and won't be committed.

### 12.3 Configure GitHub Actions

Add the token as a repository secret named `APP_GITHUB_TOKEN` (see Step 10.3).

The token is injected into the build at compile time and hardcoded into the APK - this is acceptable since it's a private app for personal use only.

## Summary

This Android app provides a streamlined way to update the India 2026 cycle tour website directly from a phone. Key features:

1. **Edit existing days** - Select from pre-configured day entries and update status, Strava ID, and content
2. **Photo picker with captions** - Select photos from gallery, add captions, auto-compress for web
3. **Monorepo structure** - App lives alongside website in same repository (`android/` and `website/`)
4. **GitHub integration** - Automatic branch creation and PR submission to `master`
5. **Preview links** - Direct access to PR and Amplify preview URLs
6. **Automated builds** - GitHub Actions compiles APK on changes to `android/`

The app is designed for single-user private use, with minimal overhead and maximum convenience for updating the site while traveling. Day entries (title, date, distance, location) are pre-configured in the repository - the app only edits existing days, it does not create new ones.

Photo captions are stored in the frontmatter of each day's `index.md` file, allowing the website to display them alongside images.
