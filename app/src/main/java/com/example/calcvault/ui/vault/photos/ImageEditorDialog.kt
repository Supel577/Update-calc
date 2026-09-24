package com.example.calcvault.ui.vault.photos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.calcvault.data.model.VaultMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class EditorTab {
    CROP,
    ROTATE,
    FLIP,
    BRIGHTNESS
}

enum class CropRatio(val label: String, val ratio: Float?) {
    ORIGINAL("Free", null),
    SQUARE("1:1", 1.0f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f)
}

@Composable
fun ImageEditorDialog(
    media: VaultMedia,
    file: File,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var activeTab by remember { mutableStateOf(EditorTab.ROTATE) }
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) } // -100f to +100f
    var selectedCropRatio by remember { mutableStateOf(CropRatio.ORIGINAL) }

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Load original bitmap
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, opts)
                var sampleSize = 1
                val maxDim = 1920
                while (opts.outWidth / sampleSize > maxDim || opts.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }
                val loadOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val bmp = BitmapFactory.decodeFile(file.absolutePath, loadOpts)
                originalBitmap = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isProcessing = false
            }
        }
    }

    // Recompute preview bitmap whenever parameters change
    LaunchedEffect(originalBitmap, rotationDegrees, flipHorizontal, flipVertical, brightness, selectedCropRatio) {
        val base = originalBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            try {
                // 1. Matrix transformations: rotation & flip
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                val sx = if (flipHorizontal) -1f else 1f
                val sy = if (flipVertical) -1f else 1f
                matrix.postScale(sx, sy)

                var transformed = Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)

                // 2. Crop
                val cropRatio = selectedCropRatio.ratio
                if (cropRatio != null) {
                    val w = transformed.width
                    val h = transformed.height
                    val targetW: Int
                    val targetH: Int
                    if (w.toFloat() / h.toFloat() > cropRatio) {
                        targetH = h
                        targetW = (h * cropRatio).toInt().coerceAtMost(w)
                    } else {
                        targetW = w
                        targetH = (w / cropRatio).toInt().coerceAtMost(h)
                    }
                    val startX = ((w - targetW) / 2).coerceAtLeast(0)
                    val startY = ((h - targetH) / 2).coerceAtLeast(0)
                    val cropped = Bitmap.createBitmap(transformed, startX, startY, targetW, targetH)
                    if (cropped != transformed) {
                        transformed = cropped
                    }
                }

                // 3. Brightness
                if (brightness != 0f) {
                    val brightnessBmp = Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(brightnessBmp)
                    val cm = ColorMatrix(
                        floatArrayOf(
                            1f, 0f, 0f, 0f, brightness * 1.5f,
                            0f, 1f, 0f, 0f, brightness * 1.5f,
                            0f, 0f, 1f, 0f, brightness * 1.5f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    val paint = Paint().apply {
                        colorFilter = ColorMatrixColorFilter(cm)
                    }
                    canvas.drawBitmap(transformed, 0f, 0f, paint)
                    transformed = brightnessBmp
                }

                previewBitmap = transformed
            } catch (e: Exception) {
                e.printStackTrace()
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
                .testTag("image_editor_dialog"),
            color = Color(0xFF0A0D14)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action Bar
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

                    Text(
                        text = "Photo Editor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Button(
                        onClick = {
                            val toSave = previewBitmap ?: return@Button
                            isSaving = true
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    try {
                                        FileOutputStream(file).use { out ->
                                            toSave.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                isSaving = false
                                onSaved()
                            }
                        },
                        enabled = !isSaving && previewBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSaving) {
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

                // Center Image Viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF030712))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isProcessing || originalBitmap == null -> {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                        }
                        previewBitmap != null -> {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Editor Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Editor Controls Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF161B26),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Dynamic Sub-Controls per tab
                        when (activeTab) {
                            EditorTab.ROTATE -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            rotationDegrees = (rotationDegrees - 90 + 360) % 360
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                                            contentDescription = "Rotate Left",
                                            tint = Color(0xFF38BDF8)
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text("-90°", color = Color.White)
                                    }

                                    Text(
                                        text = "$rotationDegrees°",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )

                                    Button(
                                        onClick = {
                                            rotationDegrees = (rotationDegrees + 90) % 360
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                            contentDescription = "Rotate Right",
                                            tint = Color(0xFF38BDF8)
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text("+90°", color = Color.White)
                                    }
                                }
                            }

                            EditorTab.FLIP -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { flipHorizontal = !flipHorizontal },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (flipHorizontal) Color(0xFF0284C7) else Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Horizontal Flip",
                                            tint = if (flipHorizontal) Color.White else Color(0xFF38BDF8)
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text("Flip H")
                                    }

                                    Button(
                                        onClick = { flipVertical = !flipVertical },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (flipVertical) Color(0xFF0284C7) else Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Vertical Flip",
                                            tint = if (flipVertical) Color.White else Color(0xFF38BDF8)
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text("Flip V")
                                    }

                                    if (flipHorizontal || flipVertical) {
                                        IconButton(
                                            onClick = {
                                                flipHorizontal = false
                                                flipVertical = false
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Reset Flip",
                                                tint = Color(0xFFF87171)
                                            )
                                        }
                                    }
                                }
                            }

                            EditorTab.CROP -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    CropRatio.entries.forEach { ratio ->
                                        val isSelected = selectedCropRatio == ratio
                                        Button(
                                            onClick = { selectedCropRatio = ratio },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(ratio.label, color = if (isSelected) Color.White else Color(0xFF94A3B8))
                                        }
                                    }
                                }
                            }

                            EditorTab.BRIGHTNESS -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Brightness Level",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = "${brightness.toInt()}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                    Slider(
                                        value = brightness,
                                        onValueChange = { brightness = it },
                                        valueRange = -80f..80f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF38BDF8),
                                            activeTrackColor = Color(0xFF0284C7),
                                            inactiveTrackColor = Color(0xFF334155)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Tab Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            EditorTabButton(
                                title = "Rotate",
                                icon = Icons.Default.RotateRight,
                                isSelected = activeTab == EditorTab.ROTATE,
                                onClick = { activeTab = EditorTab.ROTATE }
                            )
                            EditorTabButton(
                                title = "Flip",
                                icon = Icons.Default.SwapHoriz,
                                isSelected = activeTab == EditorTab.FLIP,
                                onClick = { activeTab = EditorTab.FLIP }
                            )
                            EditorTabButton(
                                title = "Crop",
                                icon = Icons.Default.Crop,
                                isSelected = activeTab == EditorTab.CROP,
                                onClick = { activeTab = EditorTab.CROP }
                            )
                            EditorTabButton(
                                title = "Brightness",
                                icon = Icons.Default.Brightness6,
                                isSelected = activeTab == EditorTab.BRIGHTNESS,
                                onClick = { activeTab = EditorTab.BRIGHTNESS }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
