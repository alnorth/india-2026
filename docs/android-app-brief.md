# Android App Product Brief: India 2026 Tour Updater

## Overview

A private Android app for updating the India 2026 cycle tour website while on the road. The app allows the rider to create daily posts with photos, GPS routes, and written content directly from their phone. Updates are submitted via GitHub Pull Requests, with Amplify preview links for verification before merging.

## Problem Statement

While cycling through India, the rider needs a simple way to update the tour website from their phone without:
- Needing a laptop or desktop computer
- Understanding Git commands or GitHub's web interface
- Manually formatting markdown files
- Dealing with complex file upload processes

## Target User

Single user (the cyclist) - this is a private app, not for public distribution.

## Core Features

### 1. Day Entry Creation
- Create new day entries with:
  - Title (e.g., "Day 5: Chennai to Mahabalipuram")
  - Date picker
  - Distance (km)
  - Location
  - Status (planned/in-progress/completed)
  - Strava activity ID (optional)
  - Written content (markdown supported)

### 2. Photo Selection & Upload
- Pick multiple photos from device gallery
- Preview selected photos before upload
- Automatic image compression for web
- Photos organized in day's `photos/` directory

### 3. GPX File Attachment
- Select GPX file from device storage
- Preview route on map before upload
- Renamed to `route.gpx` automatically

### 4. GitHub Integration
- Authenticate with GitHub (Personal Access Token)
- Create feature branch automatically
- Commit all files (markdown, photos, GPX)
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
│  Create New Day │
│    or Edit      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Fill Details   │
│  - Title        │
│  - Date         │
│  - Distance     │
│  - Location     │
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
│  Add GPX Route  │
│   (optional)    │
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

### GitHub API Integration
- Repository: `alnorth/india-2026`
- Base branch: `main`
- Feature branch naming: `app/day-XX-YYYY-MM-DD`
- Required scopes: `repo` (for private repo access)
- **Token**: Hardcoded in app (single-user private app)

### Build & Distribution
- APK built via GitHub Actions
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
        @Body request: CreateFileRequest
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

