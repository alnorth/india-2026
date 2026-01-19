package com.alnorth.india2026.api

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
    val base: String   // Target branch (master)
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

// For listing pull requests
data class PullRequest(
    val number: Int,
    val title: String,
    val html_url: String,
    val state: String,
    val created_at: String,
    val updated_at: String,
    val head: PullRequestHead,
    val user: GitHubUser
)
