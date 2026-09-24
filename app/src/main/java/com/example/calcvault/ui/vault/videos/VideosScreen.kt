package com.example.calcvault.ui.vault.videos

import android.media.MediaMetadataRetriever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import com.example.calcvault.data.video.SecretVideoRecorderManager
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.example.calcvault.data.security.VaultSecurityManager
import com.example.calcvault.ui.vault.VaultViewModel
import com.example.calcvault.ui.vault.photos.MediaDetailViewerDialog
import com.example.calcvault.ui.vault.picker.InAppGalleryPickerDialog
import java.io.File
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideosScreen(
    viewModel: VaultViewModel,
    securityManager: VaultSecurityManager? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage = securityManager?.appLanguage ?: "en"
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showInAppGalleryPicker by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Video frame decoder ImageLoader for fast real video thumbnail generation
    val videoImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        securityManager?.isExternalActivityActive = false
        if (uris.isNotEmpty()) {
            viewModel.importMediaItems(uris, isVideo = true)
        }
    }

    val launchSystemPicker = {
        securityManager?.isExternalActivityActive = true
        videoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }

    val launchPicker = {
        showInAppGalleryPicker = true
    }

    val isSecretVideoRecording by SecretVideoRecorderManager.isRecording.collectAsStateWithLifecycle()
    val secretVideoSeconds by SecretVideoRecorderManager.recordingSeconds.collectAsStateWithLifecycle()

    val videoPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val cam = grants[android.Manifest.permission.CAMERA] == true
        val mic = grants[android.Manifest.permission.RECORD_AUDIO] == true
        if (cam && mic) {
            SecretVideoRecorderManager.startRecording(context)
        } else {
            val msg = if (appLanguage == "bn")
                "গোপন ভিডিও রেকর্ডের জন্য ক্যামেরা ও মাইক্রোফোন পারমিশন প্রয়োজন!"
            else
                "Camera & Microphone permissions required for secret video recording!"
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        SecretVideoRecorderManager.onVideoRecordedCallback = {
            viewModel.onVaultUnlocked()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("videos_screen"),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Selection",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "${uiState.selectedMediaIds.size} Selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.selectAll(videos) }) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select All",
                                    tint = Color(0xFFF59E0B)
                                )
                            }
                            IconButton(onClick = { viewModel.batchUnhide(videos) }) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Unhide Selected",
                                    tint = Color(0xFF38BDF8)
                                )
                            }
                            IconButton(onClick = { viewModel.batchMoveToTrash(videos) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Move to Trash",
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
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (appLanguage == "bn") "লুকানো ভিডিও" else "Hidden Videos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (appLanguage == "bn") "${videos.size}টি এনক্রিপ্টেড ভিডিও" else "${videos.size} encrypted videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isSecretVideoRecording) {
                                SecretVideoRecorderManager.stopRecording(context)
                            } else {
                                val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (hasCam && hasMic) {
                                    SecretVideoRecorderManager.startRecording(context)
                                } else {
                                    videoPermissionsLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.CAMERA,
                                            android.Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSecretVideoRecording) Icons.Default.Stop else Icons.Default.Videocam,
                            contentDescription = "Secret Screen-Off Video",
                            tint = if (isSecretVideoRecording) Color(0xFFEF4444) else Color(0xFF38BDF8)
                        )
                    }
                    IconButton(
                        onClick = launchPicker
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoCall,
                            contentDescription = "Add Videos",
                            tint = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isMultiSelectMode) {
                FloatingActionButton(
                    onClick = launchPicker,
                    containerColor = Color(0xFFD97706),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_videos")
                ) {
                    Icon(imageVector = Icons.Default.VideoCall, contentDescription = "Import Videos")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSecretVideoRecording) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF7F1D1D).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (appLanguage == "bn") "স্ক্রিন-অফ গোপন ভিডিও রেকর্ড চলছে" else "Secret Video Recording Active",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = String.format("%02d:%02d", secretVideoSeconds / 60, secretVideoSeconds % 60) +
                                            if (appLanguage == "bn") " (স্ক্রিন বন্ধেও চলবে)" else " (Screen-off supported)",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Button(
                            onClick = { SecretVideoRecorderManager.stopRecording(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (appLanguage == "bn") "সেভ করুন" else "Stop & Save",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (videos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = if (appLanguage == "bn") "কোনো লুকানো ভিডিও নেই" else "No Hidden Videos Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (appLanguage == "bn") "আপনার ব্যক্তিগত ভিডিও রেকর্ডসমূহ এই ক্যালকুলেটর ভল্টে নিরাপদে লুকিয়ে রাখুন।" else "Conceal your private video recordings safely inside the encrypted calculator vault.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = launchPicker,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VideoCall, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(if (appLanguage == "bn") "ভিডিও ইম্পোর্ট করুন" else "Import Videos")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                items(videos, key = { it.id }) { video ->
                    val file = remember(video.fileName) { viewModel.getMediaFile(video) }
                    val isSelected = uiState.selectedMediaIds.contains(video.id)
                    val formattedSize = remember(video.sizeBytes) {
                        val mb = video.sizeBytes / (1024.0 * 1024.0)
                        if (mb >= 1.0) "%.1f MB".format(mb) else "${video.sizeBytes / 1024} KB"
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.25f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    if (uiState.isMultiSelectMode) {
                                        viewModel.toggleSelection(video.id)
                                    } else {
                                        viewModel.selectMediaForView(video)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(video.id)
                                }
                            )
                            .testTag("video_item_${video.id}"),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Video Thumbnail via Coil VideoFrameDecoder
                            AsyncImage(
                                model = file,
                                imageLoader = videoImageLoader,
                                contentDescription = video.originalName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Scrim Gradient Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                            )

                            // Center Play Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Selection Indicator or Filename + Size
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .background(Color(0xFFF59E0B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Bottom Label and Size Badge
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = video.originalName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formattedSize,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFFCBD5E1)
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

    // In-App Gallery Picker Dialog
    if (showInAppGalleryPicker) {
        InAppGalleryPickerDialog(
            initialMediaType = "videos",
            appLanguage = appLanguage,
            onDismiss = { showInAppGalleryPicker = false },
            onOpenSystemPicker = { launchSystemPicker() },
            onImportSelected = { selectedPhotos, selectedVideos ->
                if (selectedPhotos.isEmpty()) {
                    viewModel.importMediaItems(selectedVideos, isVideo = true)
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
