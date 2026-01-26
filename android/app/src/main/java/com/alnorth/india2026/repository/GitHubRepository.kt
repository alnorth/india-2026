package com.alnorth.india2026.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.alnorth.india2026.api.*
import com.alnorth.india2026.model.*
import com.alnorth.india2026.util.RetryConfig
import com.alnorth.india2026.util.RetryResult
import com.alnorth.india2026.util.withRetry

class GitHubRepository(
    private val api: GitHubApi
) {
    private val owner = "alnorth"
    private val repo = "india-2026"
    private val baseBranch = "master"

    // Natural sort comparator for photo filenames (photo-2.jpg before photo-10.jpg)
    private val naturalSortComparator = Comparator<GitHubContent> { a, b ->
        naturalCompare(a.name, b.name)
    }

    private fun naturalCompare(a: String, b: String): Int {
        val pattern = Regex("""(\d+)|(\D+)""")
        val partsA = pattern.findAll(a).map { it.value }.toList()
        val partsB = pattern.findAll(b).map { it.value }.toList()

        for (i in 0 until minOf(partsA.size, partsB.size)) {
            val partA = partsA[i]
            val partB = partsB[i]
            val numA = partA.toIntOrNull()
            val numB = partB.toIntOrNull()

            val cmp = when {
                numA != null && numB != null -> numA.compareTo(numB)
                else -> partA.compareTo(partB)
            }
            if (cmp != 0) return cmp
        }
        return partsA.size.compareTo(partsB.size)
    }

    // Fetch all existing days from the repository
    suspend fun getAllDays(): Result<List<DaySummary>> = runCatching {
        val contents = api.getDirectoryContents(owner, repo, "website/content/days")
        contents
            .filter { it.type == "dir" }
            .mapNotNull { dir ->
                try {
                    // Fetch the index.md for each day to get metadata
                    val indexContent = api.getFileContent(owner, repo, "${dir.path}/index.md")
                    val markdown = String(Base64.decode(indexContent.content, Base64.DEFAULT))
                    parseDaySummary(dir.name, markdown, indexContent.sha)
                } catch (e: Exception) {
                    // Skip days that can't be parsed
                    null
                }
            }
            .sortedBy { it.date }
    }

    // Fetch a specific day's full content
    suspend fun getDayBySlug(slug: String, branchName: String? = null): Result<DayEntry> = runCatching {
        val indexContent = if (branchName != null) {
            api.getFileContent(owner, repo, "website/content/days/$slug/index.md", branchName)
        } else {
            api.getFileContent(owner, repo, "website/content/days/$slug/index.md")
        }
        val markdown = String(Base64.decode(indexContent.content, Base64.DEFAULT))

        // Get actual photos from the branch's directory listing
        val photos = getPhotosFromDirectory(slug, branchName)

        parseDayEntry(slug, markdown, indexContent.sha, photos)
    }

    // Update an existing day entry
    suspend fun updateDayEntry(
        dayEntry: DayEntry,
        newPhotos: List<SelectedPhoto>,
        context: Context,
        onProgress: (progress: UploadProgress) -> Unit = { _ -> },
        startFromIndex: Int = 0
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

        // Add already-uploaded photos if resuming
        for (i in 0 until startFromIndex) {
            val photoNum = existingPhotoCount + i + 1
            val filename = "photo-$photoNum.jpg"
            uploadedPhotos.add(PhotoWithCaption(filename, newPhotos[i].caption))
        }

        newPhotos.forEachIndexed { index, selectedPhoto ->
            // Skip already uploaded photos when resuming
            if (index < startFromIndex) return@forEachIndexed

            val photoBytes = readOriginalImage(context, selectedPhoto.uri)
            val photoNum = existingPhotoCount + index + 1
            val filename = "photo-$photoNum.jpg"
            val photoPath = "website/content/days/${dayEntry.slug}/photos/$filename"

            val retryConfig = RetryConfig(maxAttempts = 4, initialDelayMs = 2000)
            val result = withRetry(
                config = retryConfig,
                onRetry = { attempt, delayMs, error ->
                    onProgress(UploadProgress(
                        currentPhoto = index + 1,
                        totalPhotos = newPhotos.size,
                        retryAttempt = attempt,
                        retryDelayMs = delayMs,
                        lastError = error.message
                    ))
                }
            ) {
                api.createOrUpdateFile(
                    owner, repo, photoPath,
                    UpdateFileRequest(
                        message = "Add photo $photoNum",
                        content = Base64.encodeToString(photoBytes, Base64.NO_WRAP),
                        branch = branchName
                    )
                )
            }

            when (result) {
                is RetryResult.Success -> {
                    uploadedPhotos.add(PhotoWithCaption(filename, selectedPhoto.caption))
                    onProgress(UploadProgress(
                        currentPhoto = index + 1,
                        totalPhotos = newPhotos.size,
                        retryAttempt = null,
                        retryDelayMs = null,
                        lastError = null
                    ))
                }
                is RetryResult.Failure -> {
                    throw PhotoUploadException(
                        message = result.exception.message ?: "Upload failed",
                        failedPhotoIndex = index,
                        uploadedCount = uploadedPhotos.size,
                        totalCount = newPhotos.size,
                        branchName = branchName,
                        cause = result.exception
                    )
                }
            }
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

    // Commit changes to an existing PR branch
    suspend fun commitToExistingBranch(
        branchName: String,
        dayEntry: DayEntry,
        newPhotos: List<SelectedPhoto>,
        context: Context,
        onProgress: (progress: UploadProgress) -> Unit = { _ -> },
        startFromIndex: Int = 0
    ): Result<SubmissionResult> = runCatching {

        // 1. Get the PR number for this branch
        val allPRs = api.getPullRequests(owner, repo, state = "open")
        val pr = allPRs.find { it.head.ref == branchName }
            ?: throw IllegalArgumentException("No open PR found for branch $branchName")

        // 2. Upload new photos first (to get filenames)
        val existingPhotoCount = getExistingPhotoCount(dayEntry.slug, branchName)
        val uploadedPhotos = mutableListOf<PhotoWithCaption>()

        // Add already-uploaded photos if resuming
        for (i in 0 until startFromIndex) {
            val photoNum = existingPhotoCount + i + 1
            val filename = "photo-$photoNum.jpg"
            uploadedPhotos.add(PhotoWithCaption(filename, newPhotos[i].caption))
        }

        newPhotos.forEachIndexed { index, selectedPhoto ->
            // Skip already uploaded photos when resuming
            if (index < startFromIndex) return@forEachIndexed

            val photoBytes = readOriginalImage(context, selectedPhoto.uri)
            val photoNum = existingPhotoCount + index + 1
            val filename = "photo-$photoNum.jpg"
            val photoPath = "website/content/days/${dayEntry.slug}/photos/$filename"

            val retryConfig = RetryConfig(maxAttempts = 4, initialDelayMs = 2000)
            val result = withRetry(
                config = retryConfig,
                onRetry = { attempt, delayMs, error ->
                    onProgress(UploadProgress(
                        currentPhoto = index + 1,
                        totalPhotos = newPhotos.size,
                        retryAttempt = attempt,
                        retryDelayMs = delayMs,
                        lastError = error.message
                    ))
                }
            ) {
                api.createOrUpdateFile(
                    owner, repo, photoPath,
                    UpdateFileRequest(
                        message = "Add photo $photoNum",
                        content = Base64.encodeToString(photoBytes, Base64.NO_WRAP),
                        branch = branchName
                    )
                )
            }

            when (result) {
                is RetryResult.Success -> {
                    uploadedPhotos.add(PhotoWithCaption(filename, selectedPhoto.caption))
                    onProgress(UploadProgress(
                        currentPhoto = index + 1,
                        totalPhotos = newPhotos.size,
                        retryAttempt = null,
                        retryDelayMs = null,
                        lastError = null
                    ))
                }
                is RetryResult.Failure -> {
                    throw PhotoUploadException(
                        message = result.exception.message ?: "Upload failed",
                        failedPhotoIndex = index,
                        uploadedCount = uploadedPhotos.size,
                        totalCount = newPhotos.size,
                        branchName = branchName,
                        cause = result.exception
                    )
                }
            }
        }

        // 3. Update markdown file with photo captions
        val markdownContent = dayEntry.toMarkdown(uploadedPhotos)
        val markdownPath = "website/content/days/${dayEntry.slug}/index.md"

        // Get the current file SHA from the branch
        val currentFile = api.getFileContent(owner, repo, markdownPath, branchName)

        api.createOrUpdateFile(
            owner, repo, markdownPath,
            UpdateFileRequest(
                message = "Update ${dayEntry.title}",
                content = Base64.encodeToString(
                    markdownContent.toByteArray(),
                    Base64.NO_WRAP
                ),
                branch = branchName,
                sha = currentFile.sha  // Use SHA from the branch, not master
            )
        )

        SubmissionResult(
            prNumber = pr.number,
            prUrl = pr.html_url,
            branchName = branchName
        )
    }

    private suspend fun getExistingPhotoCount(slug: String, branchName: String? = null): Int {
        return try {
            val contents = if (branchName != null) {
                api.getDirectoryContents(owner, repo, "website/content/days/$slug/photos", branchName)
            } else {
                api.getDirectoryContents(owner, repo, "website/content/days/$slug/photos")
            }
            contents.count { it.type == "file" && it.name.endsWith(".jpg") }
        } catch (e: Exception) {
            0  // No photos directory yet
        }
    }

    // Get photos from directory listing on a specific branch
    private suspend fun getPhotosFromDirectory(slug: String, branchName: String?): List<PhotoWithCaption> {
        return try {
            // Get actual files from the photos directory
            val photoFiles = if (branchName != null) {
                api.getDirectoryContents(owner, repo, "website/content/days/$slug/photos", branchName)
            } else {
                api.getDirectoryContents(owner, repo, "website/content/days/$slug/photos")
            }

            // Filter for image files and sort naturally (photo-2 before photo-10)
            photoFiles
                .filter { it.type == "file" && (it.name.endsWith(".jpg") || it.name.endsWith(".jpeg") || it.name.endsWith(".png")) }
                .sortedWith(naturalSortComparator)
                .map { PhotoWithCaption(filename = it.name, caption = "") }
        } catch (e: Exception) {
            emptyList()  // No photos directory yet
        }
    }

    suspend fun getAmplifyPreviewUrl(prNumber: Int): String? {
        return try {
            // Use issue comments endpoint - Amplify bot posts to general comments, not review comments
            val comments = api.getIssueComments(owner, repo, prNumber)
            val amplifyComment = comments.find {
                it.user.login.startsWith("aws-amplify-") ||
                it.body.contains("amplifyapp.com")
            }
            amplifyComment?.body?.let { body ->
                // Match URLs like https://pr-24.did5czmmf06mc.amplifyapp.com
                val regex = Regex("""https://[a-z0-9.-]+\.amplifyapp\.com[^\s\)]*""")
                regex.find(body)?.value
            }
        } catch (e: Exception) {
            null
        }
    }

    // Fetch open pull requests created by the app
    suspend fun getAppCreatedPullRequests(): Result<List<com.alnorth.india2026.api.PullRequest>> = runCatching {
        val allPRs = api.getPullRequests(owner, repo, state = "open")
        // Filter PRs created by the app - branches start with "app/"
        allPRs.filter { pr ->
            pr.head.ref.startsWith("app/")
        }
    }

    // Check if a newer master build is available
    suspend fun checkForUpdate(currentCommitSha: String): Result<UpdateInfo?> = runCatching {
        val releases = api.getReleases(owner, repo, perPage = 10)

        // Find the latest master build release (not prerelease)
        val latestMasterRelease = releases
            .filter { !it.prerelease && it.tag_name.startsWith("master-build-") }
            .maxByOrNull { it.created_at }

        if (latestMasterRelease == null) {
            return@runCatching null
        }

        // Extract commit SHA from release body
        val commitShaPattern = Regex("""Commit:\s*([a-f0-9]{40})""")
        val matchResult = commitShaPattern.find(latestMasterRelease.body ?: "")
        val releaseCommitSha = matchResult?.groupValues?.get(1)

        if (releaseCommitSha == null) {
            return@runCatching null
        }

        // Compare commit SHAs (compare full SHAs or first 7 chars)
        val currentShort = currentCommitSha.take(7)
        val releaseShort = releaseCommitSha.take(7)

        if (currentShort != releaseShort && releaseCommitSha != currentCommitSha) {
            UpdateInfo(
                newVersion = latestMasterRelease.name,
                commitSha = releaseShort,
                downloadUrl = latestMasterRelease.html_url,
                releaseNotesUrl = latestMasterRelease.html_url
            )
        } else {
            null
        }
    }

    private fun readOriginalImage(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open image URI")
        return inputStream.use { it.readBytes() }
    }

    private fun parseDaySummary(slug: String, markdown: String, sha: String): DaySummary {
        // Parse frontmatter to extract summary info
        val frontmatter = extractFrontmatter(markdown)
        return DaySummary(
            slug = slug,
            title = frontmatter["title"] ?: slug,
            date = frontmatter["date"] ?: "",
            status = frontmatter["status"] ?: "planned",
            location = frontmatter["location"] ?: ""
        )
    }

    private fun parseDayEntry(slug: String, markdown: String, sha: String, directoryPhotos: List<PhotoWithCaption>): DayEntry {
        val frontmatter = extractFrontmatter(markdown)
        val photoCaptions = parsePhotoCaptions(markdown)
        val content = markdown.substringAfter("---").substringAfter("---").trim()

        // Merge directory photos with captions from markdown
        val photos = directoryPhotos.map { photo ->
            val caption = photoCaptions[photo.filename] ?: ""
            photo.copy(caption = caption)
        }

        return DayEntry(
            slug = slug,
            fileSha = sha,
            date = frontmatter["date"] ?: "",
            title = frontmatter["title"] ?: slug,
            location = frontmatter["location"] ?: "",
            status = frontmatter["status"] ?: "planned",
            stravaId = frontmatter["stravaId"]?.ifEmpty { null },
            coordinates = frontmatter["coordinates"]?.ifEmpty { null },
            content = content,
            photos = photos
        )
    }

    private fun extractFrontmatter(markdown: String): Map<String, String> {
        if (!markdown.startsWith("---")) return emptyMap()

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

    // Parse photo captions from markdown frontmatter
    private fun parsePhotoCaptions(markdown: String): Map<String, String> {
        if (!markdown.startsWith("---")) return emptyMap()

        val frontmatterSection = markdown
            .substringAfter("---")
            .substringBefore("---")

        // Simple YAML list parser for photo captions
        val captions = mutableMapOf<String, String>()
        var currentFilename: String? = null

        val inPhotosSection = frontmatterSection.contains("photos:")
        if (!inPhotosSection) return emptyMap()

        val lines = frontmatterSection.lines()
        var inPhotos = false

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed == "photos:" -> inPhotos = true
                inPhotos && trimmed.startsWith("- file:") -> {
                    currentFilename = trimmed
                        .substringAfter("file:")
                        .trim()
                        .removeSurrounding("\"")
                }
                inPhotos && trimmed.startsWith("caption:") && currentFilename != null -> {
                    val caption = trimmed
                        .substringAfter("caption:")
                        .trim()
                        .removeSurrounding("\"")
                    captions[currentFilename!!] = caption
                    currentFilename = null
                }
                inPhotos && !trimmed.startsWith("-") && !trimmed.startsWith("file:") &&
                !trimmed.startsWith("caption:") && trimmed.isNotEmpty() && !trimmed.startsWith(" ") -> {
                    // We've left the photos section
                    break
                }
            }
        }
        return captions
    }

    private fun buildPrBody(entry: DayEntry, newPhotoCount: Int): String {
        return buildString {
            appendLine("## ${entry.title}")
            appendLine()
            appendLine("**Date:** ${entry.date}")
            appendLine("**Location:** ${entry.location}")
            appendLine("**Status:** ${entry.status}")
            appendLine()
            if (entry.content.isNotEmpty()) {
                appendLine("### Content Preview")
                appendLine(entry.content.take(500))
                if (entry.content.length > 500) appendLine("...")
                appendLine()
            }
            if (newPhotoCount > 0) {
                appendLine("### New Photos Added: $newPhotoCount")
                appendLine()
            }
            appendLine("---")
            appendLine("*Submitted via India 2026 Android App*")
        }
    }
}

data class UpdateInfo(
    val newVersion: String,
    val commitSha: String,
    val downloadUrl: String,
    val releaseNotesUrl: String
)

/**
 * Progress information for photo uploads, including retry status
 */
data class UploadProgress(
    val currentPhoto: Int,
    val totalPhotos: Int,
    val retryAttempt: Int? = null,
    val retryDelayMs: Long? = null,
    val lastError: String? = null
) {
    val isRetrying: Boolean get() = retryAttempt != null
}

/**
 * Exception thrown when photo upload fails after all retries
 * Contains information needed to resume the upload
 */
class PhotoUploadException(
    message: String,
    val failedPhotoIndex: Int,
    val uploadedCount: Int,
    val totalCount: Int,
    val branchName: String,
    cause: Throwable? = null
) : Exception(message, cause)
