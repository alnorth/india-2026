package com.alnorth.india2026.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.alnorth.india2026.api.*
import com.alnorth.india2026.model.*
import java.io.ByteArrayOutputStream

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
            val filename = "photo-$photoNum.jpg"
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
        return try {
            val comments = api.getPullRequestComments(owner, repo, prNumber)
            val amplifyComment = comments.find {
                it.user.login == "aws-amplify-us-east-1" ||
                it.body.contains("amplifyapp.com")
            }
            amplifyComment?.body?.let { body ->
                val regex = Regex("""https://[a-z0-9-]+\.amplifyapp\.com[^\s\)]*""")
                regex.find(body)?.value
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open image URI")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

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

    private fun parsePhotos(markdown: String): List<PhotoWithCaption> {
        if (!markdown.startsWith("---")) return emptyList()

        val frontmatterSection = markdown
            .substringAfter("---")
            .substringBefore("---")

        // Simple YAML list parser for photos
        val photos = mutableListOf<PhotoWithCaption>()
        var currentFilename: String? = null

        val inPhotosSection = frontmatterSection.contains("photos:")
        if (!inPhotosSection) return emptyList()

        val lines = frontmatterSection.lines()
        var inPhotos = false

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed == "photos:" -> inPhotos = true
                inPhotos && trimmed.startsWith("- filename:") -> {
                    currentFilename = trimmed
                        .substringAfter("filename:")
                        .trim()
                        .removeSurrounding("\"")
                }
                inPhotos && trimmed.startsWith("caption:") && currentFilename != null -> {
                    val caption = trimmed
                        .substringAfter("caption:")
                        .trim()
                        .removeSurrounding("\"")
                    photos.add(PhotoWithCaption(currentFilename!!, caption))
                    currentFilename = null
                }
                inPhotos && !trimmed.startsWith("-") && !trimmed.startsWith("filename:") &&
                !trimmed.startsWith("caption:") && trimmed.isNotEmpty() && !trimmed.startsWith(" ") -> {
                    // We've left the photos section
                    break
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
