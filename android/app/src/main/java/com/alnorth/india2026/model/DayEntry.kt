package com.alnorth.india2026.model

import android.net.Uri

data class PhotoWithCaption(
    val filename: String,
    val caption: String
)

data class DayEntry(
    val slug: String,           // Directory name (e.g., "day-01-alamparai-pondicherry")
    val fileSha: String,        // Git SHA of index.md (required for updates)
    val date: String,           // YYYY-MM-DD (read-only, from existing file)
    val title: String,          // Read-only, from existing file
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
        appendLine("location: \"$location\"")
        appendLine("status: $status")
        stravaId?.let { if (it.isNotEmpty()) appendLine("stravaId: \"$it\"") }

        // Combine existing photos with new photos
        val allPhotos = photos + newPhotos
        if (allPhotos.isNotEmpty()) {
            appendLine("photos:")
            allPhotos.forEach { photo ->
                appendLine("  - file: \"${photo.filename}\"")
                appendLine("    caption: \"${photo.caption}\"")
            }
        }

        appendLine("---")
        appendLine()
        appendLine(content)
    }
}

// Summary for day list display
data class DaySummary(
    val slug: String,
    val title: String,
    val date: String,
    val status: String,
    val location: String
)

data class SubmissionResult(
    val prNumber: Int,
    val prUrl: String,
    val branchName: String
)

// Represents a photo selected from gallery with its caption
data class SelectedPhoto(
    val uri: Uri,
    val caption: String = ""
)
