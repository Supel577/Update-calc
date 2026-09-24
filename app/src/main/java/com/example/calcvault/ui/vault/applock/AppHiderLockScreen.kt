package com.example.calcvault.ui.vault.applock

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calcvault.data.applock.AppLockManager
import com.example.calcvault.data.applock.LockedAppInfo
import com.example.calcvault.data.security.VaultSecurityManager
import kotlinx.coroutines.launch

@Composable
fun AppHiderLockScreen(
    onBack: () -> Unit,
    securityManager: VaultSecurityManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appLockManager = remember { AppLockManager.getInstance(context) }
    val lockedPackages by appLockManager.lockedPackagesFlow.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var allApps by remember { mutableStateOf<List<LockedAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "locked", "unlocked", "social"

    var hasUsageAccess by remember { mutableStateOf(appLockManager.hasUsageStatsPermission()) }
    var hasOverlayAccess by remember { mutableStateOf(appLockManager.hasOverlayPermission()) }

    fun refreshAppsAndPermissions() {
        hasUsageAccess = appLockManager.hasUsageStatsPermission()
        hasOverlayAccess = appLockManager.hasOverlayPermission()
        coroutineScope.launch {
            if (allApps.isEmpty()) {
                isLoading = true
                allApps = appLockManager.getInstalledApps()
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAppsAndPermissions()
    }

    // Lifecycle observer: When user returns from granting permissions in Settings, refresh without kicking out to calculator!
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                securityManager.extendExternalActivityGracePeriod(300_000L)
                hasUsageAccess = appLockManager.hasUsageStatsPermission()
                hasOverlayAccess = appLockManager.hasOverlayPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val socialKeywords = remember {
        listOf("facebook", "messenger", "whatsapp", "instagram", "telegram", "tiktok", "twitter", "x", "snapchat", "viber", "imo", "discord", "signal", "youtube", "chrome")
    }

    val filteredApps = remember(allApps, searchQuery, selectedFilter, lockedPackages) {
        allApps.filter { app ->
            val matchesQuery = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "locked" -> lockedPackages.contains(app.packageName)
                "unlocked" -> !lockedPackages.contains(app.packageName)
                "social" -> socialKeywords.any { kw ->
                    app.appName.contains(kw, ignoreCase = true) || app.packageName.contains(kw, ignoreCase = true)
                }
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("app_lock_screen"),
        containerColor = Color(0xFF090D16),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B1120))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Lock Apps",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                                ) {
                                    Text(
                                        text = "${lockedPackages.size} Locked",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Protect any installed application with PIN shield",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps to lock...", color = Color(0xFF64748B), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF131B2E),
                        unfocusedContainerColor = Color(0xFF131B2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "all",
                            onClick = { selectedFilter = "all" },
                            label = { Text("All Apps (${allApps.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "locked",
                            onClick = { selectedFilter = "locked" },
                            label = { Text("Locked (${lockedPackages.size})") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "unlocked",
                            onClick = { selectedFilter = "unlocked" },
                            label = { Text("Unlocked (${(allApps.size - lockedPackages.size).coerceAtLeast(0)})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "social",
                            onClick = { selectedFilter = "social" },
                            label = { Text("Social & Messaging") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading installed applications...", color = Color(0xFF94A3B8))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // PERMISSION BANNER 1: Usage Access
                    if (!hasUsageAccess) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF78350F).copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Usage Access Required to Lock Apps",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Allows CalcVault to detect when a locked app is launched and show the PIN screen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFDE68A)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            securityManager.isExternalActivityActive = true
                                            securityManager.extendExternalActivityGracePeriod(600_000L)
                                            appLockManager.openUsageStatsSettings()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Grant Usage Access", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // PERMISSION BANNER 2: Overlay Permission
                    if (!hasOverlayAccess) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFF818CF8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Display Over Other Apps Required",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF818CF8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Allows CalcVault to display the PIN shield instantly over locked applications.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFC7D2FE)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            securityManager.isExternalActivityActive = true
                                            securityManager.extendExternalActivityGracePeriod(600_000L)
                                            appLockManager.openOverlaySettings()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Grant Overlay Permission", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    if (filteredApps.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFF475569)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("No matching applications found", color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    } else {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isLocked = lockedPackages.contains(app.packageName)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("app_item_${app.packageName}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLocked) Color(0xFF0369A1).copy(alpha = 0.18f) else Color(0xFF131B2E)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isLocked) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF1E293B)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // App Icon
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0F172A))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (app.icon != null) {
                                            Image(
                                                bitmap = app.icon.toBitmap(120, 120).asImageBitmap(),
                                                contentDescription = app.appName,
                                                modifier = Modifier.size(38.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        if (isLocked) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .background(Color(0xFF0284C7), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Locked",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    // App Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isLocked) "Locked with PIN shield" else app.packageName,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (isLocked) Color(0xFF38BDF8) else Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Single Clean Lock Toggle Switch
                                    Switch(
                                        checked = isLocked,
                                        onCheckedChange = { locked ->
                                            if (locked) {
                                                if (!hasUsageAccess) {
                                                    val msg = if (securityManager.appLanguage == "bn")
                                                        "⚠️ অ্যাপ লক চালু করতে Usage Access পারমিশন প্রয়োজন!"
                                                    else
                                                        "⚠️ Usage Access permission required to lock apps!"
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    securityManager.isExternalActivityActive = true
                                                    securityManager.extendExternalActivityGracePeriod(600_000L)
                                                    appLockManager.openUsageStatsSettings()
                                                    return@Switch
                                                }
                                                if (!hasOverlayAccess) {
                                                    val msg = if (securityManager.appLanguage == "bn")
                                                        "⚠️ অ্যাপের উপর স্ক্রিন দেখানোর জন্য Overlay পারমিশন প্রয়োজন!"
                                                    else
                                                        "⚠️ Display over other apps (Overlay) permission required!"
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    securityManager.isExternalActivityActive = true
                                                    securityManager.extendExternalActivityGracePeriod(600_000L)
                                                    appLockManager.openOverlaySettings()
                                                    return@Switch
                                                }
                                            }
                                            appLockManager.setPackageLocked(app.packageName, locked)
                                            val message = if (locked) {
                                                if (securityManager.appLanguage == "bn")
                                                    "${app.appName} পিন শিল্ড দিয়ে লক করা হয়েছে!"
                                                else
                                                    "${app.appName} locked behind PIN shield!"
                                            } else {
                                                if (securityManager.appLanguage == "bn")
                                                    "${app.appName} আনলক করা হয়েছে।"
                                                else
                                                    "${app.appName} unlocked."
                                            }
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF0284C7),
                                            uncheckedThumbColor = Color(0xFF94A3B8),
                                            uncheckedTrackColor = Color(0xFF334155)
                                        )
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
