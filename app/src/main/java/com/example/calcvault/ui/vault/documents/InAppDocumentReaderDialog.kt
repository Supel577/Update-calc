package com.example.calcvault.ui.vault.documents

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.calcvault.data.model.VaultMedia
import com.example.calcvault.data.security.VaultSecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DocumentReaderType {
    PDF,
    TEXT,
    OTHER
}

fun detectReaderType(fileName: String): DocumentReaderType {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> DocumentReaderType.PDF
        in listOf("txt", "md", "json", "xml", "csv", "log", "html", "htm", "js", "ts", "py", "java", "kt", "sql", "conf", "ini", "properties", "yaml", "yml") -> DocumentReaderType.TEXT
        else -> DocumentReaderType.OTHER
    }
}

@Composable
fun InAppDocumentReaderDialog(
    document: VaultMedia,
    file: File,
    securityManager: VaultSecurityManager? = null,
    onClose: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val readerType = remember(document.originalName) { detectReaderType(document.originalName) }

    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
        ) {
            when (readerType) {
                DocumentReaderType.PDF -> {
                    InAppPdfViewer(
                        document = document,
                        file = file,
                        securityManager = securityManager,
                        onClose = onClose,
                        onUnhide = onUnhide,
                        onDelete = { showDeleteConfirmDialog = true },
                        onShowInfo = { showInfoDialog = true }
                    )
                }
                DocumentReaderType.TEXT -> {
                    InAppTextViewer(
                        document = document,
                        file = file,
                        securityManager = securityManager,
                        onClose = onClose,
                        onUnhide = onUnhide,
                        onDelete = { showDeleteConfirmDialog = true },
                        onShowInfo = { showInfoDialog = true }
                    )
                }
                DocumentReaderType.OTHER -> {
                    InAppOtherDocumentViewer(
                        document = document,
                        file = file,
                        securityManager = securityManager,
                        onClose = onClose,
                        onUnhide = onUnhide,
                        onDelete = { showDeleteConfirmDialog = true },
                        onShowInfo = { showInfoDialog = true }
                    )
                }
            }

            // Info Dialog
            if (showInfoDialog) {
                DocumentInfoDialog(
                    document = document,
                    file = file,
                    onDismiss = { showInfoDialog = false }
                )
            }

            // Delete Confirm Dialog
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Move to Trash?", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = { Text("This document will be moved to the Trash bin. You can restore it later.", color = Color(0xFFCBD5E1)) },
                    containerColor = Color(0xFF1E293B),
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = false
                                onDelete()
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    }
                )
            }
        }
    }
}

