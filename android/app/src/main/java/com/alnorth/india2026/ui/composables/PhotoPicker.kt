package com.alnorth.india2026.ui.composables

import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alnorth.india2026.model.PhotoWithCaption
import com.alnorth.india2026.model.SelectedPhoto
import com.alnorth.india2026.util.ThumbnailCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PhotoPickerSection(
    selectedPhotos: List<SelectedPhoto>,
    onPhotosChanged: (List<SelectedPhoto>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Photo picker launcher (Android 13+)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newPhotos = uris.map { SelectedPhoto(uri = it, caption = "") }
            onPhotosChanged(selectedPhotos + newPhotos)
        }
    }

    // Fallback for older Android versions
    val legacyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newPhotos = uris.map { SelectedPhoto(uri = it, caption = "") }
            onPhotosChanged(selectedPhotos + newPhotos)
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "New Photos (${selectedPhotos.size})",
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        legacyPickerLauncher.launch("image/*")
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add Photos")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (selectedPhotos.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                itemsIndexed(selectedPhotos) { index, photo ->
                    PhotoWithCaptionCard(
                        photo = photo,
                        onCaptionChanged = { newCaption ->
                            val updated = selectedPhotos.toMutableList()
                            updated[index] = photo.copy(caption = newCaption)
                            onPhotosChanged(updated)
                        },
                        onRemove = {
                            onPhotosChanged(selectedPhotos - photo)
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No photos selected",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun PhotoWithCaptionCard(
    photo: SelectedPhoto,
    onCaptionChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Photo thumbnail
            AsyncImage(
                model = photo.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(Modifier.width(12.dp))

            // Caption input
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = photo.caption,
                    onValueChange = onCaptionChanged,
                    label = { Text("Caption") },
                    placeholder = { Text("Describe this photo...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )
            }

            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ExistingPhotosSection(
    slug: String,
    existingPhotos: List<PhotoWithCaption>,
    onCaptionChanged: (Int, String) -> Unit,
    onMoveUp: ((Int) -> Unit)? = null,
    onMoveDown: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (existingPhotos.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = "Existing Photos (${existingPhotos.size})",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            itemsIndexed(existingPhotos) { index, photo ->
                ExistingPhotoCard(
                    slug = slug,
                    photo = photo,
                    photoNumber = index + 1,
                    isFirst = index == 0,
                    isLast = index == existingPhotos.lastIndex,
                    onCaptionChanged = { newCaption ->
                        onCaptionChanged(index, newCaption)
                    },
                    onMoveUp = if (onMoveUp != null && index > 0) {
                        { onMoveUp(index) }
                    } else null,
                    onMoveDown = if (onMoveDown != null && index < existingPhotos.lastIndex) {
                        { onMoveDown(index) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun ExistingPhotoCard(
    slug: String,
    photo: PhotoWithCaption,
    photoNumber: Int,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onCaptionChanged: (String) -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val photoUrl = "https://raw.githubusercontent.com/alnorth/india-2026/master/website/content/days/$slug/photos/${photo.filename}"

    // Thumbnail loading state
    var thumbnailFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load thumbnail asynchronously
    LaunchedEffect(photoUrl) {
        isLoading = true
        val cache = ThumbnailCache(context)
        thumbnailFile = withContext(Dispatchers.IO) {
            cache.getThumbnail(photoUrl)
        }
        isLoading = false
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Reorder buttons column
            Column(
                modifier = Modifier.padding(end = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = onMoveUp != null,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (onMoveUp != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = onMoveDown != null,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (onMoveDown != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }

            // Photo thumbnail with caching
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    thumbnailFile != null -> {
                        val bitmap = remember(thumbnailFile) {
                            BitmapFactory.decodeFile(thumbnailFile!!.absolutePath)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = photo.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Photo,
                                contentDescription = "Failed to load",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    else -> {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = "Failed to load",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Photo info and caption input
            Column(modifier = Modifier.weight(1f)) {
                // Show filename
                Text(
                    text = photo.filename,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(Modifier.height(4.dp))

                // Caption input (editable)
                OutlinedTextField(
                    value = photo.caption,
                    onValueChange = onCaptionChanged,
                    label = { Text("Caption") },
                    placeholder = { Text("Describe this photo...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )
            }
        }
    }
}
