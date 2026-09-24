package com.example.calcvault.ui.vault

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calcvault.data.backup.GoogleDriveBackupService
import com.example.calcvault.data.backup.VaultBackupManager
import com.example.calcvault.data.camouflage.AppCamouflageManager
import com.example.calcvault.data.intruder.IntruderManager
import com.example.calcvault.data.repository.VaultRepository
import com.example.calcvault.data.security.VaultSecurityManager
import com.example.calcvault.data.storage.MediaStoreHelper
import com.example.calcvault.ui.vault.audios.AudiosScreen
import com.example.calcvault.ui.vault.browser.PrivateBrowserScreen
import com.example.calcvault.data.i18n.VaultStrings
import com.example.calcvault.ui.vault.guide.CategoryIntroOverlay
import com.example.calcvault.ui.vault.intruder.IntruderAlertsScreen
import com.example.calcvault.ui.vault.notes.NotesScreen
import com.example.calcvault.ui.vault.photos.PhotosScreen
import com.example.calcvault.ui.vault.settings.SecuritySettingsDialog
import com.example.calcvault.ui.vault.theme.ThemeSelectionDialog
import com.example.calcvault.ui.vault.theme.VaultThemes
import com.example.calcvault.ui.vault.trash.TrashBinScreen
import com.example.calcvault.ui.vault.videos.VideosScreen
import com.example.calcvault.data.ads.AdMobManager
import java.io.File

