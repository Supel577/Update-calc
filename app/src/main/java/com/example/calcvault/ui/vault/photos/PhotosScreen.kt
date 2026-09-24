package com.example.calcvault.ui.vault.photos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.calcvault.data.i18n.VaultStrings
import com.example.calcvault.data.security.VaultSecurityManager
import com.example.calcvault.ui.vault.VaultViewModel
import com.example.calcvault.ui.vault.picker.InAppGalleryPickerDialog
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    viewModel: VaultViewModel,
    securityManager: VaultSecurityManager? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage = securityManager?.appLanguage ?: "en"
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showInAppGalleryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        securityManager?.isExternalActivityActive = false
        if (uris.isNotEmpty()) {
            viewModel.importMediaItems(uris, isVideo = false)
        }
    }

    val launchSystemPicker = {
        securityManager?.isExternalActivityActive = true
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val launchPicker = {
        showInAppGalleryPicker = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("photos_screen"),
            containerColor = Color(0xFF0F172A),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isMultiSelectMode) {
                // Contextual Multi-Selection Action Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E293B),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = VaultStrings.get(appLanguage, "close"),
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "${uiState.selectedMediaIds.size} " + VaultStrings.get(appLanguage, "photos_selected_count"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.selectAll(photos) }) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = VaultStrings.get(appLanguage, "photos_select_all"),
                                    tint = Color(0xFF38BDF8)
                                )
                            }
                            IconButton(onClick = { viewModel.batchUnhide(photos) }) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = VaultStrings.get(appLanguage, "photos_unhide_selected"),
                                    tint = Color(0xFF38BDF8)
                                )
                            }
                            IconButton(onClick = { viewModel.batchMoveToTrash(photos) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = VaultStrings.get(appLanguage, "photos_move_trash"),
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = VaultStrings.get(appLanguage, "back"),
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = VaultStrings.get(appLanguage, "photos_title"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${photos.size} " + VaultStrings.get(appLanguage, "photos_count_suffix"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(
                        onClick = launchPicker
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = VaultStrings.get(appLanguage, "photos_import_btn"),
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isMultiSelectMode) {
                FloatingActionButton(
                    onClick = launchPicker,
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_photos")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = VaultStrings.get(appLanguage, "photos_import_btn")
                    )
                }
            }
        }
    ) { innerPadding ->
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = CircleShape,
                        color = Color(0xFF1E293B)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = VaultStrings.get(appLanguage, "photos_empty_title"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = VaultStrings.get(appLanguage, "photos_empty_desc"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = launchPicker,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(VaultStrings.get(appLanguage, "photos_import_btn"))
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos, key = { it.id }) { photo ->
                    val file = remember(photo.fileName) { viewModel.getMediaFile(photo) }
                    val isSelected = uiState.selectedMediaIds.contains(photo.id)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    if (uiState.isMultiSelectMode) {
                                        viewModel.toggleSelection(photo.id)
                                    } else {
                                        viewModel.selectMediaForView(photo)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(photo.id)
                                }
                            )
                            .testTag("photo_item_${photo.id}")
                    ) {
                        AsyncImage(
                            model = file,
                            contentDescription = photo.originalName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Selection indicator or Discreet lock badge
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .background(Color(0xFF0284C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .padding(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // In-App Gallery Picker Dialog
    if (showInAppGalleryPicker) {
        InAppGalleryPickerDialog(
            initialMediaType = "photos",
            appLanguage = appLanguage,
            onDismiss = { showInAppGalleryPicker = false },
            onOpenSystemPicker = { launchSystemPicker() },
            onImportSelected = { selectedPhotos, selectedVideos ->
                if (selectedVideos.isEmpty()) {
                    viewModel.importMediaItems(selectedPhotos, isVideo = false)
                } else {
                    viewModel.importMixedMediaItems(selectedPhotos, selectedVideos)
                }
            }
        )
    }

    // Detail Viewer Dialog
    if (uiState.isViewingMediaDetail && uiState.selectedMedia != null) {
        val selected = uiState.selectedMedia!!
        MediaDetailViewerDialog(
            media = selected,
            file = viewModel.getMediaFile(selected),
            appLanguage = appLanguage,
            onClose = { viewModel.closeMediaView() },
            onUnhide = { viewModel.unhideMedia(selected) },
            onDelete = { viewModel.moveToTrash(selected) }
        )
    }
}
}
