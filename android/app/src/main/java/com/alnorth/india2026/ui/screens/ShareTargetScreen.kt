package com.alnorth.india2026.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alnorth.india2026.api.ApiClient
import com.alnorth.india2026.model.DaySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTargetScreen(
    sharedImageUris: List<Uri>,
    viewModel: ShareTargetViewModel = viewModel(),
    onDaySelected: (slug: String, sharedUris: List<Uri>) -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDays()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Add Photo to Day")
                        Text(
                            text = "${sharedImageUris.size} photo${if (sharedImageUris.size != 1) "s" else ""} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ShareTargetUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading days...")
                    }
                }
            }

            is ShareTargetUiState.Success -> {
                if (state.days.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No days found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.days) { day ->
                            ShareTargetDayCard(
                                day = day,
                                onClick = { onDaySelected(day.slug, sharedImageUris) }
                            )
                        }
                    }
                }
            }

            is ShareTargetUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Error loading days",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onCancel) {
                                Text("Cancel")
                            }
                            Button(onClick = { viewModel.loadDays() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTargetDayCard(
    day: DaySummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = day.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${day.date} - ${day.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

class ShareTargetViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ShareTargetUiState>(ShareTargetUiState.Loading)
    val uiState: StateFlow<ShareTargetUiState> = _uiState.asStateFlow()

    fun loadDays() {
        viewModelScope.launch {
            _uiState.value = ShareTargetUiState.Loading

            try {
                val repository = ApiClient.repository

                repository.getAllDays()
                    .onSuccess { days ->
                        _uiState.value = ShareTargetUiState.Success(days)
                    }
                    .onFailure { e ->
                        _uiState.value = ShareTargetUiState.Error(
                            e.message ?: "Failed to load days"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = ShareTargetUiState.Error(
                    "Error: ${e.message ?: "Unknown error occurred"}"
                )
            }
        }
    }
}

sealed class ShareTargetUiState {
    object Loading : ShareTargetUiState()
    data class Success(val days: List<DaySummary>) : ShareTargetUiState()
    data class Error(val message: String) : ShareTargetUiState()
}
