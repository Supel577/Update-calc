package com.example.calcvault.ui.vault.photos

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.calcvault.data.i18n.VaultStrings
import com.example.calcvault.data.model.VaultMedia
import com.example.calcvault.data.model.VaultMediaType
import com.example.calcvault.ui.vault.videos.VideoEditorDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VideoAspectMode(val label: String) {
    FILL_ZERO_GAP("Full Screen (Zero Gap)"),
    FIT_CENTER("Fit Original Video"),
    STRETCH_FULL("Stretch Full")
}

private class TextureVideoPlayerView(context: Context) : FrameLayout(context), TextureView.SurfaceTextureListener {
    private val textureView = TextureView(context)
    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null
    var videoWidth = 0
        private set
    var videoHeight = 0
        private set

    var aspectMode: VideoAspectMode = VideoAspectMode.FIT_CENTER
        set(value) {
            field = value
            updateTransform()
        }

    init {
        setBackgroundColor(android.graphics.Color.BLACK)
        textureView.surfaceTextureListener = this
        addView(
            textureView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
    }

    fun attachMediaPlayer(mp: MediaPlayer) {
        mediaPlayer = mp
        surface?.let { s ->
            if (s.isValid) {
                mp.setSurface(s)
            }
        }
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
        surface?.release()
        val s = Surface(st)
        surface = s
        mediaPlayer?.setSurface(s)
        updateTransform(width, height)
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
        updateTransform(width, height)
    }

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        mediaPlayer?.setSurface(null)
        surface?.release()
        surface = null
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateTransform(right - left, bottom - top)
    }

    fun setVideoDimensions(w: Int, h: Int) {
        videoWidth = w
        videoHeight = h
        updateTransform()
    }

    fun updateTransform(w: Int = width, h: Int = height) {
        if (w <= 0 || h <= 0 || videoWidth <= 0 || videoHeight <= 0) return

        val viewRatio = w.toFloat() / h.toFloat()
        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
        val matrix = Matrix()

        val scaleX: Float
        val scaleY: Float

        when (aspectMode) {
            VideoAspectMode.FIT_CENTER -> {
                // Preserves exact natural aspect ratio, centered in screen with clean black bars
                if (viewRatio > videoRatio) {
                    scaleX = videoRatio / viewRatio
                    scaleY = 1f
                } else {
                    scaleX = 1f
                    scaleY = viewRatio / videoRatio
                }
            }
            VideoAspectMode.FILL_ZERO_GAP -> {
                // 100% Full Screen with ZERO gap and ZERO black bars, preserving 1:1 pixel aspect ratio (no distortion / no squishing!)
                if (viewRatio > videoRatio) {
                    scaleX = 1f
                    scaleY = viewRatio / videoRatio
                } else {
                    scaleX = videoRatio / viewRatio
                    scaleY = 1f
                }
            }
            VideoAspectMode.STRETCH_FULL -> {
                scaleX = 1f
                scaleY = 1f
            }
        }

        matrix.setScale(scaleX, scaleY, w / 2f, h / 2f)
        textureView.setTransform(matrix)
    }

    fun release() {
        surface?.release()
        surface = null
        mediaPlayer = null
    }
}

