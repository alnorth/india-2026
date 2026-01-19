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
import androidx.compose.ui.unit.dp
import com.alnorth.india2026.BuildConfig

@Composable
fun BranchFooter(
    modifier: Modifier = Modifier
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
            .padding(top = 4.dp, bottom = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Built from: $branchName @ $commitSha",
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