data class CreateFileRequest(
    val message: String,
    val content: String,  // Base64 encoded
    val branch: String
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
    private val baseBranch = "main"

    companion object {
        // Token accessed directly from BuildConfig (hardcoded at build time)
        val token: String = BuildConfig.GITHUB_TOKEN
    }

    suspend fun createDayEntry(
        dayEntry: DayEntry,
        photos: List<Uri>,
        gpxUri: Uri?,
        context: Context
    ): Result<SubmissionResult> = runCatching {

        // 1. Get latest commit SHA from main
        val mainBranch = api.getBranch(owner, repo, baseBranch)
        val baseSha = mainBranch.commit.sha

        // 2. Create feature branch
        val branchName = "app/${dayEntry.slug}-${System.currentTimeMillis()}"
        api.createBranch(
            owner, repo,
            CreateBranchRequest(
                ref = "refs/heads/$branchName",
                sha = baseSha
            )
        )

        // 3. Create markdown file
        val markdownContent = dayEntry.toMarkdown()
        val markdownPath = "content/days/${dayEntry.slug}/index.md"
        api.createOrUpdateFile(
            owner, repo, markdownPath,
            CreateFileRequest(
                message = "Add ${dayEntry.title}",
                content = Base64.encodeToString(
                    markdownContent.toByteArray(),
                    Base64.NO_WRAP
                ),
                branch = branchName
            )
        )

        // 4. Upload photos
        photos.forEachIndexed { index, uri ->
            val photoBytes = compressImage(context, uri)
            val photoPath = "content/days/${dayEntry.slug}/photos/photo-${index + 1}.jpg"
            api.createOrUpdateFile(
                owner, repo, photoPath,
                CreateFileRequest(
                    message = "Add photo ${index + 1}",
                    content = Base64.encodeToString(photoBytes, Base64.NO_WRAP),
                    branch = branchName
                )
            )
        }

        // 5. Upload GPX if provided
        gpxUri?.let { uri ->
            val gpxBytes = context.contentResolver.openInputStream(uri)?.readBytes()
            if (gpxBytes != null) {
                val gpxPath = "content/days/${dayEntry.slug}/route.gpx"
                api.createOrUpdateFile(
                    owner, repo, gpxPath,
                    CreateFileRequest(
                        message = "Add GPX route",
                        content = Base64.encodeToString(gpxBytes, Base64.NO_WRAP),
                        branch = branchName
                    )
                )
            }
        }

        // 6. Create Pull Request
        val pr = api.createPullRequest(
            owner, repo,
            CreatePullRequestRequest(
                title = "Add ${dayEntry.title}",
                body = buildPrBody(dayEntry, photos.size, gpxUri != null),
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

    suspend fun getAmplifyPreviewUrl(prNumber: Int): String? {
        // Poll PR comments for Amplify bot comment with preview URL
        val comments = api.getPullRequestComments(owner, repo, prNumber)
        val amplifyComment = comments.find {
            it.user.login == "aws-amplify-us-east-1" ||
            it.body.contains("amplifyapp.com")
        }

        // Extract URL from comment body
        return amplifyComment?.body?.let { body ->
            val regex = Regex("""https://[a-z0-9]+\.amplifyapp\.com[^\s\)]*""")
            regex.find(body)?.value
        }
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Resize if too large (max 1920px on longest side)
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

    private fun buildPrBody(entry: DayEntry, photoCount: Int, hasGpx: Boolean): String {
        return buildString {
            appendLine("## ${entry.title}")
            appendLine()
            appendLine("**Date:** ${entry.date}")
            appendLine("**Distance:** ${entry.distance} km")
            appendLine("**Location:** ${entry.location}")
            appendLine("**Status:** ${entry.status}")
            appendLine()
            appendLine("### Content")
            appendLine(entry.content.take(500))
            if (entry.content.length > 500) appendLine("...")
            appendLine()
            appendLine("### Attachments")
            appendLine("- Photos: $photoCount")
            appendLine("- GPX Route: ${if (hasGpx) "Yes" else "No"}")
            appendLine()
            appendLine("---")
            appendLine("*Submitted via India 2026 Android App*")
        }
    }
}

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

data class DayEntry(
    val date: String,           // YYYY-MM-DD
    val title: String,
    val distance: Int,
    val location: String,
    val status: String,         // planned, in-progress, completed
    val stravaId: String?,
    val content: String
) {
    val slug: String
        get() {
            // Generate slug from title: "Day 5: Chennai to Mahabalipuram" -> "day-05-chennai-to-mahabalipuram"
            val dayNumber = Regex("""Day (\d+)""").find(title)?.groupValues?.get(1)?.padStart(2, '0') ?: "00"
            val locationPart = title.substringAfter(":").trim()
                .lowercase()
                .replace(Regex("[^a-z0-9\\s]"), "")
                .replace(Regex("\\s+"), "-")
                .take(40)
            return "day-$dayNumber-$locationPart"
        }

    fun toMarkdown(): String = buildString {
        appendLine("---")
        appendLine("date: $date")
        appendLine("title: \"$title\"")
        appendLine("distance: $distance")
        appendLine("location: \"$location\"")
        appendLine("status: $status")
        stravaId?.let { appendLine("stravaId: \"$it\"") }
        appendLine("---")
        appendLine()
        appendLine(content)
    }
}
```

## Step 5: Photo Picker Implementation

```kotlin
// app/src/main/java/com/alnorth/india2026/ui/composables/PhotoPicker.kt

@Composable
fun PhotoPickerSection(
    selectedPhotos: List<Uri>,
    onPhotosSelected: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Photo picker launcher (Android 13+ uses new photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        onPhotosSelected(selectedPhotos + uris)
    }

    // Fallback for older Android versions
    val legacyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        onPhotosSelected(selectedPhotos + uris)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Photos (${selectedPhotos.size})",
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(selectedPhotos) { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        IconButton(
                            onClick = {
                                onPhotosSelected(selectedPhotos - uri)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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
```

## Step 6: GPX File Picker

```kotlin
// app/src/main/java/com/alnorth/india2026/ui/composables/GpxPicker.kt

@Composable
fun GpxPickerSection(
    selectedGpx: Uri?,
    onGpxSelected: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val gpxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        onGpxSelected(uri)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GPX Route",
                style = MaterialTheme.typography.titleMedium
            )

            if (selectedGpx == null) {
                Button(
                    onClick = {
                        gpxPickerLauncher.launch("application/gpx+xml")
                    }
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add GPX")
                }
            } else {
                TextButton(
                    onClick = { onGpxSelected(null) }
                ) {
                    Text("Remove")
                }
            }
        }

        if (selectedGpx != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = getFileName(context, selectedGpx) ?: "route.gpx",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    }
}
```

## Step 7: Main Entry Form Screen

```kotlin
// app/src/main/java/com/alnorth/india2026/ui/screens/CreateEntryScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEntryScreen(
    viewModel: CreateEntryViewModel = viewModel(),
    onNavigateToResult: (SubmissionResult) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Day Entry") }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is CreateEntryUiState.Form -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::updateTitle,
                        label = { Text("Title") },
                        placeholder = { Text("Day 1: Kanyakumari to Nagercoil") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date picker
                    DatePickerField(
                        selectedDate = state.date,
                        onDateSelected = viewModel::updateDate
                    )

                    // Distance
                    OutlinedTextField(
                        value = state.distance,
                        onValueChange = viewModel::updateDistance,
                        label = { Text("Distance (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Location
                    OutlinedTextField(
                        value = state.location,
                        onValueChange = viewModel::updateLocation,
                        label = { Text("Location") },
                        placeholder = { Text("Tamil Nadu, India") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Status dropdown
                    StatusDropdown(
                        selectedStatus = state.status,
                        onStatusSelected = viewModel::updateStatus
                    )

                    // Strava ID (optional)
                    OutlinedTextField(
                        value = state.stravaId,
                        onValueChange = viewModel::updateStravaId,
                        label = { Text("Strava Activity ID (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Content
                    OutlinedTextField(
                        value = state.content,
                        onValueChange = viewModel::updateContent,
                        label = { Text("Content (Markdown)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 10
                    )

                    Divider()

                    // Photo picker
                    PhotoPickerSection(
                        selectedPhotos = state.photos,
                        onPhotosSelected = viewModel::updatePhotos
                    )

                    Divider()

                    // GPX picker
                    GpxPickerSection(
                        selectedGpx = state.gpxUri,
                        onGpxSelected = viewModel::updateGpx
                    )

                    Spacer(Modifier.height(16.dp))

                    // Submit button
                    Button(
                        onClick = { viewModel.submit(context) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isValid
                    ) {
                        Text("Create Pull Request")
                    }
                }
            }

            is CreateEntryUiState.Submitting -> {
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

            is CreateEntryUiState.Success -> {
                LaunchedEffect(state.result) {
                    onNavigateToResult(state.result)
                }
            }

            is CreateEntryUiState.Error -> {
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
                        Button(onClick = viewModel::retry) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
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
├── android/                          # Android app source
│   ├── app/
│   │   ├── src/
│   │   │   └── main/
│   │   │       ├── java/com/alnorth/india2026/
│   │   │       │   ├── api/
│   │   │       │   │   ├── GitHubApi.kt
│   │   │       │   │   └── Models.kt
│   │   │       │   ├── repository/
│   │   │       │   │   └── GitHubRepository.kt
│   │   │       │   ├── model/
│   │   │       │   │   └── DayEntry.kt
│   │   │       │   ├── ui/
│   │   │       │   │   ├── screens/
│   │   │       │   │   │   ├── CreateEntryScreen.kt
│   │   │       │   │   │   └── ResultScreen.kt
│   │   │       │   │   ├── composables/
│   │   │       │   │   │   ├── PhotoPicker.kt
│   │   │       │   │   │   ├── GpxPicker.kt
│   │   │       │   │   │   └── DatePickerField.kt
│   │   │       │   │   └── theme/
│   │   │       │   │       └── Theme.kt
│   │   │       │   ├── viewmodel/
│   │   │       │   │   ├── CreateEntryViewModel.kt
│   │   │       │   │   └── ResultViewModel.kt
│   │   │       │   └── MainActivity.kt
│   │   │       ├── res/
│   │   │       └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradle.properties
├── .github/
│   └── workflows/
│       └── build-android.yml
├── content/                          # Website content (existing)
├── src/                              # Astro site (existing)
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

1. **Simple form-based entry** - No need to understand Git or markdown formatting
2. **Native photo picker** - Select multiple photos from gallery with automatic compression
3. **GPX support** - Attach route files from cycling apps
4. **GitHub integration** - Automatic branch creation and PR submission
5. **Preview links** - Direct access to PR and Amplify preview URLs
6. **Automated builds** - GitHub Actions compiles APK on every push

The app is designed for single-user private use, with minimal overhead and maximum convenience for updating the site while traveling.
