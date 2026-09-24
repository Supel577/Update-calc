package com.example.calcvault.ui.vault.videos

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.calcvault.data.model.VaultMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.Locale

@Composable
fun VideoEditorDialog(
    media: VaultMedia,
    file: File,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onFrameExtracted: ((File) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var totalDurationMs by remember { mutableIntStateOf(0) }
    var trimRange by remember { mutableStateOf(0f..1f) }
    var muteAudio by remember { mutableStateOf(false) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    // Read total duration from metadata
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val dur = durStr?.toIntOrNull() ?: 1000
                totalDurationMs = dur.coerceAtLeast(1000)
                trimRange = 0f..totalDurationMs.toFloat()
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
                totalDurationMs = 10000
                trimRange = 0f..10000f
            }
        }
    }

    // Playback loop confined to trim range
    LaunchedEffect(videoViewRef, isPlaying) {
        while (isActive) {
            val vv = videoViewRef
            if (vv != null && isPlaying) {
                val pos = vv.currentPosition
                currentPositionMs = pos
                if (pos >= trimRange.endInclusive.toInt()) {
                    vv.seekTo(trimRange.start.toInt())
                    vv.pause()
                    isPlaying = false
                }
            }
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    fun applyTrimAndSave() {
        val startMs = trimRange.start.toLong()
        val endMs = trimRange.endInclusive.toLong()
        if (endMs - startMs < 500) {
            Toast.makeText(context, "Trim clip must be at least 0.5s long", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true
        statusText = "Trimming video..."

        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                val tempOut = File(context.cacheDir, "trim_temp_${System.currentTimeMillis()}.mp4")
                val trimmed = performTrimMux(file, tempOut, startMs, endMs, muteAudio)
                if (trimmed && tempOut.exists() && tempOut.length() > 0) {
                    tempOut.copyTo(file, overwrite = true)
                    tempOut.delete()
                    true
                } else {
                    tempOut.delete()
                    false
                }
            }

            isProcessing = false
            if (success) {
                Toast.makeText(context, "Video edited successfully!", Toast.LENGTH_SHORT).show()
                onSaved()
            } else {
                Toast.makeText(context, "Failed to edit video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun extractCurrentFrameAsPhoto() {
        isProcessing = true
        statusText = "Extracting photo snapshot..."
        coroutineScope.launch {
            val photoFile = withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath)
                    val frame = retriever.getFrameAtTime(
                        currentPositionMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    retriever.release()
                    if (frame != null) {
                        val outFile = File(context.filesDir, "frame_snapshot_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(outFile).use { out ->
                            frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                        }
                        outFile
                    } else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            isProcessing = false
            if (photoFile != null) {
                Toast.makeText(context, "Snapshot saved to Photos!", Toast.LENGTH_SHORT).show()
                onFrameExtracted?.invoke(photoFile)
            } else {
                Toast.makeText(context, "Could not extract frame", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("video_editor_dialog"),
            color = Color(0xFF0A0D14)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Video Editor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Trim • Mute • Frame Capture",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { applyTrimAndSave() },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Save")
                            }
                        }
                    }
                }

                // Video Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                videoViewRef = this
                                setVideoPath(file.absolutePath)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = false
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                }
                            }
                        }
                    )

                    // Play/Pause Center Pill
                    IconButton(
                        onClick = {
                            val vv = videoViewRef ?: return@IconButton
                            if (vv.isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                if (vv.currentPosition < trimRange.start.toInt() || vv.currentPosition >= trimRange.endInclusive.toInt()) {
                                    vv.seekTo(trimRange.start.toInt())
                                }
                                vv.start()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    if (isProcessing) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black.copy(alpha = 0.75f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = statusText ?: "Processing...",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Editing Controls
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Trim Range Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Trim Range",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }

                            Text(
                                text = "${formatDuration(trimRange.start.toInt())} — ${formatDuration(trimRange.endInclusive.toInt())} (Length: ${formatDuration((trimRange.endInclusive - trimRange.start).toInt())})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Trim Range Slider
                        if (totalDurationMs > 0) {
                            RangeSlider(
                                value = trimRange,
                                onValueChange = { range ->
                                    trimRange = range
                                    videoViewRef?.seekTo(range.start.toInt())
                                    currentPositionMs = range.start.toInt()
                                },
                                valueRange = 0f..totalDurationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF38BDF8),
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Secondary Options: Mute Audio & Extract Frame
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute Audio Switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (muteAudio) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (muteAudio) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Mute Audio",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = muteAudio,
                                    onCheckedChange = { muteAudio = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFEF4444)
                                    )
                                )
                            }

                            // Extract Frame Button
                            Surface(
                                modifier = Modifier
                                    .clickable { extractCurrentFrameAsPhoto() },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Capture Frame",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
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

private fun formatDuration(durationMs: Int): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

private fun performTrimMux(srcFile: File, dstFile: File, startMs: Long, endMs: Long, muteAudio: Boolean): Boolean {
    var extractor: MediaExtractor? = null
    var muxer: MediaMuxer? = null
    try {
        extractor = MediaExtractor()
        extractor.setDataSource(srcFile.absolutePath)
        val trackCount = extractor.trackCount

        muxer = MediaMuxer(dstFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val indexMap = HashMap<Int, Int>()
        var maxBufferSize = -1

        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            val isAudio = mime.startsWith("audio/")
            if (isAudio && muteAudio) continue

            extractor.selectTrack(i)
            val dstTrack = muxer.addTrack(format)
            indexMap[i] = dstTrack
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                val size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                if (size > maxBufferSize) maxBufferSize = size
            }
        }

        if (maxBufferSize <= 0) maxBufferSize = 2 * 1024 * 1024
        muxer.start()

        val buffer = ByteBuffer.allocate(maxBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        // Seek to sync frame right before start time
        extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        while (true) {
            val trackIndex = extractor.sampleTrackIndex
            if (trackIndex < 0) break

            val dstTrack = indexMap[trackIndex]
            if (dstTrack == null) {
                extractor.advance()
                continue
            }

            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            bufferInfo.presentationTimeUs = extractor.sampleTime
            bufferInfo.flags = extractor.sampleFlags

            if (bufferInfo.presentationTimeUs > endMs * 1000L) {
                break
            }

            if (bufferInfo.presentationTimeUs >= startMs * 1000L) {
                muxer.writeSampleData(dstTrack, buffer, bufferInfo)
            }
            extractor.advance()
        }

        muxer.stop()
        muxer.release()
        extractor.release()
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        try { muxer?.release() } catch (_: Exception) {}
        try { extractor?.release() } catch (_: Exception) {}
        return false
    }
}
