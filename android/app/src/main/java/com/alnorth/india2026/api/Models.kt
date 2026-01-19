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

data class BranchResponse(
    @SerializedName("name") val name: String,
    @SerializedName("commit") val commit: CommitRef
)

data class CommitRef(
    @SerializedName("sha") val sha: String
)

data class CreateBranchRequest(
    @SerializedName("ref") val ref: String,  // "refs/heads/branch-name"
    @SerializedName("sha") val sha: String   // SHA of commit to branch from
)

data class RefResponse(
    @SerializedName("ref") val ref: String,
    @SerializedName("url") val url: String
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

data class CreatePullRequestRequest(
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("head") val head: String,  // Branch name
    @SerializedName("base") val base: String   // Target branch (master)
)

data class PullRequestResponse(
    @SerializedName("number") val number: Int,
    @SerializedName("html_url") val html_url: String,
    @SerializedName("head") val head: PullRequestHead
)

data class PullRequestHead(
    @SerializedName("ref") val ref: String
)

data class PullRequestComment(
    @SerializedName("body") val body: String,
    @SerializedName("user") val user: GitHubUser
)

data class GitHubUser(
    @SerializedName("login") val login: String
)

// For listing pull requests
data class PullRequest(
    @SerializedName("number") val number: Int,
    @SerializedName("title") val title: String,
    @SerializedName("html_url") val html_url: String,
    @SerializedName("state") val state: String,
    @SerializedName("created_at") val created_at: String,
    @SerializedName("updated_at") val updated_at: String,
    @SerializedName("head") val head: PullRequestHead,
    @SerializedName("user") val user: GitHubUser
)