@Composable
fun VaultDashboardScreen(
    viewModel: VaultViewModel,
    securityManager: VaultSecurityManager,
    intruderManager: IntruderManager,
    camouflageManager: AppCamouflageManager,
    backupManager: VaultBackupManager,
    driveService: GoogleDriveBackupService,
    repository: VaultRepository,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier,
    adMobManager: AdMobManager? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoCount by viewModel.photoCount.collectAsStateWithLifecycle()
    val videoCount by viewModel.videoCount.collectAsStateWithLifecycle()
    val audioCount by viewModel.audioCount.collectAsStateWithLifecycle()
    val trashCount by viewModel.trashCount.collectAsStateWithLifecycle()
    val noteCount by viewModel.noteCount.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? Activity
    val navigateBackToDashboard: () -> Unit = {
        if (adMobManager != null) {
            adMobManager.onUserActionTrigger(activity) {
                viewModel.navigateTo(VaultScreenTab.DASHBOARD)
            }
        } else {
            viewModel.navigateTo(VaultScreenTab.DASHBOARD)
        }
    }

    val currentThemeId by securityManager.themeIdFlow.collectAsStateWithLifecycle()
    val customWallpaperPath by securityManager.customWallpaperPathFlow.collectAsStateWithLifecycle()
    val currentTheme = remember(currentThemeId) { VaultThemes.getThemeById(currentThemeId) }

    val appLockManager = remember { com.example.calcvault.data.applock.AppLockManager.getInstance(context) }
    val lockedPackages by appLockManager.lockedPackagesFlow.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showThemes by remember { mutableStateOf(false) }
    val appLanguage by securityManager.appLanguageFlow.collectAsStateWithLifecycle()
    var activeIntroTab by remember { mutableStateOf<VaultScreenTab?>(null) }

    val handleCategoryClick: (VaultScreenTab) -> Unit = { targetTab ->
        val key = when (targetTab) {
            VaultScreenTab.PHOTOS -> "photos"
            VaultScreenTab.VIDEOS -> "videos"
            VaultScreenTab.NOTES -> "notes"
            VaultScreenTab.DOCUMENTS -> "documents"
            VaultScreenTab.AUDIOS -> "audios"
            VaultScreenTab.BROWSER -> "browser"
            VaultScreenTab.APP_LOCK -> "applock"
            VaultScreenTab.TRASH -> "trash"
            VaultScreenTab.SECRET_CAMERA -> "camera"
            VaultScreenTab.INTRUDER_ALERTS -> "intruder"
            else -> ""
        }
        if (key.isNotEmpty() && !securityManager.isFeatureGuideShown(key)) {
            securityManager.setFeatureGuideShown(key, true)
            activeIntroTab = targetTab
        } else {
            viewModel.navigateTo(targetTab)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val deleteIntentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onOriginalsDeleted(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMediaItems(uris, isVideo = false)
        }
    }

    AnimatedContent(
        targetState = uiState.currentTab,
        transitionSpec = {
            if (targetState == VaultScreenTab.DASHBOARD) {
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "VaultScreenTransition"
    ) { tab ->
        when (tab) {
            VaultScreenTab.PHOTOS -> {
                PhotosScreen(
                    viewModel = viewModel,
                    securityManager = securityManager,
                    onBack = navigateBackToDashboard
                )
            }
            VaultScreenTab.VIDEOS -> {
                VideosScreen(
                    viewModel = viewModel,
                    securityManager = securityManager,
                    onBack = navigateBackToDashboard
                )
            }
            VaultScreenTab.AUDIOS -> {
                AudiosScreen(
                    viewModel = viewModel,
                    securityManager = securityManager,
                    onBack = navigateBackToDashboard
                )
            }
            VaultScreenTab.TRASH -> {
                TrashBinScreen(
                    viewModel = viewModel,
                    onBack = navigateBackToDashboard
                )
            }
            VaultScreenTab.NOTES -> {
                NotesScreen(
                    viewModel = viewModel,
                    onBack = navigateBackToDashboard
                )
            }
            VaultScreenTab.BROWSER -> {
                PrivateBrowserScreen(
                    repository = repository,
                    onNavigateBack = navigateBackToDashboard
                )
            }
            VaultScreenTab.INTRUDER_ALERTS -> {
                IntruderAlertsScreen(
                    intruderManager = intruderManager,
                    onNavigateBack = navigateBackToDashboard
                )
            }
            VaultScreenTab.DOCUMENTS -> {
                com.example.calcvault.ui.vault.documents.DocumentsScreen(
                    viewModel = viewModel,
                    securityManager = securityManager,
                    onBack = navigateBackToDashboard
                )
            }
            VaultScreenTab.SECRET_CAMERA -> {
                com.example.calcvault.ui.vault.camera.SecretCameraScreen(
                    viewModel = viewModel,
                    onBack = navigateBackToDashboard,
                    onNavigateToPhotos = { viewModel.navigateTo(VaultScreenTab.PHOTOS) }
                )
            }
            VaultScreenTab.APP_LOCK -> {
                com.example.calcvault.ui.vault.applock.AppHiderLockScreen(
                    onBack = navigateBackToDashboard,
                    securityManager = securityManager
                )
            }
            VaultScreenTab.DASHBOARD, VaultScreenTab.SETTINGS -> {
                Box(modifier = modifier.fillMaxSize()) {
                    // Wallpaper Layer - Instant preset loading & async gallery loading
                    val preset = remember(customWallpaperPath) {
                        VaultThemes.getWallpaperPreset(customWallpaperPath)
                    }
                    if (preset != null) {
                        com.example.calcvault.ui.vault.theme.CodeBasedWallpaper(
                            renderType = preset.renderType,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Sophisticated glassmorphism dark scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.45f),
                                            Color.Black.copy(alpha = 0.65f),
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                    } else if (customWallpaperPath != null) {
                        val wpFile = remember(customWallpaperPath) { File(customWallpaperPath!!) }
                        if (wpFile.exists()) {
                            AsyncImage(
                                model = wpFile,
                                contentDescription = "Active Wallpaper",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Sophisticated glassmorphism dark scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.55f),
                                                Color.Black.copy(alpha = 0.75f),
                                                Color.Black.copy(alpha = 0.90f)
                                            )
                                        )
                                    )
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(currentTheme.background))
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(currentTheme.background))
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .testTag("vault_dashboard_screen"),
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = currentTheme.primaryAccent.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            currentTheme.primaryAccent.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = currentTheme.primaryAccent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = VaultStrings.get(appLanguage, "vault_title"),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color(0xFF10B981), CircleShape)
                                            )
                                            Text(
                                                text = VaultStrings.get(appLanguage, "vault_subtitle"),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = Color(0xFF10B981),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {


                                    // Themes & 10 Wallpaper Selector Button
                                    Surface(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true),
                                                onClick = { showThemes = true }
                                            )
                                            .testTag("btn_vault_themes"),
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = "Themes & Wallpaper",
                                                tint = Color.White,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }

                                    // Settings Button
                                    Surface(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true),
                                                onClick = { showSettings = true }
                                            )
                                            .testTag("btn_vault_settings"),
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                tint = Color.White,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }

                                    // Lock Now Button
                                    Surface(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(19.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true),
                                                onClick = onLockNow
                                            )
                                            .testTag("btn_lock_now"),
                                        shape = RoundedCornerShape(19.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.22f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp)
                                                .fillMaxHeight(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Lock",
                                                tint = Color(0xFFFCA5A5),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Lock",
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFCA5A5),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        val intruderAlerts = remember(tab) { intruderManager.getIntruderAlerts() }
                        val alertCount = intruderAlerts.size

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {


                            // High-End Storage / Status Hero Card spanning full width
                            item(span = { GridItemSpan(2) }) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF1E293B).copy(alpha = 0.85f),
                                                        Color(0xFF0F172A).copy(alpha = 0.95f)
                                                    )
                                                )
                                            )
                                            .padding(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Encrypted Sandbox Active",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${uiState.totalVaultSize} protected in private isolated storage",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = currentTheme.primaryAccent.copy(alpha = 0.2f),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    currentTheme.primaryAccent.copy(alpha = 0.4f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Storage,
                                                        contentDescription = null,
                                                        tint = currentTheme.primaryAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = uiState.totalVaultSize,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = currentTheme.primaryAccent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 1. Photos
                            item {
                                ModernVaultGridCard(
                                    title = VaultStrings.get(appLanguage, "cat_photos"),
                                    count = "$photoCount files",
                                    iconDrawable = com.example.R.drawable.img_vault_photos_icon_1790257695579,
                                    fallbackIcon = Icons.Default.PhotoLibrary,
                                    gradient = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
                                    badgeColor = Color(0xFF38BDF8),
                                    testTag = "category_photos",
                                    onClick = { handleCategoryClick(VaultScreenTab.PHOTOS) }
                                )
                            }

                            // 2. Videos
                            item {
                                ModernVaultGridCard(
                                    title = VaultStrings.get(appLanguage, "cat_videos"),
                                    count = "$videoCount files",
                                    iconDrawable = com.example.R.drawable.img_vault_videos_icon_1790257706862,
                                    fallbackIcon = Icons.Default.VideoLibrary,
                                    gradient = listOf(Color(0xFFD97706), Color(0xFFB45309)),
                                    badgeColor = Color(0xFFFBBF24),
                                    testTag = "category_videos",
                                    onClick = { handleCategoryClick(VaultScreenTab.VIDEOS) }
                                )
                            }

                            // 3. Notes
                            item {
                                ModernVaultGridCard(
                                    title = VaultStrings.get(appLanguage, "cat_notes"),
                                    count = "$noteCount notes",
                                    iconDrawable = com.example.R.drawable.ic_app_notes_real_1789885876446,
                                    fallbackIcon = Icons.Default.Description,
                                    gradient = listOf(Color(0xFF059669), Color(0xFF047857)),
                                    badgeColor = Color(0xFF34D399),
                                    testTag = "category_notes",
                                    onClick = { handleCategoryClick(VaultScreenTab.NOTES) }
                                )
                            }

                            // 4. Audios
                            item {
                                ModernVaultGridCard(
                                    title = VaultStrings.get(appLanguage, "cat_audios"),
                                    count = "$audioCount audios",
                                    iconDrawable = com.example.R.drawable.ic_disguise_music_1789899183733,
                                    fallbackIcon = Icons.Default.AudioFile,
                                    gradient = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
                                    badgeColor = Color(0xFFA78BFA),
                                    testTag = "category_audios",
                                    onClick = { handleCategoryClick(VaultScreenTab.AUDIOS) }
                                )
                            }

                            // 5. Private Browser
                            item {
                                ModernVaultGridCard(
                                    title = VaultStrings.get(appLanguage, "cat_browser"),
                                    count = "Stealth web",
                                    iconDrawable = com.example.R.drawable.img_vault_real_browser_1790259063071,
                                    fallbackIcon = Icons.Default.Public,
                                    gradient = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                                    badgeColor = Color(0xFF60A5FA),
                                    testTag = "category_browser",
                                    onClick = { handleCategoryClick(VaultScreenTab.BROWSER) }
                                )
                            }

                            // 6. Intruder Alerts
                            item {
                                ModernVaultGridCard(
                                    title = "Break-in Alerts",
                                    count = if (alertCount == 0) "Arm active" else "$alertCount caught",
                                    iconDrawable = com.example.R.drawable.img_vault_intruder_icon_1790257731760,
                                    fallbackIcon = Icons.Default.Security,
                                    gradient = if (alertCount > 0) listOf(Color(0xFFDC2626), Color(0xFF991B1B)) else listOf(Color(0xFF0F766E), Color(0xFF115E59)),
                                    badgeColor = if (alertCount > 0) Color(0xFFF87171) else Color(0xFF2DD4BF),
                                    testTag = "category_intruder_alerts",
                                    onClick = { handleCategoryClick(VaultScreenTab.INTRUDER_ALERTS) }
                                )
                            }

                            // 7. Documents & Files
                            item {
                                ModernVaultGridCard(
                                    title = VaultStrings.get(appLanguage, "cat_documents"),
                                    count = "PDF, Office & Zip",
                                    iconDrawable = com.example.R.drawable.ic_disguise_files_1789899243046,
                                    fallbackIcon = Icons.Default.FolderZip,
                                    gradient = listOf(Color(0xFF0D9488), Color(0xFF0F766E)),
                                    badgeColor = Color(0xFF2DD4BF),
                                    testTag = "category_documents",
                                    onClick = { handleCategoryClick(VaultScreenTab.DOCUMENTS) }
                                )
                            }

                            // 8. Secret Camera
                            item {
                                ModernVaultGridCard(
                                    title = "Secret Cam",
                                    count = "Direct to vault",
                                    iconDrawable = com.example.R.drawable.img_vault_camera_icon_1790257719221,
                                    fallbackIcon = Icons.Default.PhotoCamera,
                                    gradient = listOf(Color(0xFFE11D48), Color(0xFFBE123C)),
                                    badgeColor = Color(0xFFFB7185),
                                    testTag = "category_secret_cam",
                                    onClick = { handleCategoryClick(VaultScreenTab.SECRET_CAMERA) }
                                )
                            }

                            // 9. Hide & Lock Apps (Facebook, Messenger, WhatsApp, etc.)
                            item(span = { GridItemSpan(2) }) {
                                val lockedCount = lockedPackages.size
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true),
                                            onClick = { handleCategoryClick(VaultScreenTab.APP_LOCK) }
                                        )
                                        .testTag("category_app_lock"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color(0xFF6366F1).copy(alpha = 0.4f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF312E81).copy(alpha = 0.85f),
                                                        Color(0xFF1E1B4B).copy(alpha = 0.95f)
                                                    )
                                                )
                                            )
                                            .padding(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(44.dp),
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = Color(0xFF4F46E5).copy(alpha = 0.3f),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        Color(0xFF818CF8).copy(alpha = 0.5f)
                                                    )
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Shield,
                                                            contentDescription = null,
                                                            tint = Color(0xFFA5B4FC),
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = VaultStrings.get(appLanguage, "cat_applock"),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = if (lockedCount == 0) "Lock Facebook, Messenger & any app" else "$lockedCount apps protected by PIN shield",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFFC7D2FE)
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF4F46E5).copy(alpha = 0.3f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = if (lockedCount == 0) "Set Up" else "$lockedCount Locked",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFA5B4FC),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 9. Trash / Recycle Bin (Full Width horizontal card)
                            item(span = { GridItemSpan(2) }) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true),
                                            onClick = { handleCategoryClick(VaultScreenTab.TRASH) }
                                        )
                                        .testTag("category_trash"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color(0xFFEF4444).copy(alpha = 0.35f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF450A0A).copy(alpha = 0.85f),
                                                        Color(0xFF1F1212).copy(alpha = 0.95f)
                                                    )
                                                )
                                            )
                                            .padding(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(48.dp),
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = Color(0xFFEF4444).copy(alpha = 0.25f),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteSweep,
                                                            contentDescription = null,
                                                            tint = Color(0xFFF87171),
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = VaultStrings.get(appLanguage, "cat_trash"),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "$trashCount items awaiting permanent deletion or restore",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFFFCA5A5)
                                                    )
                                                }
                                            }

                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = Color(0xFFF87171),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Spacer at bottom
                            item(span = { GridItemSpan(2) }) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SecuritySettingsDialog(
            securityManager = securityManager,
            viewModel = viewModel,
            camouflageManager = camouflageManager,
            backupManager = backupManager,
            driveService = driveService,
            onDismiss = { showSettings = false }
        )
    }

    if (showThemes) {
        ThemeSelectionDialog(
            securityManager = securityManager,
            camouflageManager = camouflageManager,
            onDismiss = { showThemes = false }
        )
    }

    if (uiState.showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = { Text("Software Update") },
            text = { Text("You are currently running CalcVault v2.5.0. No newer updates are available. Your vault is fully up to date and secure.") },
            confirmButton = {
                Button(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text("OK")
                }
            }
        )
    }

    DeleteOriginalDialog(
        prompt = uiState.deleteOriginalPrompt,
        appLanguage = securityManager.appLanguage,
        onConfirm = { prompt ->
            viewModel.dismissDeleteOriginalPrompt()
            if (prompt.isDocument) {
                var deletedCount = 0
                for (u in prompt.uris) {
                    try {
                        if (android.provider.DocumentsContract.deleteDocument(context.contentResolver, u)) {
                            deletedCount++
                        } else {
                            val rows = context.contentResolver.delete(u, null, null)
                            if (rows > 0) deletedCount++
                        }
                    } catch (_: Exception) {
                        try {
                            val rows = context.contentResolver.delete(u, null, null)
                            if (rows > 0) deletedCount++
                        } catch (_: Exception) {}
                    }
                }
                viewModel.onOriginalsDeleted(deletedCount > 0)
            } else {
                MediaStoreHelper.requestDelete(
                    context = context,
                    uris = prompt.uris,
                    isVideo = prompt.isVideo,
                    onLaunchIntentSender = { senderRequest ->
                        deleteIntentSenderLauncher.launch(senderRequest)
                    },
                    onDirectResult = { success ->
                        viewModel.onOriginalsDeleted(success)
                    }
                )
            }
        },
        onDismiss = {
            viewModel.dismissDeleteOriginalPrompt()
        }
    )

    activeIntroTab?.let { introTab ->
        val key = when (introTab) {
            VaultScreenTab.PHOTOS -> "photos"
            VaultScreenTab.VIDEOS -> "videos"
            VaultScreenTab.NOTES -> "notes"
            VaultScreenTab.DOCUMENTS -> "documents"
            VaultScreenTab.AUDIOS -> "audios"
            VaultScreenTab.BROWSER -> "browser"
            VaultScreenTab.APP_LOCK -> "applock"
            VaultScreenTab.TRASH -> "trash"
            VaultScreenTab.SECRET_CAMERA -> "camera"
            VaultScreenTab.INTRUDER_ALERTS -> "intruder"
            else -> ""
        }
        CategoryIntroOverlay(
            tab = introTab,
            appLanguage = appLanguage,
            onDismissAndProceed = {
                if (key.isNotEmpty()) {
                    securityManager.setFeatureGuideShown(key, true)
                }
                activeIntroTab = null
                viewModel.navigateTo(introTab)
            }
        )
    }
}

@Composable
fun ModernVaultGridCard(
    title: String,
    count: String,
    iconDrawable: Int? = null,
    fallbackIcon: ImageVector,
    gradient: List<Color>,
    badgeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = badgeColor.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E293B).copy(alpha = 0.82f),
                            Color(0xFF0F172A).copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = gradient.first().copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, gradient.first().copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (iconDrawable != null && iconDrawable != com.example.R.drawable.ic_launcher_foreground) {
                                Image(
                                    painter = painterResource(id = iconDrawable),
                                    contentDescription = title,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = fallbackIcon,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = count,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Encrypted Vault",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
