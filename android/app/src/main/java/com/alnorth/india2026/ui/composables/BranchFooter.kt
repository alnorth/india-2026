package com.alnorth.india2026.ui.composables

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alnorth.india2026.BuildConfig
import com.alnorth.india2026.repository.UpdateInfo

@Composable
fun BranchFooter(
    modifier: Modifier = Modifier,
    updateInfo: UpdateInfo? = null
) {
    val context = LocalContext.current
    val branchName = BuildConfig.BRANCH_NAME
    val actionUrl = BuildConfig.BUILD_ACTION_URL
    val commitSha = BuildConfig.COMMIT_SHA.take(7)

    // Determine color based on branch name
    val textColor = if (branchName == "master" || branchName == "main") {
        Color.Gray.copy(alpha = 0.5f)
    } else {
        Color.Red
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = actionUrl.isNotEmpty()) {
                if (actionUrl.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl))
                    context.startActivity(intent)
                }
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (updateInfo != null) {
            // Show update available with link
            val annotatedText = buildAnnotatedString {
                withStyle(style = SpanStyle(color = textColor)) {
                    append("Built from: $branchName @ $commitSha  •  ")
                }
                withStyle(style = SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold)) {
                    append("Update Available")
                }
            }

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))
                    context.startActivity(intent)
                }
            )
        } else {
            Text(
                text = "Built from: $branchName @ $commitSha",
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