@Composable
fun MediaDetailViewerDialog(
    media: VaultMedia,
    file: File,
    appLanguage: String = "en",
    onClose: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var fileReloadTrigger by remember { mutableIntStateOf(0) }
    val isVideo = media.mediaType == VaultMediaType.VIDEO
    val formattedDate = remember(media.dateAdded) {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(media.dateAdded))
    }
    val formattedSize = remember(media.sizeBytes) {
        val mb = media.sizeBytes / (1024.0 * 1024.0)
        if (mb >= 1.0) "%.1f MB".format(mb) else "${media.sizeBytes / 1024} KB"
    }

    // Photo Zoom & Pan State
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Video Playback State
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var playerViewRef by remember { mutableStateOf<TextureVideoPlayerView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPlaybackCompleted by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    // Audio & Speed & Orientation states
    var isMuted by remember { mutableStateOf(false) }
    val speedOptions = remember { listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f) }
    var currentSpeedIndex by remember { mutableIntStateOf(1) } // 1.0f
    var isLandscape by remember { mutableStateOf(false) }
    var videoAspectMode by remember { mutableStateOf(VideoAspectMode.FIT_CENTER) }
    var aspectNotificationText by remember { mutableStateOf<String?>(null) }

    // Auto-dismiss aspect mode badge
    LaunchedEffect(aspectNotificationText) {
        if (aspectNotificationText != null) {
            delay(1800)
            aspectNotificationText = null
        }
    }

    // Auto-hide controls after 3.5 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3500)
            showControls = false
        }
    }

    // Video progress polling loop
    LaunchedEffect(mediaPlayerRef, isPlaying, isUserScrubbing) {
        while (isActive) {
            val mp = mediaPlayerRef
            if (mp != null && isPlaying && !isUserScrubbing) {
                try {
                    currentPositionMs = mp.currentPosition
                    val dur = mp.duration
                    if (dur > 0 && totalDurationMs != dur) {
                        totalDurationMs = dur
                    }
                } catch (_: Exception) {}
            }
            delay(250)
        }
    }

    // Back button handling to close viewer
    BackHandler(enabled = true) {
        onClose()
    }

    // Fullscreen Immersive Mode: Hide system bars while viewing, restore on exit
    val view = LocalView.current
    val window = activity?.window
    DisposableEffect(window) {
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = originalOrientation
            mediaPlayerRef?.let { mp ->
                try {
                    if (mp.isPlaying) mp.stop()
                    mp.reset()
                    mp.release()
                } catch (_: Exception) {}
            }
            mediaPlayerRef = null
            playerViewRef?.release()
            playerViewRef = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("media_viewer_dialog")
    ) {
        // MAIN MEDIA CONTENT
        if (isVideo) {
            // Built-in True Fullscreen Video Player
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                            },
                            onDoubleTap = {
                                // Quick double-tap toggles between Fullscreen (Zero Gap) and Fit Original Video
                                videoAspectMode = if (videoAspectMode == VideoAspectMode.FILL_ZERO_GAP) {
                                    VideoAspectMode.FIT_CENTER
                                } else {
                                    VideoAspectMode.FILL_ZERO_GAP
                                }
                                playerViewRef?.aspectMode = videoAspectMode
                                playerViewRef?.updateTransform()
                                aspectNotificationText = videoAspectMode.label
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val pView = TextureVideoPlayerView(ctx).apply {
                            this.aspectMode = videoAspectMode
                        }
                        // Query natural video dimensions accounting for 90/270 degree rotation metadata
                        val retriever = MediaMetadataRetriever()
                        var naturalW = 0
                        var naturalH = 0
                        try {
                            retriever.setDataSource(file.absolutePath)
                            val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                            val rW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                            val rH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                            if (rot == 90 || rot == 270) {
                                naturalW = rH
                                naturalH = rW
                            } else {
                                naturalW = rW
                                naturalH = rH
                            }
                        } catch (_: Exception) {} finally {
                            try { retriever.release() } catch (_: Exception) {}
                        }
                        if (naturalW > 0 && naturalH > 0) {
                            pView.setVideoDimensions(naturalW, naturalH)
                        }
                        val mp = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build()
                            )
                            try {
                                setDataSource(file.absolutePath)
                                setOnPreparedListener { player ->
                                    mediaPlayerRef = player
                                    totalDurationMs = player.duration
                                    if (naturalW > 0 && naturalH > 0) {
                                        pView.setVideoDimensions(naturalW, naturalH)
                                    } else {
                                        pView.setVideoDimensions(player.videoWidth, player.videoHeight)
                                    }
                                    player.start()
                                    isPlaying = true
                                    isPlaybackCompleted = false
                                }
                                setOnVideoSizeChangedListener { _, w, h ->
                                    if (w > 0 && h > 0) {
                                        pView.setVideoDimensions(w, h)
                                    }
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    isPlaybackCompleted = true
                                    showControls = true
                                }
                                setOnErrorListener { _, _, _ ->
                                    isPlaying = false
                                    true
                                }
                                prepareAsync()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        pView.attachMediaPlayer(mp)
                        playerViewRef = pView
                        mediaPlayerRef = mp
                        pView
                    },
                    update = { pView ->
                        pView.aspectMode = videoAspectMode
                        pView.updateTransform()
                    }
                )

                // Floating Aspect Ratio Mode Indicator
                AnimatedVisibility(
                    visible = aspectNotificationText != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = aspectNotificationText ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Big Center Play/Pause / Rewind / Forward Action Overlay
                AnimatedVisibility(
                    visible = showControls || !isPlaying,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Rewind 10 Seconds
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            IconButton(
                                onClick = {
                                    val mp = mediaPlayerRef ?: return@IconButton
                                    try {
                                        val target = (mp.currentPosition - 10000).coerceAtLeast(0)
                                        mp.seekTo(target)
                                        currentPositionMs = target
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Main Play / Pause
                        Surface(
                            modifier = Modifier.size(76.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.75f)
                        ) {
                            IconButton(
                                onClick = {
                                    val mp = mediaPlayerRef ?: return@IconButton
                                    try {
                                        if (isPlaybackCompleted) {
                                            mp.seekTo(0)
                                            mp.start()
                                            isPlaying = true
                                            isPlaybackCompleted = false
                                        } else if (mp.isPlaying) {
                                            mp.pause()
                                            isPlaying = false
                                        } else {
                                            mp.start()
                                            isPlaying = true
                                        }
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Icon(
                                    imageVector = when {
                                        isPlaybackCompleted -> Icons.Default.Replay
                                        isPlaying -> Icons.Default.Pause
                                        else -> Icons.Default.PlayArrow
                                    },
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        // Forward 10 Seconds
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            IconButton(
                                onClick = {
                                    val mp = mediaPlayerRef ?: return@IconButton
                                    try {
                                        val target = (mp.currentPosition + 10000).coerceAtMost(totalDurationMs)
                                        mp.seekTo(target)
                                        currentPositionMs = target
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }

                // Video Scrubbing HUD Controls (Bottom of player, shown when tapped)
                AnimatedVisibility(
                    visible = showControls || !isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val mp = mediaPlayerRef ?: return@IconButton
                                        try {
                                            if (isPlaybackCompleted) {
                                                mp.seekTo(0)
                                                mp.start()
                                                isPlaying = true
                                                isPlaybackCompleted = false
                                            } else if (mp.isPlaying) {
                                                mp.pause()
                                                isPlaying = false
                                            } else {
                                                mp.start()
                                                isPlaying = true
                                            }
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = "${formatDuration(currentPositionMs)} / ${formatDuration(totalDurationMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Secondary Controls: Mute, Speed, Auto-rotate, Fullscreen Aspect Mode
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Mute / Unmute
                                IconButton(
                                    onClick = {
                                        isMuted = !isMuted
                                        mediaPlayerRef?.let { mp ->
                                            try {
                                                val vol = if (isMuted) 0f else 1f
                                                mp.setVolume(vol, vol)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = "Sound Toggle",
                                        tint = if (isMuted) Color(0xFFEF4444) else Color.White
                                    )
                                }

                                // Playback Speed Toggle
                                TextButton(
                                    onClick = {
                                        currentSpeedIndex = (currentSpeedIndex + 1) % speedOptions.size
                                        val newSpeed = speedOptions[currentSpeedIndex]
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            mediaPlayerRef?.let { mp ->
                                                try {
                                                    mp.playbackParams = mp.playbackParams.setSpeed(newSpeed)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "${speedOptions[currentSpeedIndex]}x",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Auto-Rotate / Screen Orientation Toggle
                                IconButton(
                                    onClick = {
                                        isLandscape = !isLandscape
                                        activity?.requestedOrientation = if (isLandscape) {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = "Rotate",
                                        tint = if (isLandscape) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                }

                                // Fill Screen / Fit Aspect Toggle
                                IconButton(
                                    onClick = {
                                        videoAspectMode = when (videoAspectMode) {
                                            VideoAspectMode.FILL_ZERO_GAP -> VideoAspectMode.FIT_CENTER
                                            VideoAspectMode.FIT_CENTER -> VideoAspectMode.STRETCH_FULL
                                            VideoAspectMode.STRETCH_FULL -> VideoAspectMode.FILL_ZERO_GAP
                                        }
                                        playerViewRef?.let { pv ->
                                            pv.aspectMode = videoAspectMode
                                            pv.updateTransform()
                                        }
                                        aspectNotificationText = videoAspectMode.label
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = when (videoAspectMode) {
                                            VideoAspectMode.FILL_ZERO_GAP -> Icons.Default.Fullscreen
                                            VideoAspectMode.FIT_CENTER -> Icons.Default.FitScreen
                                            VideoAspectMode.STRETCH_FULL -> Icons.Default.AspectRatio
                                        },
                                        contentDescription = videoAspectMode.label,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Slider(
                            value = currentPositionMs.toFloat().coerceIn(0f, (totalDurationMs.coerceAtLeast(1)).toFloat()),
                            onValueChange = { newPos ->
                                isUserScrubbing = true
                                currentPositionMs = newPos.toInt()
                            },
                            onValueChangeFinished = {
                                try {
                                    mediaPlayerRef?.seekTo(currentPositionMs)
                                } catch (_: Exception) {}
                                isUserScrubbing = false
                            },
                            valueRange = 0f..(totalDurationMs.coerceAtLeast(1)).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        } else {
                // Photo Viewer with Pinch-to-Zoom, Pan, Single-tap controls toggle, and Double-Tap reset
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    showControls = !showControls
                                },
                                onDoubleTap = {
                                    if (zoomScale > 1f) {
                                        zoomScale = 1f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    } else {
                                        zoomScale = 2.5f
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                zoomScale = newScale
                                if (newScale > 1f) {
                                    panOffsetX += pan.x
                                    panOffsetY += pan.y
                                    showControls = false
                                } else {
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = remember(file, fileReloadTrigger) {
                            ImageRequest.Builder(context)
                                .data(file)
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = media.originalName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffsetX
                                translationY = panOffsetY
                            }
                    )

                    // Zoom indicator badge when zoomed in
                    if (zoomScale > 1.05f) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    zoomScale = 1f
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                },
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset Zoom",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1fx (Reset)".format(zoomScale),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Top Bar (Auto-hides cleanly for an unhindered full-screen viewing experience)
            AnimatedVisibility(
                visible = showControls,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(start = 14.dp, end = 14.dp, top = 26.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = media.originalName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$formattedSize • $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showEditorDialog = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (isVideo) "Edit Video" else "Edit Photo",
                                tint = Color(0xFF38BDF8)
                            )
                        }

                        // Unhide Action Button
                        IconButton(
                            onClick = onUnhide,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("btn_unhide_media")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = VaultStrings.get(appLanguage, "photos_unhide_selected"),
                                tint = Color(0xFF38BDF8)
                            )
                        }

                        // Trash Action Button
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("btn_delete_media")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = VaultStrings.get(appLanguage, "photos_move_trash"),
                                tint = Color(0xFFF87171)
                            )
                        }
                    }
                }
            }
        }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(VaultStrings.get(appLanguage, "photos_trash_dialog_title")) },
            text = { Text(String.format(VaultStrings.get(appLanguage, "photos_trash_dialog_desc"), media.originalName)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text(VaultStrings.get(appLanguage, "photos_move_trash"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(VaultStrings.get(appLanguage, "cancel"))
                }
            }
        )
    }

    if (showEditorDialog) {
        if (isVideo) {
            VideoEditorDialog(
                media = media,
                file = file,
                onDismiss = { showEditorDialog = false },
                onSaved = {
                    showEditorDialog = false
                    fileReloadTrigger++
                }
            )
        } else {
            ImageEditorDialog(
                media = media,
                file = file,
                onDismiss = { showEditorDialog = false },
                onSaved = {
                    showEditorDialog = false
                    fileReloadTrigger++
                }
            )
        }
    }
}

private fun formatDuration(millis: Int): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
