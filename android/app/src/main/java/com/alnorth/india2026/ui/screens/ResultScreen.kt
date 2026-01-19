package com.alnorth.india2026.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alnorth.india2026.api.ApiClient
import com.alnorth.india2026.model.SubmissionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: SubmissionResult,
    viewModel: ResultViewModel = viewModel(),
    onCreateAnother: () -> Unit,
    onEditDay: (slug: String, branchName: String) -> Unit
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

            Text(
                "Your changes have been submitted to GitHub. Review the changes in the preview, then merge the pull request to publish them.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth()
            )

            // PR Link Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Pull Request #${result.prNumber}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.prUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null)
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

                    if (previewUrl != null) {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(previewUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
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

            // Edit this day button
            OutlinedButton(
                onClick = {
                    // Extract slug from branch name (e.g., "app/day-1-1234567890" -> "day-1")
                    val slug = result.branchName
                        .removePrefix("app/")
                        .substringBeforeLast("-")
                    onEditDay(slug, result.branchName)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit This Day")
            }

            // Create another button
            Button(
                onClick = onCreateAnother,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Another Day")
            }
        }
    }
}

class ResultViewModel : ViewModel() {
    private val repository = ApiClient.repository

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
