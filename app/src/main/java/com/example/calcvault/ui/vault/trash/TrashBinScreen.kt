package com.example.calcvault.ui.vault.trash

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.compose.AsyncImage
import coil.request.videoFrameMillis
import com.example.calcvault.data.model.VaultMedia
import com.example.calcvault.data.model.VaultMediaType
import com.example.calcvault.ui.vault.VaultViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrashBinScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
    val trashItems by viewModel.trash.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var itemToPermanentDelete by remember { mutableStateOf<VaultMedia?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("trash_bin_screen"),
        containerColor = Color(0xFF0B0F17),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.isMultiSelectMode) "${uiState.selectedMediaIds.size} Selected" else "Trash Bin (${trashItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                if (trashItems.isNotEmpty()) {
                    if (uiState.isMultiSelectMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { viewModel.batchRestore(trashItems) }) {
                                Icon(Icons.Default.Restore, contentDescription = "Batch Restore", tint = Color(0xFF38BDF8))
                            }
                            IconButton(onClick = { showBatchDeleteConfirm = true }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Batch Delete", tint = Color(0xFFEF4444))
                            }
                        }
                    } else {
                        Button(
                            onClick = { showEmptyTrashConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Empty Trash",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (trashItems.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Trash Bin is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Deleted files will stay here before permanent removal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trashItems, key = { it.id }) { item ->
                        val isSelected = uiState.selectedMediaIds.contains(item.id)
                        val file = viewModel.getMediaFile(item)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (uiState.isMultiSelectMode) {
                                            viewModel.toggleSelection(item.id)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(item.id)
                                    }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131B2E)
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(56.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    val context = LocalContext.current
                                    if ((item.mediaType == VaultMediaType.PHOTO || item.mediaType == VaultMediaType.VIDEO) && file.exists()) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = remember(file.absolutePath) {
                                                    if (item.mediaType == VaultMediaType.VIDEO) {
                                                        ImageRequest.Builder(context)
                                                            .data(file)
                                                            .decoderFactory(VideoFrameDecoder.Factory())
                                                            .videoFrameMillis(1000)
                                                            .crossfade(true)
                                                            .build()
                                                    } else {
                                                        ImageRequest.Builder(context)
                                                            .data(file)
                                                            .crossfade(true)
                                                            .build()
                                                    }
                                                },
                                                imageLoader = videoImageLoader,
                                                contentDescription = item.originalName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (item.mediaType == VaultMediaType.VIDEO) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.35f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when (item.mediaType) {
                                                    VaultMediaType.VIDEO -> Icons.Default.Videocam
                                                    VaultMediaType.AUDIO -> Icons.Default.AudioFile
                                                    else -> Icons.Default.Image
                                                },
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.originalName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${item.sizeBytes / 1024} KB • Deleted recently",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { viewModel.restoreFromTrash(item) }) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = "Restore",
                                            tint = Color(0xFF38BDF8)
                                        )
                                    }
                                    IconButton(onClick = { itemToPermanentDelete = item }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Delete Permanently",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation: Empty Trash
    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("Empty Trash Bin?") },
            text = { Text("Are you sure you want to permanently erase all ${trashItems.size} items in the trash? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyTrashConfirm = false
                        viewModel.emptyTrash()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Empty Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation: Batch Delete
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("Permanently Delete Selected?") },
            text = { Text("Permanently erase ${uiState.selectedMediaIds.size} selected items? They cannot be recovered.") },
            confirmButton = {
                Button(
                    onClick = {
                        showBatchDeleteConfirm = false
                        viewModel.batchPermanentDelete(trashItems)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation: Single Item Delete
    itemToPermanentDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToPermanentDelete = null },
            title = { Text("Permanently Delete Item?") },
            text = { Text("Permanently delete '${item.originalName}'? It cannot be recovered.") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = itemToPermanentDelete
                        itemToPermanentDelete = null
                        if (toDel != null) {
                            viewModel.permanentDelete(toDel)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToPermanentDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
