package com.example.calcvault.ui.vault.guide

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.calcvault.data.i18n.VaultStrings
import com.example.calcvault.ui.vault.VaultScreenTab

data class CategoryIntroInfo(
    val titleKey: String,
    val subtitleKey: String,
    val accentColor: Color,
    val heroIcon: ImageVector,
    val steps: List<IntroStepInfo>,
    val heroDrawable: Int? = null
)

data class IntroStepInfo(
    val titleKey: String,
    val descKey: String,
    val icon: ImageVector
)

fun getIntroInfoForTab(tab: VaultScreenTab): CategoryIntroInfo {
    return when (tab) {
        VaultScreenTab.PHOTOS -> CategoryIntroInfo(
            titleKey = "overlay_photos_title",
            subtitleKey = "overlay_photos_sub",
            accentColor = Color(0xFF38BDF8),
            heroIcon = Icons.Default.PhotoLibrary,
            heroDrawable = com.example.R.drawable.img_vault_photos_icon_1790257695579,
            steps = listOf(
                IntroStepInfo("overlay_photos_step1_title", "overlay_photos_step1_desc", Icons.Default.Add),
                IntroStepInfo("overlay_photos_step2_title", "overlay_photos_step2_desc", Icons.Default.Lock),
                IntroStepInfo("overlay_photos_step3_title", "overlay_photos_step3_desc", Icons.Default.Delete)
            )
        )
        VaultScreenTab.VIDEOS -> CategoryIntroInfo(
            titleKey = "overlay_videos_title",
            subtitleKey = "overlay_videos_sub",
            accentColor = Color(0xFFF59E0B),
            heroIcon = Icons.Default.VideoLibrary,
            heroDrawable = com.example.R.drawable.img_vault_videos_icon_1790257706862,
            steps = listOf(
                IntroStepInfo("overlay_videos_step1_title", "overlay_videos_step1_desc", Icons.Default.VideoLibrary),
                IntroStepInfo("overlay_videos_step2_title", "overlay_videos_step2_desc", Icons.Default.Security),
                IntroStepInfo("overlay_videos_step3_title", "overlay_videos_step3_desc", Icons.Default.VisibilityOff)
            )
        )
        VaultScreenTab.NOTES -> CategoryIntroInfo(
            titleKey = "overlay_notes_title",
            subtitleKey = "overlay_notes_sub",
            accentColor = Color(0xFF10B981),
            heroIcon = Icons.Default.Description,
            heroDrawable = com.example.R.drawable.ic_app_notes_real_1789885876446,
            steps = listOf(
                IntroStepInfo("overlay_notes_step1_title", "overlay_notes_step1_desc", Icons.Default.Description),
                IntroStepInfo("overlay_notes_step2_title", "overlay_notes_step2_desc", Icons.Default.Check)
            )
        )
        VaultScreenTab.DOCUMENTS -> CategoryIntroInfo(
            titleKey = "overlay_docs_title",
            subtitleKey = "overlay_docs_sub",
            accentColor = Color(0xFF8B5CF6),
            heroIcon = Icons.Default.Description,
            heroDrawable = com.example.R.drawable.ic_disguise_files_1789899243046,
            steps = listOf(
                IntroStepInfo("overlay_docs_step1_title", "overlay_docs_step1_desc", Icons.Default.Lock),
                IntroStepInfo("overlay_docs_step2_title", "overlay_docs_step2_desc", Icons.Default.Check)
            )
        )
        VaultScreenTab.AUDIOS -> CategoryIntroInfo(
            titleKey = "overlay_audio_title",
            subtitleKey = "overlay_audio_sub",
            accentColor = Color(0xFFEC4899),
            heroIcon = Icons.Default.AudioFile,
            heroDrawable = com.example.R.drawable.ic_disguise_radio,
            steps = listOf(
                IntroStepInfo("overlay_audio_step1_title", "overlay_audio_step1_desc", Icons.Default.VisibilityOff),
                IntroStepInfo("overlay_audio_step2_title", "overlay_audio_step2_desc", Icons.Default.AudioFile)
            )
        )
        VaultScreenTab.BROWSER -> CategoryIntroInfo(
            titleKey = "overlay_browser_title",
            subtitleKey = "overlay_browser_sub",
            accentColor = Color(0xFF06B6D4),
            heroIcon = Icons.Default.Public,
            heroDrawable = com.example.R.drawable.img_vault_browser_icon_1790257683240,
            steps = listOf(
                IntroStepInfo("overlay_browser_step1_title", "overlay_browser_step1_desc", Icons.Default.Public),
                IntroStepInfo("overlay_browser_step2_title", "overlay_browser_step2_desc", Icons.Default.Lock)
            )
        )
        VaultScreenTab.APP_LOCK -> CategoryIntroInfo(
            titleKey = "overlay_applock_title",
            subtitleKey = "overlay_applock_sub",
            accentColor = Color(0xFF6366F1),
            heroIcon = Icons.Default.Lock,
            steps = listOf(
                IntroStepInfo("overlay_applock_step1_title", "overlay_applock_step1_desc", Icons.Default.Security),
                IntroStepInfo("overlay_applock_step2_title", "overlay_applock_step2_desc", Icons.Default.LockOpen)
            )
        )
        VaultScreenTab.TRASH -> CategoryIntroInfo(
            titleKey = "overlay_trash_title",
            subtitleKey = "overlay_trash_sub",
            accentColor = Color(0xFFEF4444),
            heroIcon = Icons.Default.DeleteSweep,
            steps = listOf(
                IntroStepInfo("overlay_trash_step1_title", "overlay_trash_step1_desc", Icons.Default.Security),
                IntroStepInfo("overlay_trash_step2_title", "overlay_trash_step2_desc", Icons.Default.Delete)
            )
        )
        VaultScreenTab.SECRET_CAMERA -> CategoryIntroInfo(
            titleKey = "overlay_camera_title",
            subtitleKey = "overlay_camera_sub",
            accentColor = Color(0xFFFB7185),
            heroIcon = Icons.Default.PhotoCamera,
            heroDrawable = com.example.R.drawable.img_vault_camera_icon_1790257719221,
            steps = listOf(
                IntroStepInfo("overlay_camera_step1_title", "overlay_camera_step1_desc", Icons.Default.PhotoCamera),
                IntroStepInfo("overlay_camera_step2_title", "overlay_camera_step2_desc", Icons.Default.Security)
            )
        )
        VaultScreenTab.INTRUDER_ALERTS -> CategoryIntroInfo(
            titleKey = "overlay_intruder_title",
            subtitleKey = "overlay_intruder_sub",
            accentColor = Color(0xFFF87171),
            heroIcon = Icons.Default.Security,
            heroDrawable = com.example.R.drawable.img_vault_intruder_icon_1790257731760,
            steps = listOf(
                IntroStepInfo("overlay_intruder_step1_title", "overlay_intruder_step1_desc", Icons.Default.Security),
                IntroStepInfo("overlay_intruder_step2_title", "overlay_intruder_step2_desc", Icons.Default.VisibilityOff)
            )
        )
        else -> CategoryIntroInfo(
            titleKey = "overlay_photos_title",
            subtitleKey = "overlay_photos_sub",
            accentColor = Color(0xFF38BDF8),
            heroIcon = Icons.Default.Security,
            steps = emptyList()
        )
    }
}

