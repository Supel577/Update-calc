package com.example.calcvault.ui.vault.documents

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calcvault.data.model.VaultMedia
import com.example.calcvault.data.security.VaultSecurityManager
import com.example.calcvault.ui.vault.VaultViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentsScreen(
    viewModel: VaultViewModel,
    securityManager: VaultSecurityManager? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var viewingDocument by remember { mutableStateOf<VaultMedia?>(null) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        securityManager?.isExternalActivityActive = false
        if (uris.isNotEmpty()) {
            viewModel.importDocuments(uris)
        }
    }

    val launchPicker = {
        securityManager?.isExternalActivityActive = true
        // Allow all documents, archives, pdf, office, apks, etc.
        documentPickerLauncher.launch(arrayOf("*/*"))
    }

    val filteredDocs = remember(documents, searchQuery, selectedFilter) {
        documents.filter { doc ->
            val ext = doc.originalName.substringAfterLast('.', "").lowercase()
            val matchesFilter = when (selectedFilter) {
                "PDF" -> ext == "pdf"
                "Word" -> ext in listOf("doc", "docx", "rtf", "odt")
                "Excel" -> ext in listOf("xls", "xlsx", "csv")
                "Zip" -> ext in listOf("zip", "rar", "7z", "tar", "gz")
                "Text" -> ext in listOf("txt", "md", "json", "xml", "html")
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() || doc.originalName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("documents_screen"),
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
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                            Text(
                                text = "${uiState.selectedMediaIds.size} Selected",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row {
                            IconButton(onClick = {
                                viewModel.selectAll(filteredDocs)
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = Color(0xFF38BDF8))
                            }

                            IconButton(onClick = {
                                val selectedList = documents.filter { uiState.selectedMediaIds.contains(it.id) }
                                viewModel.batchUnhide(selectedList)
                            }) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Unhide", tint = Color(0xFF34D399))
                            }

                            IconButton(onClick = {
                                val selectedList = documents.filter { uiState.selectedMediaIds.contains(it.id) }
                                viewModel.batchMoveToTrash(selectedList)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E293B),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Documents & Files",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${documents.size} encrypted files",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            placeholder = { Text("Search document name...", fontSize = 14.sp, color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter Chips
                        val filters = listOf("All", "PDF", "Word", "Excel", "Zip", "Text")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(filters) { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFF94A3B8)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedFilter == filter,
                                        borderColor = if (selectedFilter == filter) MaterialTheme.colorScheme.primary else Color(0xFF334155)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isMultiSelectMode) {
                FloatingActionButton(
                    onClick = launchPicker,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("import_document_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "Hide Documents",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (filteredDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedFilter != "All") "No matching documents" else "No Hidden Documents Yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Secure your PDFs, Word documents, Excel sheets, ZIP archives, and any other files safely inside the vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredDocs, key = { it.id }) { doc ->
                    val isSelected = uiState.selectedMediaIds.contains(doc.id)
                    DocumentListItem(
                        document = doc,
                        isSelected = isSelected,
                        isMultiSelectMode = uiState.isMultiSelectMode,
                        onItemClick = {
                            if (uiState.isMultiSelectMode) {
                                viewModel.toggleSelection(doc.id)
                            } else {
                                viewingDocument = doc
                            }
                        },
                        onItemLongClick = {
                            viewModel.toggleSelection(doc.id)
                        },
                        onUnhide = {
                            viewModel.unhideMedia(doc)
                        },
                        onDelete = {
                            viewModel.moveToTrash(doc)
                        }
                    )
                }
            }
        }
    }

    // In-App PDF & Document Reader Dialog
    if (viewingDocument != null) {
        val selectedDoc = viewingDocument!!
        val docFile = viewModel.getFile(selectedDoc)
        InAppDocumentReaderDialog(
            document = selectedDoc,
            file = docFile,
            securityManager = securityManager,
            onClose = { viewingDocument = null },
            onUnhide = {
                viewModel.unhideMedia(selectedDoc)
                viewingDocument = null
            },
            onDelete = {
                viewModel.moveToTrash(selectedDoc)
                viewingDocument = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentListItem(
    document: VaultMedia,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val (icon, badgeColor, typeLabel) = getDocumentVisuals(document.originalName)

    val formattedSize = remember(document.sizeBytes) {
        val mb = document.sizeBytes / (1024.0 * 1024.0)
        if (mb >= 1.0) "%.1f MB".format(mb) else "${document.sizeBytes / 1024} KB"
    }

    val formattedDate = remember(document.dateAdded) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(document.dateAdded))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF334155),
                shape = RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B).copy(alpha = 0.95f) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = typeLabel,
                        tint = badgeColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.originalName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$formattedSize • $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Options menu (if not multi-selecting)
            if (!isMultiSelectMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open / View", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                showMenu = false
                                onItemClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Unhide (Export to Downloads)", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF34D399)) },
                            onClick = {
                                showMenu = false
                                onUnhide()
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

fun getDocumentVisuals(fileName: String): Triple<ImageVector, Color, String> {
    val ext = fileName.substringAfterLast('.', "").uppercase()
    return when (ext) {
        "PDF" -> Triple(Icons.Default.PictureAsPdf, Color(0xFFEF4444), "PDF")
        "DOC", "DOCX" -> Triple(Icons.Default.Article, Color(0xFF3B82F6), "WORD")
        "XLS", "XLSX", "CSV" -> Triple(Icons.Default.Article, Color(0xFF10B981), "EXCEL")
        "PPT", "PPTX" -> Triple(Icons.Default.Article, Color(0xFFF97316), "PPT")
        "ZIP", "RAR", "7Z", "TAR", "GZ" -> Triple(Icons.Default.FolderZip, Color(0xFFF59E0B), "ARCHIVE")
        "TXT", "MD", "JSON", "XML" -> Triple(Icons.Default.Description, Color(0xFFA855F7), "TEXT")
        "APK" -> Triple(Icons.Default.Archive, Color(0xFF06B6D4), "APK")
        else -> Triple(Icons.Default.Description, Color(0xFF64748B), if (ext.isNotEmpty()) ext else "FILE")
    }
}

fun openDocumentSafely(
    context: Context,
    media: VaultMedia,
    viewModel: VaultViewModel,
    securityManager: VaultSecurityManager?
) {
    try {
        val file = viewModel.repository.fileManager.getFile(media)
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
        Toast.makeText(context, "No app found to open this file type.", Toast.LENGTH_SHORT).show()
    }
}
