package com.alnorth.india2026.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
fun DayListScreen(
    viewModel: DayListViewModel = viewModel(),
    onDaySelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDays()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("India 2026 - Select Day") },
                actions = {
                    if (uiState is DayListUiState.Success) {
                        IconButton(onClick = { viewModel.loadDays() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DayListUiState.Loading -> {
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
                        Text("Loading days from GitHub...")
                    }
                }
            }

            is DayListUiState.Success -> {
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
                            DayCard(
                                day = day,
                                onClick = { onDaySelected(day.slug) }
                            )
                        }
                    }
                }
            }

            is DayListUiState.Error -> {
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
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { viewModel.loadDays() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCard(
    day: DaySummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = day.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${day.date} • ${day.distance} km • ${day.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "completed" -> MaterialTheme.colorScheme.primary to "Done"
        "in-progress" -> MaterialTheme.colorScheme.tertiary to "Today"
        else -> MaterialTheme.colorScheme.outline to "Planned"
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

class DayListViewModel : ViewModel() {
    private val repository = ApiClient.repository

    private val _uiState = MutableStateFlow<DayListUiState>(DayListUiState.Loading)
    val uiState: StateFlow<DayListUiState> = _uiState.asStateFlow()

    fun loadDays() {
        viewModelScope.launch {
            _uiState.value = DayListUiState.Loading
            repository.getAllDays()
                .onSuccess { days ->
                    _uiState.value = DayListUiState.Success(days)
                }
                .onFailure { e ->
                    _uiState.value = DayListUiState.Error(
                        e.message ?: "Failed to load days. Check your internet connection."
                    )
                }
        }
    }
}

sealed class DayListUiState {
    object Loading : DayListUiState()
    data class Success(val days: List<DaySummary>) : DayListUiState()
    data class Error(val message: String) : DayListUiState()
}
