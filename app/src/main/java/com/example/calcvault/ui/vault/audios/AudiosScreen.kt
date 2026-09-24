package com.example.calcvault.ui.vault.audios

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calcvault.data.model.VaultMedia
import com.example.calcvault.data.security.VaultSecurityManager
import com.example.calcvault.ui.vault.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudiosScreen(
    viewModel: VaultViewModel,
    securityManager: VaultSecurityManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audios by viewModel.audios.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val appLanguage by securityManager.appLanguageFlow.collectAsStateWithLifecycle()

    var showSecretRecorder by remember { mutableStateOf(false) }
    var currentPlayingAudio by remember { mutableStateOf<VaultMedia?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPreparing by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }

    // User seeking / scrubbing state
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosMs by remember { mutableFloatStateOf(0f) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioItems(uris)
        }
    }

    // Launch audio picker safely without triggering auto-lock
    val openAudioPicker = {
        securityManager.isExternalActivityActive = true
        audioPickerLauncher.launch("audio/*")
    }

    // Playback progress ticker
    LaunchedEffect(mediaPlayer, isPlaying, isScrubbing) {
        while (isActive) {
            val mp = mediaPlayer
            if (mp != null && isPlaying && !isScrubbing) {
                try {
                    currentPosMs = mp.currentPosition
                    if (mp.duration > 0) {
                        durationMs = mp.duration
                    }
                } catch (_: Exception) {}
            }
            delay(250)
        }
    }

    // Helper to start playback of a VaultMedia audio item asynchronously (supports any size file)
    fun playAudioItem(audio: VaultMedia) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
        currentPosMs = 0
        durationMs = 0

        val file = viewModel.getMediaFile(audio)
        if (!file.exists()) {
            Log.e("AudiosScreen", "Audio file does not exist: ${file.absolutePath}")
            return
        }

        isPreparing = true
        currentPlayingAudio = audio

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                java.io.FileInputStream(file).use { fis ->
                    setDataSource(fis.fd)
                }
                setOnPreparedListener { player ->
                    isPreparing = false
                    durationMs = player.duration
                    player.start()
                    isPlaying = true
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosMs = 0
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudiosScreen", "MediaPlayer error: what=$what, extra=$extra")
                    isPreparing = false
                    isPlaying = false
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e("AudiosScreen", "Failed to start audio playback: ${e.message}", e)
            isPreparing = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("audios_screen"),
        containerColor = Color(0xFF0F172A),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isMultiSelectMode) {
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
                                    contentDescription = "Cancel",
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
                            IconButton(onClick = { viewModel.selectAll(audios) }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = Color(0xFF38BDF8))
                            }
                            IconButton(onClick = { viewModel.batchUnhide(audios) }) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Unhide", tint = Color(0xFF38BDF8))
                            }
                            IconButton(onClick = { viewModel.batchMoveToTrash(audios) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Hidden Audios",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${audios.size} encrypted audio files",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSecretRecorder = true }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Secret Audio Recorder",
                                tint = Color(0xFFFB7185)
                            )
                        }
                        IconButton(onClick = openAudioPicker) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = "Import Audio",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isMultiSelectMode && currentPlayingAudio == null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { showSecretRecorder = true },
                        containerColor = Color(0xFFE11D48),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = "Secret Record")
                    }

                    FloatingActionButton(
                        onClick = openAudioPicker,
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.AudioFile, contentDescription = "Import Audio")
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
            if (audios.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Hidden Audios",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to import confidential voice notes or audio tracks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = if (currentPlayingAudio != null) 160.dp else 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(audios, key = { it.id }) { audio ->
                        val isSelected = uiState.selectedMediaIds.contains(audio.id)
                        val isCurrentPlaying = currentPlayingAudio?.id == audio.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (uiState.isMultiSelectMode) {
                                            viewModel.toggleSelection(audio.id)
                                        } else {
                                            if (isCurrentPlaying) {
                                                val mp = mediaPlayer
                                                if (mp != null) {
                                                    if (isPlaying) {
                                                        mp.pause()
                                                        isPlaying = false
                                                    } else {
                                                        mp.start()
                                                        isPlaying = true
                                                    }
                                                } else {
                                                    playAudioItem(audio)
                                                }
                                            } else {
                                                playAudioItem(audio)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(audio.id)
                                    }
                                ),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D31)
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8))
                            } else if (isCurrentPlaying) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8))
                            } else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(46.dp),
                                    shape = CircleShape,
                                    color = if (isCurrentPlaying && isPlaying) Color(0xFF0284C7) else Color(0xFF1E293B)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isCurrentPlaying && isPreparing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color(0xFF38BDF8),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = when {
                                                    isSelected -> Icons.Default.Check
                                                    isCurrentPlaying && isPlaying -> Icons.Default.Pause
                                                    else -> Icons.Default.PlayArrow
                                                },
                                                contentDescription = null,
                                                tint = if (isSelected || (isCurrentPlaying && isPlaying)) Color.White else Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = audio.originalName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${audio.sizeBytes / 1024} KB • ${
                                            when {
                                                isCurrentPlaying && isPreparing -> "Loading audio..."
                                                isCurrentPlaying && isPlaying -> "Playing (${formatAudioDuration(currentPosMs)})"
                                                isCurrentPlaying -> "Paused"
                                                else -> "Encrypted audio"
                                            }
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCurrentPlaying) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                    )
                                }

                                IconButton(onClick = { viewModel.moveToTrash(audio) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Move to Trash",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Full-Featured Audio Player Card at Bottom
            AnimatedVisibility(
                visible = currentPlayingAudio != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                currentPlayingAudio?.let { audio ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E293B),
                        shadowElevation = 12.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Title row with Close button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = audio.originalName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isPreparing) "Buffering..." else "Vault Audio Player",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        try {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                        } catch (_: Exception) {}
                                        mediaPlayer = null
                                        isPlaying = false
                                        isPreparing = false
                                        currentPlayingAudio = null
                                        currentPosMs = 0
                                        durationMs = 0
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Player",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Interactive Drag & Seek Slider
                            val effectiveDuration = durationMs.coerceAtLeast(1)
                            val sliderValue = if (isScrubbing) {
                                scrubPosMs.coerceIn(0f, effectiveDuration.toFloat())
                            } else {
                                currentPosMs.toFloat().coerceIn(0f, effectiveDuration.toFloat())
                            }

                            Slider(
                                value = sliderValue,
                                onValueChange = { newVal ->
                                    isScrubbing = true
                                    scrubPosMs = newVal
                                },
                                onValueChangeFinished = {
                                    isScrubbing = false
                                    val targetMs = scrubPosMs.toInt()
                                    try {
                                        mediaPlayer?.seekTo(targetMs)
                                        currentPosMs = targetMs
                                    } catch (_: Exception) {}
                                },
                                valueRange = 0f..effectiveDuration.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF0284C7),
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                            )

                            // Timestamp indicators (Current / Total)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatAudioDuration(if (isScrubbing) scrubPosMs.toInt() else currentPosMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = formatAudioDuration(durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Controls Row: -10s, Play/Pause, +10s
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Skip backward 10s
                                IconButton(
                                    onClick = {
                                        val mp = mediaPlayer ?: return@IconButton
                                        val target = (currentPosMs - 10000).coerceAtLeast(0)
                                        try {
                                            mp.seekTo(target)
                                            currentPosMs = target
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Back 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(18.dp))

                                // Play / Pause primary button
                                Surface(
                                    onClick = {
                                        val mp = mediaPlayer
                                        if (mp == null) {
                                            currentPlayingAudio?.let { playAudioItem(it) }
                                        } else {
                                            if (isPlaying) {
                                                mp.pause()
                                                isPlaying = false
                                            } else {
                                                mp.start()
                                                isPlaying = true
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isPreparing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(18.dp))

                                // Skip forward 10s
                                IconButton(
                                    onClick = {
                                        val mp = mediaPlayer ?: return@IconButton
                                        val target = (currentPosMs + 10000).coerceAtMost(durationMs)
                                        try {
                                            mp.seekTo(target)
                                            currentPosMs = target
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Forward 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSecretRecorder) {
        SecretAudioRecorderDialog(
            appLanguage = appLanguage,
            onDismiss = { showSecretRecorder = false }
        )
    }
}

private fun formatAudioDuration(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
