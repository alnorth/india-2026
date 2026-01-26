package com.alnorth.india2026.api

import com.google.gson.annotations.SerializedName

// For fetching existing content from GitHub
data class GitHubContent(
    @SerializedName("name") val name: String,
    @SerializedName("path") val path: String,
    @SerializedName("sha") val sha: String,
    @SerializedName("type") val type: String,  // "file" or "dir"
    @SerializedName("content") val content: String?,  // Base64 encoded (only for files)
    @SerializedName("encoding") val encoding: String?
)

// For updating existing files (requires sha) or creating new ones
data class UpdateFileRequest(
    @SerializedName("message") val message: String,
    @SerializedName("content") val content: String,  // Base64 encoded
    @SerializedName("branch") val branch: String,
    @SerializedName("sha") val sha: String? = null  // Required when updating existing file
)

data class FileResponse(
    @SerializedName("content") val content: FileContent,
    @SerializedName("commit") val commit: CommitInfo
)

data class FileContent(
    @SerializedName("path") val path: String,
    @SerializedName("sha") val sha: String
)

data class CommitInfo(
    @SerializedName("sha") val sha: String
)

// For fetching GitHub releases
data class GitHubRelease(
    @SerializedName("id") val id: Long,
    @SerializedName("tag_name") val tag_name: String,
    @SerializedName("name") val name: String,
    @SerializedName("html_url") val html_url: String,
    @SerializedName("body") val body: String?,
    @SerializedName("prerelease") val prerelease: Boolean,
    @SerializedName("created_at") val created_at: String,
    @SerializedName("published_at") val published_at: String?,
    @SerializedName("assets") val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browser_download_url: String,
    @SerializedName("size") val size: Long
)
