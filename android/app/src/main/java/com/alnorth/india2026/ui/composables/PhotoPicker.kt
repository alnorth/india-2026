package com.alnorth.india2026.ui.composables

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alnorth.india2026.model.PhotoWithCaption
import com.alnorth.india2026.model.SelectedPhoto

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
    branchName: String?,
    existingPhotos: List<PhotoWithCaption>,
    modifier: Modifier = Modifier
) {
    if (existingPhotos.isEmpty()) return

    val branch = branchName ?: "master"

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
                    branch = branch,
                    photo = photo,
                    photoNumber = index + 1
                )
            }
        }
    }
}

@Composable
fun ExistingPhotoCard(
    slug: String,
    branch: String,
    photo: PhotoWithCaption,
    photoNumber: Int
) {
    val photoUrl = "https://raw.githubusercontent.com/alnorth/india-2026/$branch/website/content/days/$slug/photos/${photo.filename}"

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Photo thumbnail
            AsyncImage(
                model = photoUrl,
                contentDescription = photo.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(Modifier.width(12.dp))

            // Caption (read-only)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Photo $photoNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (photo.caption.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = photo.caption,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "No caption",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
