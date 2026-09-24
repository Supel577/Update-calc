package com.example.calcvault.ui.vault.backup

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.calcvault.data.backup.GoogleDriveBackupService
import com.example.calcvault.data.backup.VaultBackupManager
import com.example.calcvault.data.backup.VaultBackupOptions
import com.example.calcvault.data.backup.VaultStats
import com.example.calcvault.data.security.VaultSecurityManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudBackupDialog(
    backupManager: VaultBackupManager,
    driveService: GoogleDriveBackupService?,
    securityManager: VaultSecurityManager,
    onRestoreComplete: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Backup, 1: Restore
    var isProcessing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var createdBackupFile by remember { mutableStateOf<File?>(null) }

    // Restore PIN prompt state
    var showRestorePinDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restorePinInput by remember { mutableStateOf("") }

    // Backup PIN customization (optional, defaults to current PIN)
    var backupPinInput by remember { mutableStateOf("") }

    // Category selection states
    var includePhotos by remember { mutableStateOf(true) }
    var includeVideos by remember { mutableStateOf(true) }
    var includeAudios by remember { mutableStateOf(true) }
    var includeDocs by remember { mutableStateOf(true) }
    var includeNotes by remember { mutableStateOf(true) }

    var vaultStats by remember { mutableStateOf<VaultStats?>(null) }
    LaunchedEffect(Unit) {
        vaultStats = backupManager.getVaultStats()
    }

    val currentBackupOptions = VaultBackupOptions(
        includePhotos = includePhotos,
        includeVideos = includeVideos,
        includeAudios = includeAudios,
        includeDocs = includeDocs,
        includeNotes = includeNotes
    )
    val hasSelectedItems = includePhotos || includeVideos || includeAudios || includeDocs || includeNotes
    val estimatedBytes = vaultStats?.calculateSelectedBytes(currentBackupOptions) ?: 0L

    // Launcher for selecting a .vault / .zip backup file (supports phone storage, SD card, and Google Drive)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            securityManager.isExternalActivityActive = false
            securityManager.extendExternalActivityGracePeriod(300_000L)
            pendingRestoreUri = uri
            restorePinInput = ""
            showRestorePinDialog = true
        } else {
            securityManager.isExternalActivityActive = false
            securityManager.extendExternalActivityGracePeriod(300_000L)
        }
    }

    // Launcher for saving directly to phone memory, SD Card, or Google Drive via SAF
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            securityManager.isExternalActivityActive = true
            securityManager.extendExternalActivityGracePeriod(600_000L)
            scope.launch {
                isProcessing = true
                statusMessage = null
                try {
                    val result = backupManager.writeEncryptedBackupToUri(
                        uri = uri,
                        pin = backupPinInput.trim(),
                        options = currentBackupOptions
                    ) { msg ->
                        progressStatus = msg
                    }
                    result.onSuccess { bytes ->
                        val mb = bytes / (1024 * 1024)
                        val kb = bytes / 1024
                        val sizeStr = if (mb > 0) "$mb MB" else "$kb KB"
                        statusMessage = Pair(true, "Backup saved successfully! ($sizeStr)")
                        Toast.makeText(context, "Backup successfully saved!", Toast.LENGTH_LONG).show()
                    }.onFailure { err ->
                        statusMessage = Pair(false, "Could not save backup: ${err.localizedMessage ?: "Storage write error"}")
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("CloudBackupDialog", "Backup write failed", t)
                    statusMessage = Pair(false, "Backup failed: ${t.localizedMessage ?: "Unexpected error"}")
                } finally {
                    isProcessing = false
                    securityManager.isExternalActivityActive = false
                    securityManager.extendExternalActivityGracePeriod(300_000L)
                }
            }
        } else {
            securityManager.isExternalActivityActive = false
            securityManager.extendExternalActivityGracePeriod(300_000L)
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Vault Backup & Restore",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "AES-256 Encrypted • Zero Cloud Dependency",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!isProcessing) onDismiss() },
                        enabled = !isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher (Backup / Restore)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF38BDF8),
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; statusMessage = null },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Backup", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; statusMessage = null },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Restore", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                AnimatedVisibility(visible = isProcessing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = progressStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Status Message Box
                statusMessage?.let { (success, msg) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (success) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFF7F1D1D).copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (success) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (success) Color(0xFF34D399) else Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }

                if (selectedTab == 0) {
                    // TAB 0: BACKUP
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "High-Security Encrypted Backup",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Packages all photos, videos, notes & docs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Your entire vault is packaged using AES-256 encryption. Large video files and thousands of photos are streamed in small chunks to prevent crashes or running out of memory.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Items selection section
                            Text(
                                text = "Select Items to Backup:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Photos
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = includePhotos,
                                            onCheckedChange = { includePhotos = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF38BDF8),
                                                uncheckedColor = Color(0xFF64748B),
                                                checkmarkColor = Color(0xFF0F172A)
                                            )
                                        )
                                        Text(
                                            text = "Photos (${vaultStats?.photoCount ?: 0})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = formatBackupSize(vaultStats?.photoBytes ?: 0L),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                // Videos
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = includeVideos,
                                            onCheckedChange = { includeVideos = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF38BDF8),
                                                uncheckedColor = Color(0xFF64748B),
                                                checkmarkColor = Color(0xFF0F172A)
                                            )
                                        )
                                        Column {
                                            Text(
                                                text = "Videos (${vaultStats?.videoCount ?: 0})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                            if ((vaultStats?.videoBytes ?: 0L) > 100 * 1024 * 1024L) {
                                                Text(
                                                    text = "Heavy files • uncheck to save space",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFFBBF24),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = formatBackupSize(vaultStats?.videoBytes ?: 0L),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if ((vaultStats?.videoBytes ?: 0L) > 500 * 1024 * 1024L) Color(0xFFFBBF24) else Color(0xFF94A3B8)
                                    )
                                }

                                // Notes
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = includeNotes,
                                            onCheckedChange = { includeNotes = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF38BDF8),
                                                uncheckedColor = Color(0xFF64748B),
                                                checkmarkColor = Color(0xFF0F172A)
                                            )
                                        )
                                        Text(
                                            text = "Secret Notes (${vaultStats?.noteCount ?: 0})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "< 1 MB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                if ((vaultStats?.audioCount ?: 0) > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = includeAudios,
                                                onCheckedChange = { includeAudios = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF38BDF8),
                                                    uncheckedColor = Color(0xFF64748B),
                                                    checkmarkColor = Color(0xFF0F172A)
                                                )
                                            )
                                            Text(
                                                text = "Audios (${vaultStats?.audioCount ?: 0})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = formatBackupSize(vaultStats?.audioBytes ?: 0L),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                if ((vaultStats?.docCount ?: 0) > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = includeDocs,
                                                onCheckedChange = { includeDocs = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF38BDF8),
                                                    uncheckedColor = Color(0xFF64748B),
                                                    checkmarkColor = Color(0xFF0F172A)
                                                )
                                            )
                                            Text(
                                                text = "Documents (${vaultStats?.docCount ?: 0})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = formatBackupSize(vaultStats?.docBytes ?: 0L),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Estimated size bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estimated Backup Size:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = formatBackupSize(estimatedBytes),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = backupPinInput,
                                onValueChange = { backupPinInput = it },
                                label = { Text("Custom Decryption Password (Optional)") },
                                placeholder = { Text("Leave blank to use default master key") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFF38BDF8),
                                    unfocusedLabelColor = Color(0xFF94A3B8)
                                )
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Action 1: Save to Phone Memory / SD Card
                            Button(
                                onClick = {
                                    val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    val fileName = "CalcVault_Backup_$dateStr.vault"
                                    securityManager.isExternalActivityActive = true
                                    securityManager.extendExternalActivityGracePeriod(600_000L)
                                    createDocumentLauncher.launch(fileName)
                                },
                                enabled = !isProcessing && hasSelectedItems,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_save_to_phone"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SdStorage,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Save Backup to Phone Memory",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action 2: Share / Upload to Google Drive
                            FilledTonalButton(
                                onClick = {
                                    securityManager.isExternalActivityActive = true
                                    securityManager.extendExternalActivityGracePeriod(600_000L)
                                    scope.launch {
                                        isProcessing = true
                                        statusMessage = null
                                        try {
                                            val result = backupManager.createEncryptedBackupVault(
                                                pin = backupPinInput.trim(),
                                                options = currentBackupOptions
                                            ) { msg ->
                                                progressStatus = msg
                                            }
                                            result.onSuccess { vaultFile ->
                                                createdBackupFile = vaultFile
                                                val shareIntent = backupManager.createShareIntent(vaultFile)
                                                val chooser = Intent.createChooser(shareIntent, "Share Backup to Google Drive / Files")
                                                context.startActivity(chooser)
                                                statusMessage = Pair(true, "Backup ready! Select 'Save to Drive' or choose your target app.")
                                            }.onFailure { err ->
                                                statusMessage = Pair(false, "Failed to prepare backup: ${err.localizedMessage ?: "Unknown error"}")
                                            }
                                        } catch (t: Throwable) {
                                            android.util.Log.e("CloudBackupDialog", "Share backup failed", t)
                                            statusMessage = Pair(false, "Error preparing backup: ${t.localizedMessage ?: "Out of memory or storage error"}")
                                        } finally {
                                            isProcessing = false
                                            securityManager.isExternalActivityActive = false
                                            securityManager.extendExternalActivityGracePeriod(300_000L)
                                        }
                                    }
                                },
                                enabled = !isProcessing && hasSelectedItems,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_share_to_drive"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF0369A1).copy(alpha = 0.3f),
                                    contentColor = Color(0xFF38BDF8)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Share / Upload to Google Drive",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: RESTORE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileMove,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Restore from Phone or Drive",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Import .vault or .zip backup archive",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Select any previously exported CalcVault file from your Phone Memory, Downloads folder, SD card, or Google Drive app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    // Open file selector allowing .vault, .zip, or all files
                                    securityManager.isExternalActivityActive = true
                                    securityManager.extendExternalActivityGracePeriod(600_000L)
                                    openDocumentLauncher.launch(
                                        arrayOf(
                                            "*/*",
                                            "application/octet-stream",
                                            "application/zip"
                                        )
                                    )
                                },
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_select_restore_file"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Select Backup File to Restore",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Restore PIN Decryption Dialog
    if (showRestorePinDialog && pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isProcessing) {
                    showRestorePinDialog = false
                    pendingRestoreUri = null
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8)
                    )
                    Text("Decryption Password", color = Color.White)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter the password used when this backup was created. If you did not enter a custom password, leave this blank.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = restorePinInput,
                        onValueChange = { restorePinInput = it },
                        label = { Text("Vault PIN / Password") },
                        placeholder = { Text("Leave blank if default") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingRestoreUri ?: return@Button
                        val pin = restorePinInput.trim()
                        showRestorePinDialog = false
                        pendingRestoreUri = null
                        selectedTab = 1 // Switch view immediately to Restore tab to show live progress

                        securityManager.isExternalActivityActive = true
                        securityManager.extendExternalActivityGracePeriod(600_000L)
                        scope.launch {
                            isProcessing = true
                            statusMessage = null
                            try {
                                val res = backupManager.restoreFromUri(
                                    uri = uri,
                                    pin = pin
                                ) { msg ->
                                    progressStatus = msg
                                }
                                res.onSuccess { count ->
                                    statusMessage = Pair(true, "Restoration complete! $count items restored into your vault.")
                                    Toast.makeText(context, "Restored $count items successfully!", Toast.LENGTH_LONG).show()
                                    onRestoreComplete()
                                }.onFailure { err ->
                                    statusMessage = Pair(false, "Restore failed: ${err.localizedMessage ?: "Incorrect PIN or corrupted file"}")
                                    Toast.makeText(context, "Restore failed: ${err.localizedMessage ?: "Check PIN/file"}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                                statusMessage = Pair(false, "Restore encountered an error: ${e.localizedMessage ?: "Unknown error"}")
                            } finally {
                                isProcessing = false
                                securityManager.isExternalActivityActive = false
                                securityManager.extendExternalActivityGracePeriod(300_000L)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Start Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestorePinDialog = false
                        pendingRestoreUri = null
                    }
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

private fun formatBackupSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}