@Composable
fun CategoryIntroOverlay(
    tab: VaultScreenTab,
    appLanguage: String = "en",
    onDismissAndProceed: () -> Unit
) {
    val info = remember(tab) { getIntroInfoForTab(tab) }

    // Pulsing subtle animation for hero badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = onDismissAndProceed,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, info.accentColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .testTag("category_intro_overlay"),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top row with Category Badge and Dismiss "✕" Cross
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(info.accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = VaultStrings.get(appLanguage, "overlay_badge"),
                            style = MaterialTheme.typography.labelSmall,
                            color = info.accentColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismissAndProceed,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_close_intro_overlay")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = VaultStrings.get(appLanguage, "close"),
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hero Icon with gentle pulse
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    info.accentColor.copy(alpha = 0.35f),
                                    info.accentColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.5.dp, info.accentColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (info.heroDrawable != null) {
                        Image(
                            painter = painterResource(id = info.heroDrawable),
                            contentDescription = null,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = info.heroIcon,
                            contentDescription = null,
                            tint = info.accentColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title & Subtitle
                Text(
                    text = VaultStrings.get(appLanguage, info.titleKey),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = VaultStrings.get(appLanguage, info.subtitleKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Feature points
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    info.steps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(34.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = info.accentColor.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = step.icon,
                                        contentDescription = null,
                                        tint = info.accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = VaultStrings.get(appLanguage, step.titleKey),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = VaultStrings.get(appLanguage, step.descKey),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Got It / Continue Button
                Button(
                    onClick = onDismissAndProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_intro_got_it"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = info.accentColor,
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = VaultStrings.get(appLanguage, "got_it"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
