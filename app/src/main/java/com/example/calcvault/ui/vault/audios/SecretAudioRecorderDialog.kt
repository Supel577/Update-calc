package com.example.calcvault.ui.vault.audios

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.calcvault.data.audio.SecretAudioRecorderManager
import java.io.File
import java.util.Locale

@Composable
fun SecretAudioRecorderDialog(
    appLanguage: String,
    onDismiss: () -> Unit,
    onSaveToVault: ((File, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) {
            Toast.makeText(
                context,
                if (appLanguage == "bn") "গোপন রেকর্ডের জন্য মাইক্রোফোন পারমিশন প্রয়োজন" else "Microphone permission is required for secret recording",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val isRecording by SecretAudioRecorderManager.isRecording.collectAsStateWithLifecycle()
    val recordingSeconds by SecretAudioRecorderManager.recordingSeconds.collectAsStateWithLifecycle()
    val lastSavedFile by SecretAudioRecorderManager.lastSavedFile.collectAsStateWithLifecycle()

    var recordedFileName by remember { mutableStateOf("") }
    var saveFeedbackDone by remember { mutableStateOf(false) }
    var isFakeScreenOff by remember { mutableStateOf(false) }

    if (isFakeScreenOff) {
        Dialog(
            onDismissRequest = { isFakeScreenOff = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { isFakeScreenOff = false }
                        )
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = if (appLanguage == "bn")
                        "🔴 গোপন রেকর্ড চলছে • স্ক্রিন দেখতে যেকোনো স্থানে ২ বার চাপুন"
                    else
                        "🔴 Covert Recording Active • Double-tap anywhere to reveal",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 36.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun formatDuration(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isRecording) onDismiss()
        },
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE11D48).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color(0xFFFB7185)
                        )
                    }
                    Column {
                        Text(
                            text = if (appLanguage == "bn") "সাইলেন্ট অডিও রেকর্ডার" else "Silent Audio Recorder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (appLanguage == "bn") "বাস্তব স্ক্রিন-অফ ব্যাকগ্রাউন্ড মোড" else "Real Screen-Off Background Recording",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                if (!isRecording) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!hasAudioPermission) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (appLanguage == "bn") "মাইক্রোফোন পারমিশন প্রয়োজন" else "Microphone Permission Required",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (appLanguage == "bn")
                                    "গোপন অডিও রেকর্ড চালু করতে মাইক্রোফোন অনুমোদন দিন।"
                                else
                                    "Grant microphone access to securely record audio into your vault.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                            ) {
                                Text(if (appLanguage == "bn") "অনুমতি দিন" else "Grant Permission")
                            }
                        }
                    }
                } else {
                    // Recording Visualizer Box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isRecording) Color(0xFFE11D48).copy(alpha = 0.6f) else Color(0xFF334155),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Pulsing Dot & Timer
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isRecording) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.2f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(600),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "alpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444).copy(alpha = alpha))
                                    )
                                }
                                Text(
                                    text = formatDuration(recordingSeconds),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRecording) Color(0xFFF43F5E) else Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Waveform Graphic
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val barHeights = if (isRecording) {
                                    listOf(14, 26, 36, 18, 30, 22, 34, 16, 28, 20, 32, 24, 18, 30, 14)
                                } else {
                                    listOf(6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6)
                                }

                                barHeights.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isRecording) Color(0xFFFB7185) else Color(0xFF475569))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isRecording) Color(0xFFDC2626).copy(alpha = 0.2f) else Color(0xFF334155).copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isRecording) Color(0xFFDC2626).copy(alpha = 0.4f) else Color(0xFF475569)
                                )
                            ) {
                                Text(
                                    text = if (isRecording)
                                        (if (appLanguage == "bn") "🔴 রেকর্ড চলছে • স্ক্রিন সম্পূর্ণ অফ করা যাবে" else "🔴 Live • Physical Screen-Off Supported")
                                    else
                                        (if (appLanguage == "bn") "স্ট্যান্ডবাই • রেকর্ড করতে বাটনে চাপুন" else "Standby • Ready to record"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isRecording) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Helpful Stealth & Screen-Off Guide
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (appLanguage == "bn") "প্রকৃত স্ক্রিন-অফ সুবিধা:" else "Real Screen-Off Power:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            Text(
                                text = if (appLanguage == "bn")
                                    "রেকর্ড শুরু করে আপনার ফোনের আসল পাওয়ার বাটন দিয়ে স্ক্রিন সম্পূর্ণ নিভিয়ে (অফ করে) পকেটে রেখে দিন। ব্যাকগ্রাউন্ডে রেকর্ড অক্ষত থাকবে!"
                                else
                                    "Lock your physical phone screen with the power button anytime. Covert recording continues flawlessly in the background!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeDown,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (appLanguage == "bn") "ভলিউম কি ট্রিগার:" else "Volume Key Trigger:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                            Text(
                                text = if (appLanguage == "bn")
                                    "স্ক্রিন না খুলেই ভলিউম ডাউন কি ২ বার দ্রুত চাপলে স্বয়ংক্রিয়ভাবে গোপন রেকর্ড চালু/বন্ধ হয় এবং সূক্ষ্ম ভাইব্রেশনের মাধ্যমে নিশ্চিত করে।"
                                else
                                    "Double-press Volume Down button anytime to toggle secret recording silently with discreet haptic vibration feedback.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (appLanguage == "bn") "সবুজ বাতি (Green Dot) ও ক্যামেরা তথ্য:" else "Green Dot & Camera Notice:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                            Text(
                                text = if (appLanguage == "bn")
                                    "অ্যান্ড্রয়েড সিস্টেমে মাইক্রোফোন সক্রিয় থাকলে ব্যাটারির পাশে সবুজ বাতি জ্বলে। নিশ্চিন্ত থাকুন এটি কেবল অডিওর জন্য, ক্যামেরা সম্পূর্ণ বন্ধ এবং কোনো ভিডিও হচ্ছে না!"
                                else
                                    "Android displays a green privacy dot whenever the microphone is active. Rest assured, this is purely audio — camera is 100% off and no video is being captured!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional Title Input
                    OutlinedTextField(
                        value = recordedFileName,
                        onValueChange = { recordedFileName = it },
                        label = { Text(if (appLanguage == "bn") "অডিও ফাইলের শিরোনাম (ঐচ্ছিক)" else "Audio Memo Title (Optional)") },
                        placeholder = { Text(if (appLanguage == "bn") "যেমন: সিক্রেট নোট" else "e.g. Confidential Audio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big, comfortable, non-squished Action Button
                    if (!isRecording) {
                        Button(
                            onClick = {
                                saveFeedbackDone = false
                                SecretAudioRecorderManager.startRecording(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_start_secret_rec")
                        ) {
                            Icon(imageVector = Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appLanguage == "bn") "গোপন রেকর্ড শুরু করুন" else "Start Secret Recording",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val customTitle = recordedFileName.trim().ifBlank { null }
                                SecretAudioRecorderManager.stopRecording(context, customTitle = customTitle)
                                saveFeedbackDone = true
                                Toast.makeText(
                                    context,
                                    if (appLanguage == "bn") "অডিও সফলভাবে ভল্টে সংরক্ষিত হয়েছে!" else "Audio successfully saved to vault!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_stop_secret_rec")
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appLanguage == "bn") "রেকর্ড সম্পন্ন ও সেভ করুন" else "Stop & Save to Vault",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // AMOLED Fake Screen-off button
                        Button(
                            onClick = { isFakeScreenOff = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appLanguage == "bn") "ভুয়া স্ক্রিন অফ মোড (কালো পর্দা)" else "Fake Screen-Off Mode (Pitch Black)",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (saveFeedbackDone) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981))
                            Text(
                                text = if (appLanguage == "bn") "ভল্টে সংরক্ষিত হয়েছে" else "Saved into vault",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (!isRecording) {
                TextButton(onClick = onDismiss) {
                    Text(if (appLanguage == "bn") "বন্ধ করুন" else "Close", color = Color(0xFF94A3B8))
                }
            }
        }
    )
}