// ==========================================
// 1. IN-APP PDF VIEWER
// ==========================================
@Composable
fun InAppPdfViewer(
    document: VaultMedia,
    file: File,
    securityManager: VaultSecurityManager?,
    onClose: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit
) {
    val context = LocalContext.current
    var totalPages by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pdfError by remember { mutableStateOf<String?>(null) }

    var isNightMode by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Load total pages on start
    LaunchedEffect(file) {
        isLoading = true
        pdfError = null
        val count = getPdfPageCount(file)
        if (count > 0) {
            totalPages = count
            currentPage = 0
        } else {
            pdfError = "Unable to read PDF. It might be corrupted or protected with a password."
            isLoading = false
        }
    }

    // Load current page bitmap
    LaunchedEffect(currentPage, totalPages, file) {
        if (totalPages > 0 && currentPage in 0 until totalPages) {
            isLoading = true
            // Reset zoom on page change
            scale = 1f
            offset = Offset.Zero
            val bmp = renderPdfPage(file, currentPage)
            if (bmp != null) {
                currentBitmap = bmp
                pdfError = null
            } else {
                pdfError = "Error rendering page ${currentPage + 1}"
            }
            isLoading = false
        }
    }

    val formattedSize = remember(document.sizeBytes) {
        val mb = document.sizeBytes / (1024.0 * 1024.0)
        if (mb >= 1.0) "%.1f MB".format(mb) else "${document.sizeBytes / 1024} KB"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // PDF Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, bottom = 85.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && currentBitmap == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading PDF page...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else if (pdfError != null && currentBitmap == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "PDF Display Error",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pdfError ?: "Unknown error",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            openDocumentSafely(context, document, file, securityManager)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open with External App")
                    }
                }
            } else {
                currentBitmap?.let { bmp ->
                    val nightFilter = if (isNightMode) {
                        ColorFilter.colorMatrix(
                            ColorMatrix(
                                floatArrayOf(
                                    -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                                    0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                                    0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                                )
                            )
                        )
                    } else null

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1.2f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.4f
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    scale = newScale
                                    if (newScale > 1f) {
                                        offset = Offset(offset.x + pan.x, offset.y + pan.y)
                                    } else {
                                        offset = Offset.Zero
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPage + 1}",
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit,
                            colorFilter = nightFilter
                        )

                        if (isLoading) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(44.dp)
                                    .align(Alignment.Center)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // TOP BAR (Spaced down by 38dp from status bar so it never overlaps cameras/notches)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F172A).copy(alpha = 0.96f), Color(0xFF0F172A).copy(alpha = 0.7f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 38.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PDF",
                                color = Color(0xFFEF4444),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = document.originalName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (totalPages > 0) "Page ${currentPage + 1} of $totalPages • $formattedSize" else formattedSize,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Reset Zoom button when zoomed
                if (scale > 1.1f) {
                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOutMap,
                            contentDescription = "Reset Zoom",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }

                // Invert / Night Mode
                IconButton(
                    onClick = { isNightMode = !isNightMode },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Night Reading Mode",
                        tint = if (isNightMode) Color(0xFFFBBF24) else Color(0xFF94A3B8)
                    )
                }

                // Jump to page dialog
                if (totalPages > 1) {
                    IconButton(
                        onClick = { showJumpDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Jump to page",
                            tint = Color.White
                        )
                    }
                }

                // More Options Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unhide (Export to Downloads)", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF34D399)) },
                            onClick = {
                                showMenu = false
                                onUnhide()
                                onClose()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open in External Viewer", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                showMenu = false
                                openDocumentSafely(context, document, file, securityManager)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Document Info", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF94A3B8)) },
                            onClick = {
                                showMenu = false
                                onShowInfo()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Trash", color = Color(0xFFEF4444)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        // BOTTOM CONTROLS (Page Navigation Bar)
        if (totalPages > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Page",
                            tint = if (currentPage > 0) Color.White else Color(0xFF475569)
                        )
                    }

                    // Slider for fast navigation
                    Slider(
                        value = (currentPage + 1).toFloat(),
                        onValueChange = { newVal ->
                            currentPage = (newVal.toInt() - 1).coerceIn(0, totalPages - 1)
                        },
                        valueRange = 1f..totalPages.toFloat(),
                        steps = if (totalPages > 2) totalPages - 2 else 0,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    IconButton(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Page",
                            tint = if (currentPage < totalPages - 1) Color.White else Color(0xFF475569)
                        )
                    }

                    // Page Pill
                    Surface(
                        onClick = { showJumpDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "${currentPage + 1}/$totalPages",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Jump to Page Dialog
        if (showJumpDialog) {
            var jumpInput by remember { mutableStateOf((currentPage + 1).toString()) }
            AlertDialog(
                onDismissRequest = { showJumpDialog = false },
                containerColor = Color(0xFF1E293B),
                title = { Text("Jump to Page", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Enter page number (1 to $totalPages):", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = jumpInput,
                            onValueChange = { jumpInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val p = jumpInput.toIntOrNull()
                                if (p != null && p in 1..totalPages) {
                                    currentPage = p - 1
                                    showJumpDialog = false
                                }
                            }),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val p = jumpInput.toIntOrNull()
                            if (p != null && p in 1..totalPages) {
                                currentPage = p - 1
                                showJumpDialog = false
                            } else {
                                Toast.makeText(context, "Please enter a valid page (1-$totalPages)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Go", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJumpDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

// ==========================================
// 2. IN-APP TEXT / CODE / DATA VIEWER
// ==========================================
@Composable
fun InAppTextViewer(
    document: VaultMedia,
    file: File,
    securityManager: VaultSecurityManager?,
    onClose: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit
) {
    val context = LocalContext.current
    var textContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var fontSizeSp by remember { mutableFloatStateOf(14f) }
    var isWordWrap by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        isLoading = true
        textContent = readTextContent(file)
        isLoading = false
    }

    val ext = remember(document.originalName) { document.originalName.substringAfterLast('.', "").uppercase() }
    val formattedSize = remember(document.sizeBytes) {
        val mb = document.sizeBytes / (1024.0 * 1024.0)
        if (mb >= 1.0) "%.1f MB".format(mb) else "${document.sizeBytes / 1024} KB"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Text View Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 95.dp, bottom = 16.dp, start = 12.dp, end = 12.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF131B2E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()

                    val scrollModifier = if (isWordWrap) {
                        Modifier.verticalScroll(verticalScroll)
                    } else {
                        Modifier
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                    }

                    SelectionContainer {
                        Text(
                            text = textContent ?: "(No content)",
                            color = Color(0xFFE2E8F0),
                            fontSize = fontSizeSp.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = (fontSizeSp * 1.4f).sp,
                            modifier = scrollModifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        // TOP BAR (Spaced down by 38dp from status bar)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F172A).copy(alpha = 0.96f), Color(0xFF0F172A).copy(alpha = 0.7f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 38.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFA855F7).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (ext.isNotEmpty()) ext else "TXT",
                                color = Color(0xFFA855F7),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = document.originalName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$formattedSize • In-App Reader",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Word Wrap toggle
                IconButton(
                    onClick = { isWordWrap = !isWordWrap },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WrapText,
                        contentDescription = "Word Wrap",
                        tint = if (isWordWrap) Color(0xFF38BDF8) else Color(0xFF64748B)
                    )
                }

                // Font Size smaller
                IconButton(
                    onClick = { if (fontSizeSp > 10f) fontSizeSp -= 2f },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Smaller Font",
                        tint = Color(0xFF94A3B8)
                    )
                }

                // Font Size larger
                IconButton(
                    onClick = { if (fontSizeSp < 32f) fontSizeSp += 2f },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Larger Font",
                        tint = Color(0xFF94A3B8)
                    )
                }

                // Copy Text
                IconButton(
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Document Text", textContent ?: ""))
                        Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Text",
                        tint = Color.White
                    )
                }

                // Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unhide (Export to Downloads)", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF34D399)) },
                            onClick = {
                                showMenu = false
                                onUnhide()
                                onClose()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open in External Editor", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                showMenu = false
                                openDocumentSafely(context, document, file, securityManager)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Document Info", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF94A3B8)) },
                            onClick = {
                                showMenu = false
                                onShowInfo()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Trash", color = Color(0xFFEF4444)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. IN-APP OTHER DOCUMENT / BINARY VIEWER
// ==========================================
@Composable
fun InAppOtherDocumentViewer(
    document: VaultMedia,
    file: File,
    securityManager: VaultSecurityManager?,
    onClose: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit
) {
    val context = LocalContext.current
    val (icon, badgeColor, typeLabel) = getDocumentVisuals(document.originalName)

    val formattedSize = remember(document.sizeBytes) {
        val mb = document.sizeBytes / (1024.0 * 1024.0)
        if (mb >= 1.0) "%.1f MB".format(mb) else "${document.sizeBytes / 1024} KB"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // TOP BAR
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F172A).copy(alpha = 0.96f), Color(0xFF0F172A).copy(alpha = 0.7f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 38.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = document.originalName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$typeLabel File • $formattedSize",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        // CENTER CARD
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = document.originalName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$typeLabel  •  $formattedSize",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Button(
                onClick = {
                    openDocumentSafely(context, document, file, securityManager)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open with External Office App", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    onUnhide()
                    onClose()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unhide (Export to Phone Storage)", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onShowInfo,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("File Details")
                }

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Move to Trash")
                }
            }
        }
    }
}

// ==========================================
// 4. DOCUMENT INFO DIALOG
// ==========================================
@Composable
fun DocumentInfoDialog(
    document: VaultMedia,
    file: File,
    onDismiss: () -> Unit
) {
    val formattedDate = remember(document.dateAdded) {
        SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(document.dateAdded))
    }
    val formattedSize = remember(document.sizeBytes) {
        val mb = document.sizeBytes / (1024.0 * 1024.0)
        if (mb >= 1.0) "%.2f MB (${document.sizeBytes} bytes)".format(mb) else "${document.sizeBytes / 1024} KB (${document.sizeBytes} bytes)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Document Info", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(label = "File Name", value = document.originalName)
                InfoRow(label = "Format", value = document.originalName.substringAfterLast('.', "Unknown").uppercase())
                InfoRow(label = "File Size", value = formattedSize)
                InfoRow(label = "Vault Import Date", value = formattedDate)
                InfoRow(label = "Storage Status", value = "Encrypted in Private Vault")
            }
        },
        containerColor = Color(0xFF1E293B),
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ==========================================
// 5. HELPER FUNCTIONS (NATIVE PDF & TEXT)
// ==========================================
suspend fun getPdfPageCount(file: File): Int = withContext(Dispatchers.IO) {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    try {
        pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(pfd)
        renderer.pageCount
    } catch (e: Throwable) {
        e.printStackTrace()
        -1
    } finally {
        try { renderer?.close() } catch (_: Throwable) {}
        try { pfd?.close() } catch (_: Throwable) {}
    }
}

suspend fun renderPdfPage(file: File, pageIndex: Int, scaleFactor: Float = 2.0f): Bitmap? = withContext(Dispatchers.IO) {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    var page: PdfRenderer.Page? = null
    try {
        pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(pfd)
        if (pageIndex !in 0 until renderer.pageCount) return@withContext null
        page = renderer.openPage(pageIndex)
        val width = (page.width * scaleFactor).toInt().coerceIn(100, 4096)
        val height = (page.height * scaleFactor).toInt().coerceIn(100, 4096)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    } finally {
        try { page?.close() } catch (_: Throwable) {}
        try { renderer?.close() } catch (_: Throwable) {}
        try { pfd?.close() } catch (_: Throwable) {}
    }
}

suspend fun readTextContent(file: File, maxChars: Int = 300_000): String = withContext(Dispatchers.IO) {
    try {
        file.bufferedReader().use { reader ->
            val buffer = CharArray(maxChars)
            val read = reader.read(buffer, 0, maxChars)
            if (read > 0) {
                String(buffer, 0, read) + if (file.length() > maxChars) "\n\n[... File content truncated for performance ...]" else ""
            } else {
                "(Empty document)"
            }
        }
    } catch (e: Throwable) {
        "Error reading document: ${e.localizedMessage ?: "Unknown error"}"
    }
}

fun openDocumentSafely(
    context: Context,
    media: VaultMedia,
    file: File,
    securityManager: VaultSecurityManager?
) {
    try {
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val ext = media.originalName.substringAfterLast('.', "").lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        securityManager?.isExternalActivityActive = true
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, "No external app found for this file format.", Toast.LENGTH_SHORT).show()
    }
}
