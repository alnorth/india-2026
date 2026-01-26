package com.alnorth.india2026.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alnorth.india2026.model.SubmissionResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: SubmissionResult,
    onEditAnother: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changes Saved") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Success icon
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Text(
                "Changes Saved!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                "Your changes to \"${result.dayTitle}\" have been committed to master.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline
            )

            // Commit info card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Commit",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        result.commitSha.take(7),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Edit another day button
            Button(
                onClick = onEditAnother,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Another Day")
            }
        }
    }
}
