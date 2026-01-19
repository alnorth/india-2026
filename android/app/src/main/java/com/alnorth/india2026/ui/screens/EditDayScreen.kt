package com.alnorth.india2026.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alnorth.india2026.api.ApiClient
import com.alnorth.india2026.model.DayEntry
import com.alnorth.india2026.model.SelectedPhoto
import com.alnorth.india2026.model.SubmissionResult
import com.alnorth.india2026.ui.composables.ExistingPhotosSection
import com.alnorth.india2026.ui.composables.PhotoPickerSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDayScreen(
    slug: String,
    branchName: String? = null,
    viewModel: EditDayViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToResult: (SubmissionResult) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(slug, branchName) {
        viewModel.loadDay(slug, branchName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Day") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is EditDayUiState.Loading -> {
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
                        Text("Loading day entry...")
                    }
                }
            }

            is EditDayUiState.Editing -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Read-only header info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = state.dayEntry.title,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${state.dayEntry.date} • ${state.dayEntry.location}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Status dropdown (editable)
                    StatusDropdown(
                        selectedStatus = state.status,
                        onStatusSelected = viewModel::updateStatus
                    )

                    // Strava ID (editable)
                    OutlinedTextField(
                        value = state.stravaId,
                        onValueChange = viewModel::updateStravaId,
                        label = { Text("Strava Activity ID") },
                        placeholder = { Text("e.g., 1234567890") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Content (editable)
                    OutlinedTextField(
                        value = state.content,
                        onValueChange = viewModel::updateContent,
                        label = { Text("Content (Markdown)") },
                        placeholder = { Text("Write about your day...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 10
                    )

                    Divider()

                    // Existing photos (from PR branch)
                    ExistingPhotosSection(
                        slug = state.dayEntry.slug,
                        branchName = branchName,
                        existingPhotos = state.editedExistingPhotos,
                        onCaptionChanged = viewModel::updateExistingPhotoCaption
                    )

                    if (state.editedExistingPhotos.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Divider()
                    }

                    // Photo picker
                    PhotoPickerSection(
                        selectedPhotos = state.newPhotos,
                        onPhotosChanged = viewModel::updatePhotos
                    )

                    Spacer(Modifier.height(16.dp))

                    // Submit button
                    Button(
                        onClick = { viewModel.submit(context) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.hasChanges
                    ) {
                        Text(if (branchName != null) "Commit Changes" else "Create Pull Request")
                    }

                    if (!state.hasChanges) {
                        Text(
                            "Make some changes to enable submission",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            is EditDayUiState.Submitting -> {
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
                        Text(state.message)
                    }
                }
            }

            is EditDayUiState.Success -> {
                LaunchedEffect(state.result) {
                    onNavigateToResult(state.result)
                }
            }

            is EditDayUiState.Error -> {
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
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onNavigateBack) {
                                Text("Go Back")
                            }
                            Button(onClick = { viewModel.loadDay(slug, branchName) }) {
                                Text("Try Again")
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
fun StatusDropdown(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("planned", "in-progress", "completed")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedStatus.replaceFirstChar { it.uppercase() }.replace("-", " "),
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercase() }.replace("-", " ")) },
                    onClick = {
                        onStatusSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

class EditDayViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<EditDayUiState>(EditDayUiState.Loading)
    val uiState: StateFlow<EditDayUiState> = _uiState.asStateFlow()

    private var originalEntry: DayEntry? = null
    private var existingBranchName: String? = null

    fun loadDay(slug: String, branchName: String? = null) {
        existingBranchName = branchName
        viewModelScope.launch {
            _uiState.value = EditDayUiState.Loading

            try {
                // Check if token is configured
                if (com.alnorth.india2026.BuildConfig.GITHUB_TOKEN.isEmpty()) {
                    _uiState.value = EditDayUiState.Error(
                        "GitHub token not configured. Please check the app setup."
                    )
                    return@launch
                }

                // Access repository only after token check
                val repository = ApiClient.repository

                repository.getDayBySlug(slug, branchName)
                    .onSuccess { entry ->
                        originalEntry = entry
                        _uiState.value = EditDayUiState.Editing(
                            dayEntry = entry,
                            status = entry.status,
                            stravaId = entry.stravaId ?: "",
                            content = entry.content,
                            newPhotos = emptyList(),
                            editedExistingPhotos = entry.photos,
                            hasChanges = false
                        )
                    }
                    .onFailure { e ->
                        _uiState.value = EditDayUiState.Error(
                            e.message ?: "Failed to load day entry"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = EditDayUiState.Error(
                    "Error: ${e.message ?: "Unknown error occurred"}"
                )
            }
        }
    }

    fun updateStatus(status: String) {
        updateEditingState { it.copy(status = status) }
    }

    fun updateStravaId(stravaId: String) {
        updateEditingState { it.copy(stravaId = stravaId) }
    }

    fun updateContent(content: String) {
        updateEditingState { it.copy(content = content) }
    }

    fun updatePhotos(photos: List<SelectedPhoto>) {
        updateEditingState { it.copy(newPhotos = photos) }
    }

    fun updateExistingPhotoCaption(index: Int, newCaption: String) {
        val current = _uiState.value
        if (current is EditDayUiState.Editing) {
            val updatedPhotos = current.editedExistingPhotos.toMutableList()
            if (index in updatedPhotos.indices) {
                updatedPhotos[index] = updatedPhotos[index].copy(caption = newCaption)
                updateEditingState { it.copy(editedExistingPhotos = updatedPhotos) }
            }
        }
    }

    private fun updateEditingState(update: (EditDayUiState.Editing) -> EditDayUiState.Editing) {
        val current = _uiState.value
        if (current is EditDayUiState.Editing) {
            val updated = update(current)
            val hasChanges = originalEntry?.let { orig ->
                updated.status != orig.status ||
                updated.stravaId != (orig.stravaId ?: "") ||
                updated.content != orig.content ||
                updated.newPhotos.isNotEmpty() ||
                updated.editedExistingPhotos != orig.photos
            } ?: false
            _uiState.value = updated.copy(hasChanges = hasChanges)
        }
    }

    fun submit(context: Context) {
        val current = _uiState.value
        if (current !is EditDayUiState.Editing) return

        viewModelScope.launch {
            try {
                val updatedEntry = current.dayEntry.copy(
                    status = current.status,
                    stravaId = current.stravaId.ifEmpty { null },
                    content = current.content,
                    photos = current.editedExistingPhotos
                )

                val repository = ApiClient.repository

                if (existingBranchName != null) {
                    // Commit to existing PR branch
                    _uiState.value = EditDayUiState.Submitting("Uploading photos...")

                    repository.commitToExistingBranch(
                        existingBranchName!!,
                        updatedEntry,
                        current.newPhotos,
                        context
                    )
                        .onSuccess { result ->
                            _uiState.value = EditDayUiState.Success(result)
                        }
                        .onFailure { e ->
                            _uiState.value = EditDayUiState.Error(
                                e.message ?: "Failed to commit changes"
                            )
                        }
                } else {
                    // Create new PR
                    _uiState.value = EditDayUiState.Submitting("Creating branch...")
                    _uiState.value = EditDayUiState.Submitting("Uploading photos...")

                    repository.updateDayEntry(updatedEntry, current.newPhotos, context)
                        .onSuccess { result ->
                            _uiState.value = EditDayUiState.Success(result)
                        }
                        .onFailure { e ->
                            _uiState.value = EditDayUiState.Error(
                                e.message ?: "Failed to create pull request"
                            )
                        }
                }
            } catch (e: Exception) {
                _uiState.value = EditDayUiState.Error(
                    "Error: ${e.message ?: "Unknown error occurred"}"
                )
            }
        }
    }
}

sealed class EditDayUiState {
    object Loading : EditDayUiState()
    data class Editing(
        val dayEntry: DayEntry,
        val status: String,
        val stravaId: String,
        val content: String,
        val newPhotos: List<SelectedPhoto>,
        val editedExistingPhotos: List<PhotoWithCaption>,
        val hasChanges: Boolean
    ) : EditDayUiState()
    data class Submitting(val message: String) : EditDayUiState()
    data class Success(val result: SubmissionResult) : EditDayUiState()
    data class Error(val message: String) : EditDayUiState()
}
