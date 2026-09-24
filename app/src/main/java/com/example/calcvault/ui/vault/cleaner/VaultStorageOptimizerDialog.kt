package com.example.calcvault.ui.vault.cleaner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calcvault.data.model.VaultMedia
import com.example.calcvault.data.model.VaultMediaType
import com.example.calcvault.ui.vault.VaultViewModel
import java.util.Locale

@Composable
fun VaultStorageOptimizerDialog(
    viewModel: VaultViewModel,
    appLanguage: String,
    onDismiss: () -> Unit
) {
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val audios by viewModel.audios.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val trashItems by viewModel.trash.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedDuplicateIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedLargeFileIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val allActiveMedia = remember(photos, videos, audios, documents) {
        photos + videos + audios + documents
    }

    // Group duplicates by exact sizeBytes
    val duplicateGroups = remember(allActiveMedia) {
        allActiveMedia
            .filter { it.sizeBytes > 1024 } // larger than 1KB
            .groupBy { it.sizeBytes }
            .filter { it.value.size > 1 }
    }

    val duplicateCandidates = remember(duplicateGroups) {
        duplicateGroups.flatMap { entry ->
            // Keep the first one, mark remaining as duplicates
            entry.value.drop(1)
        }
    }

    // Large files: size >= 5MB, sorted descending
    val largeFiles = remember(allActiveMedia) {
        allActiveMedia
            .filter { it.sizeBytes >= 5 * 1024 * 1024 }
            .sortedByDescending { it.sizeBytes }
    }

    val trashTotalBytes by remember(trashItems) {
        derivedStateOf { trashItems.sumOf { it.sizeBytes } }
    }

    fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }

    val tabs = listOf(
        if (appLanguage == "bn") "ডুপ্লিকেট ফাইল (${duplicateCandidates.size})" else "Duplicates (${duplicateCandidates.size})",
        if (appLanguage == "bn") "বড় ফাইলসমূহ (${largeFiles.size})" else "Large Files (${largeFiles.size})",
        if (appLanguage == "bn") "রিসাইকেল বিন (${trashItems.size})" else "Trash Bin (${trashItems.size})"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(max = 680.dp),
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color(0xFF94A3B8),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                    }
                    Column {
                        Text(
                            text = if (appLanguage == "bn") "স্টোরেজ ক্লিনার ও অপ্টিমাইজার" else "Vault Storage Cleaner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (appLanguage == "bn") "মেমোরি খালি করুন ও ডুপ্লিকেট সাফ করুন" else "Free up space & optimize storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Overview Storage Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (appLanguage == "bn") "মোট ভল্ট সাইজ" else "Total Vault Storage",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            val totalBytes = allActiveMedia.sumOf { it.sizeBytes } + trashTotalBytes
                            Text(
                                text = formatBytes(totalBytes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }

                        val reclaimable = duplicateCandidates.sumOf { it.sizeBytes } + trashTotalBytes
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (appLanguage == "bn") "সম্ভাব্য খালিযোগ্য" else "Reclaimable Space",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = formatBytes(reclaimable),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                feedbackMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF38BDF8),
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF38BDF8)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) Color.White else Color(0xFF64748B)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> { // Duplicates Tab
                        if (duplicateCandidates.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (appLanguage == "bn") "কোনো ডুপ্লিকেট ফাইল নেই!" else "No duplicate files found!",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (appLanguage == "bn") "আপনার ভল্ট স্টোরেজ সম্পূর্ণ অপ্টিমাইজড।" else "Your vault storage is perfectly optimized.",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        selectedDuplicateIds = duplicateCandidates.map { it.id }.toSet()
                                    }
                                ) {
                                    Text(if (appLanguage == "bn") "সব ডুপ্লিকেট নির্বাচন" else "Select All Duplicates", fontSize = 12.sp)
                                }

                                if (selectedDuplicateIds.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            val itemsToDelete = duplicateCandidates.filter { selectedDuplicateIds.contains(it.id) }
                                            itemsToDelete.forEach { viewModel.moveToTrash(it) }
                                            val freedSize = itemsToDelete.sumOf { it.sizeBytes }
                                            feedbackMessage = if (appLanguage == "bn") "${itemsToDelete.size}টি ডুপ্লিকেট রিসাইকেল বিনে পাঠানো হয়েছে (${formatBytes(freedSize)})" else "Moved ${itemsToDelete.size} duplicates to trash (${formatBytes(freedSize)})"
                                            selectedDuplicateIds = emptySet()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (appLanguage == "bn") "সাফ করুন (${selectedDuplicateIds.size})" else "Clean (${selectedDuplicateIds.size})", fontSize = 12.sp)
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(duplicateCandidates) { item ->
                                    val isSelected = selectedDuplicateIds.contains(item.id)
                                    StorageItemRow(
                                        item = item,
                                        sizeFormatted = formatBytes(item.sizeBytes),
                                        isSelected = isSelected,
                                        onToggle = {
                                            selectedDuplicateIds = if (isSelected) selectedDuplicateIds - item.id else selectedDuplicateIds + item.id
                                        }
                                    )
                                }
                            }
                        }
                    }

                    1 -> { // Large Files Tab
                        if (largeFiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (appLanguage == "bn") "৫ মেগাবাইটের চেয়ে বড় কোনো ফাইল নেই" else "No large files over 5 MB found",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (appLanguage == "bn") "ভারী ফাইল নির্বাচন করুন:" else "Select large files to delete:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )

                                if (selectedLargeFileIds.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            val itemsToDelete = largeFiles.filter { selectedLargeFileIds.contains(it.id) }
                                            itemsToDelete.forEach { viewModel.moveToTrash(it) }
                                            val freedSize = itemsToDelete.sumOf { it.sizeBytes }
                                            feedbackMessage = if (appLanguage == "bn") "${itemsToDelete.size}টি বড় ফাইল ট্র্যাশে পাঠানো হয়েছে (${formatBytes(freedSize)})" else "Moved ${itemsToDelete.size} large files to trash (${formatBytes(freedSize)})"
                                            selectedLargeFileIds = emptySet()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (appLanguage == "bn") "মুছুন (${selectedLargeFileIds.size})" else "Delete (${selectedLargeFileIds.size})", fontSize = 12.sp)
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(largeFiles) { item ->
                                    val isSelected = selectedLargeFileIds.contains(item.id)
                                    StorageItemRow(
                                        item = item,
                                        sizeFormatted = formatBytes(item.sizeBytes),
                                        isSelected = isSelected,
                                        onToggle = {
                                            selectedLargeFileIds = if (isSelected) selectedLargeFileIds - item.id else selectedLargeFileIds + item.id
                                        }
                                    )
                                }
                            }
                        }
                    }

                    2 -> { // Trash Bin Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = if (trashItems.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF64748B),
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (trashItems.isEmpty())
                                    (if (appLanguage == "bn") "রিসাইকেল বিন সম্পূর্ণ খালি" else "Trash bin is currently empty")
                                else
                                    (if (appLanguage == "bn") "${trashItems.size}টি ফাইল ট্র্যাশে জমা রয়েছে (${formatBytes(trashTotalBytes)})" else "${trashItems.size} items in trash occupying ${formatBytes(trashTotalBytes)}"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (trashItems.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        viewModel.emptyTrash()
                                        feedbackMessage = if (appLanguage == "bn") "রিসাইকেল বিন খালি করা হয়েছে! ${formatBytes(trashTotalBytes)} মেমোরি মুক্ত হয়েছে।" else "Trash emptied! Permanently reclaimed ${formatBytes(trashTotalBytes)}."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (appLanguage == "bn") "রিসাইকেল বিন সম্পূর্ণ খালি করুন" else "Permanently Empty Trash",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (appLanguage == "bn") "সম্পন্ন" else "Done", color = Color(0xFF38BDF8))
            }
        }
    )
}

@Composable
private fun StorageItemRow(
    item: VaultMedia,
    sizeFormatted: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF38BDF8),
                        uncheckedColor = Color(0xFF64748B)
                    )
                )

                Icon(
                    imageVector = when (item.mediaType) {
                        VaultMediaType.PHOTO -> Icons.Default.Image
                        VaultMediaType.VIDEO -> Icons.Default.VideoFile
                        VaultMediaType.AUDIO -> Icons.Default.MusicNote
                        VaultMediaType.DOCUMENT -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = item.originalName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sizeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
