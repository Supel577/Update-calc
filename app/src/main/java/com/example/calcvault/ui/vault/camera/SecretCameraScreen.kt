package com.example.calcvault.ui.vault.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import com.example.calcvault.data.video.SecretVideoRecorderManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calcvault.data.security.VaultSecurityManager
import com.example.calcvault.ui.vault.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

enum class SecretCameraMode {
    PHOTO,
    VIDEO
}

@SuppressLint("MissingPermission")
@Composable
fun SecretCameraScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onNavigateToPhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val securityManager = remember { VaultSecurityManager(context) }
    val appLanguage = securityManager.appLanguage

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* result handled dynamically */ }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val cam = grants[Manifest.permission.CAMERA] == true
        if (cam) hasCameraPermission = true
    }

    var cameraMode by remember { mutableStateOf(SecretCameraMode.PHOTO) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableIntStateOf(0) } // 0: Off, 1: On, 2: Torch, 3: Auto
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    var isCapturingPhoto by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var videoRecordingDuration by remember { mutableIntStateOf(0) }
    var flashShutterAnimation by remember { mutableStateOf(false) }

    val isBgVideoRecording by SecretVideoRecorderManager.isRecording.collectAsStateWithLifecycle()
    val bgVideoSeconds by SecretVideoRecorderManager.recordingSeconds.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        SecretVideoRecorderManager.onVideoRecordedCallback = {
            viewModel.onVaultUnlocked()
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Timer effect for video recording
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            videoRecordingDuration = 0
            while (isActive) {
                delay(1000L)
                videoRecordingDuration++
            }
        } else {
            videoRecordingDuration = 0
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                try {
                    cameraInstance?.cameraControl?.enableTorch(false)
                } catch (_: Throwable) {}
                try {
                    activeRecording?.stop()
                    activeRecording = null
                    isRecordingVideo = false
                } catch (_: Throwable) {}
                try {
                    cameraProviderRef?.unbindAll()
                } catch (_: Throwable) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                cameraInstance?.cameraControl?.enableTorch(false)
            } catch (_: Throwable) {}
            try {
                activeRecording?.stop()
                activeRecording = null
            } catch (_: Throwable) {}
            try {
                cameraProviderRef?.unbindAll()
            } catch (_: Throwable) {}
            cameraInstance = null
            imageCapture = null
            videoCapture = null
            try {
                cameraExecutor.shutdown()
            } catch (_: Throwable) {}
        }
    }

    if (!hasCameraPermission) {
        // Camera Permission Request Screen
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (appLanguage == "bn") "গোপন সিক্রেট ক্যামেরা" else "Secret In-App Camera",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (appLanguage == "bn")
                        "গোপনে ছবি ও ভিডিও সরাসরি ভল্টে এনক্রিপ্ট করে রাখুন। ফোন গ্যালারিতে এর কোনো চিহ্ন থাকবে না!"
                    else
                        "Capture photos and videos directly into your encrypted vault. Captured files never appear in your phone's public gallery!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                ) {
                    Text(
                        if (appLanguage == "bn") "ক্যামেরা পারমিশন দিন" else "Grant Camera Permission",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (appLanguage == "bn") "ভল্টে ফিরে যান" else "Back to Vault",
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
        return
    }

    // Effect to bind/rebind camera when previewView, cameraSelector, or cameraMode changes
    LaunchedEffect(previewViewRef, cameraSelector, cameraMode, lifecycleOwner) {
        val pv = previewViewRef ?: return@LaunchedEffect
        try {
            val cameraProvider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            cameraProviderRef = cameraProvider

            // Ensure the selected camera exists on this device
            val effectiveSelector = if (cameraProvider.hasCamera(cameraSelector)) {
                cameraSelector
            } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                null
            }

            if (effectiveSelector == null) {
                Toast.makeText(context, "No camera hardware available on this device", Toast.LENGTH_SHORT).show()
                return@LaunchedEffect
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(pv.surfaceProvider)
            }

            cameraProvider.unbindAll()

            if (cameraMode == SecretCameraMode.PHOTO) {
                val capture = ImageCapture.Builder()
                    .setFlashMode(
                        when (flashMode) {
                            1, 2 -> ImageCapture.FLASH_MODE_ON
                            3 -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    )
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture
                videoCapture = null

                val cam = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    effectiveSelector,
                    preview,
                    capture
                )
                cameraInstance = cam

                // Safe torch activation if hardware supports it
                if (flashMode == 2) {
                    try {
                        if (cam.cameraInfo.hasFlashUnit()) {
                            cam.cameraControl.enableTorch(true)
                        }
                    } catch (_: Throwable) {}
                }
            } else {
                // Secret Video Mode
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .setExecutor(cameraExecutor)
                    .build()
                val vidCap = VideoCapture.withOutput(recorder)
                videoCapture = vidCap
                imageCapture = null

                val cam = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    effectiveSelector,
                    preview,
                    vidCap
                )
                cameraInstance = cam
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    // Effect to update torch and flash mode safely without crashing front camera
    LaunchedEffect(flashMode, cameraInstance) {
        val cam = cameraInstance ?: return@LaunchedEffect
        try {
            if (cam.cameraInfo.hasFlashUnit()) {
                cam.cameraControl.enableTorch(flashMode == 2)
            } else {
                cam.cameraControl.enableTorch(false)
            }
        } catch (_: Throwable) {}

        try {
            val hasFlash = cam.cameraInfo.hasFlashUnit()
            imageCapture?.flashMode = if (hasFlash) {
                when (flashMode) {
                    1, 2 -> ImageCapture.FLASH_MODE_ON
                    3 -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
            } else {
                ImageCapture.FLASH_MODE_OFF
            }
        } catch (_: Throwable) {}
    }

    // Fullscreen Viewfinder & Controls
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("secret_camera_screen")
    ) {
        // CameraX Viewfinder
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    previewViewRef = this
                }
            }
        )

        // Shutter White Flash Animation
        AnimatedVisibility(
            visible = flashShutterAnimation,
            enter = fadeIn(tween(50)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White))
        }

        // Live Video Recording Status Indicator (Pulsating Duration)
        if (isRecordingVideo) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFDC2626).copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", videoRecordingDuration / 60, videoRecordingDuration % 60),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (appLanguage == "bn") "গোপন রেকর্ড চলছে" else "Recording Live",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Top Controls Bar (Back, Vault Security Badge, Flash toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    try { cameraInstance?.cameraControl?.enableTorch(false) } catch (_: Exception) {}
                    try { cameraProviderRef?.unbindAll() } catch (_: Exception) {}
                    onBack()
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Private Encrypted Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (cameraMode == SecretCameraMode.VIDEO) {
                            if (appLanguage == "bn") "এনক্রিপ্টেড ভিডিও ক্যাম" else "Encrypted Vault Video"
                        } else {
                            if (appLanguage == "bn") "এনক্রিপ্টেড ফটো ক্যাম" else "Encrypted Vault Photo"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Top Right Actions (Stealth Screen-Off & Flash)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Background Screen-Off Video Mode button
                if (cameraMode == SecretCameraMode.VIDEO) {
                    IconButton(
                        onClick = {
                            if (isBgVideoRecording) {
                                SecretVideoRecorderManager.stopRecording(context)
                            } else {
                                val camGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (camGranted && micGranted) {
                                    try { cameraInstance?.cameraControl?.enableTorch(false) } catch (_: Exception) {}
                                    try { cameraProviderRef?.unbindAll() } catch (_: Exception) {}
                                    val facing = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"
                                    SecretVideoRecorderManager.startRecording(context, facing)
                                    Toast.makeText(
                                        context,
                                        if (appLanguage == "bn")
                                            "স্ক্রিন-অফ ভিডিও শুরু হয়েছে! এখন পাওয়ার বাটন দিয়ে স্ক্রিন বন্ধ করতে পারেন।"
                                        else
                                            "Screen-off video started! You can now turn off the screen with the power button.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    multiplePermissionsLauncher.launch(
                                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(if (isBgVideoRecording) Color(0xFFDC2626) else Color.Black.copy(alpha = 0.5f), CircleShape)
                            .border(1.dp, if (isBgVideoRecording) Color(0xFFEF4444) else Color(0xFF38BDF8).copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isBgVideoRecording) Icons.Default.Stop else Icons.Default.Videocam,
                            contentDescription = "Screen-Off Video Recording",
                            tint = if (isBgVideoRecording) Color.White else Color(0xFF38BDF8)
                        )
                    }
                }

                // Flash / Torch Mode Toggle (Protected against front camera crash)
                IconButton(
                    onClick = {
                        val hasFlash = cameraInstance?.cameraInfo?.hasFlashUnit() == true
                        if (!hasFlash) {
                            val noFlashMsg = if (appLanguage == "bn") "এই ক্যামেরায় কোনো ফ্ল্যাশ লাইট নেই" else "No flash hardware on this camera"
                            Toast.makeText(context, noFlashMsg, Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }

                        val nextMode = (flashMode + 1) % 4
                        flashMode = nextMode
                        val modeLabel = when (nextMode) {
                            1 -> if (appLanguage == "bn") "ফ্ল্যাশ: চালু" else "Flash: ON"
                            2 -> if (appLanguage == "bn") "টর্চ: সবসময় অন" else "Torch: ALWAYS ON"
                            3 -> if (appLanguage == "bn") "ফ্ল্যাশ: অটো" else "Flash: AUTO"
                            else -> if (appLanguage == "bn") "ফ্ল্যাশ: বন্ধ" else "Flash: OFF"
                        }
                        Toast.makeText(context, modeLabel, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (flashMode == 2) Color(0xFFFACC15).copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .border(
                            width = if (flashMode == 2) 1.5.dp else 0.dp,
                            color = if (flashMode == 2) Color(0xFFFACC15) else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            1 -> Icons.Default.FlashOn
                            2 -> Icons.Default.FlashOn
                            3 -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash Mode",
                        tint = when (flashMode) {
                            1, 2 -> Color(0xFFFACC15)
                            3 -> Color(0xFF38BDF8)
                            else -> Color.White
                        }
                    )
                }
            }
        }

        // Mode Switcher Pill (PHOTO | VIDEO)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.65f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo Mode Tab
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (cameraMode == SecretCameraMode.PHOTO) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.clickable(enabled = !isRecordingVideo) {
                        cameraMode = SecretCameraMode.PHOTO
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (appLanguage == "bn") "ছবি" else "PHOTO",
                            color = Color.White,
                            fontWeight = if (cameraMode == SecretCameraMode.PHOTO) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                // Video Mode Tab
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (cameraMode == SecretCameraMode.VIDEO) Color(0xFFDC2626) else Color.Transparent,
                    modifier = Modifier.clickable(enabled = !isRecordingVideo) {
                        val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (!audioGranted) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        cameraMode = SecretCameraMode.VIDEO
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (appLanguage == "bn") "ভিডিও" else "VIDEO",
                            color = Color.White,
                            fontWeight = if (cameraMode == SecretCameraMode.VIDEO) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Bottom Capture Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.65f))
                .navigationBarsPadding()
                .padding(vertical = 18.dp, horizontal = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Link to Vault Photos / Gallery icon
                IconButton(
                    onClick = {
                        try { cameraInstance?.cameraControl?.enableTorch(false) } catch (_: Exception) {}
                        try { cameraProviderRef?.unbindAll() } catch (_: Exception) {}
                        onNavigateToPhotos()
                    },
                    enabled = !isRecordingVideo,
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Hidden Vault Media",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Shutter / Record Button
                if (cameraMode == SecretCameraMode.PHOTO) {
                    // Photo Shutter
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(if (isCapturingPhoto) Color.Gray else Color.White)
                            .clickable(enabled = !isCapturingPhoto) {
                                val capture = imageCapture ?: return@clickable
                                isCapturingPhoto = true
                                flashShutterAnimation = true

                                val tempFile = File(context.cacheDir, "temp_cam_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

                                capture.takePicture(
                                    outputOptions,
                                    cameraExecutor,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            viewModel.saveSecretCameraPhoto(tempFile) {
                                                isCapturingPhoto = false
                                                flashShutterAnimation = false
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                            isCapturingPhoto = false
                                            flashShutterAnimation = false
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                } else {
                    // Video Shutter / Stop Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(4.dp, if (isRecordingVideo) Color(0xFFDC2626) else Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable {
                                if (isRecordingVideo) {
                                    // Stop Recording
                                    try {
                                        activeRecording?.stop()
                                        activeRecording = null
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    // Start Recording
                                    val vidCap = videoCapture ?: return@clickable
                                    val tempVideoFile = File(context.cacheDir, "temp_cam_vid_${System.currentTimeMillis()}.mp4")
                                    val outputOptions = FileOutputOptions.Builder(tempVideoFile).build()
                                    val audioGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    try {
                                        val pending = vidCap.output.prepareRecording(context, outputOptions)
                                        if (audioGranted) {
                                            pending.withAudioEnabled()
                                        }
                                        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                                            when (recordEvent) {
                                                is VideoRecordEvent.Start -> {
                                                    isRecordingVideo = true
                                                }
                                                is VideoRecordEvent.Finalize -> {
                                                    isRecordingVideo = false
                                                    if (!recordEvent.hasError()) {
                                                        viewModel.saveSecretCameraVideo(tempVideoFile) {
                                                            Toast.makeText(
                                                                context,
                                                                if (appLanguage == "bn") "ভিডিও সফলভাবে ভল্টে সংরক্ষিত হয়েছে!" else "Secret video saved to vault!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    } else {
                                                        val err = recordEvent.cause?.message ?: "Recording error"
                                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Could not start video: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecordingVideo) {
                            // Red Square Stop Icon
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFDC2626), RoundedCornerShape(6.dp))
                            )
                        } else {
                            // Red Circle Record Icon
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color(0xFFDC2626), CircleShape)
                            )
                        }
                    }
                }

                // Front/Back Switch Camera
                IconButton(
                    onClick = {
                        flashMode = 0
                        try { cameraInstance?.cameraControl?.enableTorch(false) } catch (_: Throwable) {}
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    enabled = !isRecordingVideo,
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // True Screen-Off Video Mode Button (When in Video tab and not recording)
        if (cameraMode == SecretCameraMode.VIDEO && !isRecordingVideo && !isBgVideoRecording) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFDC2626),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp)
                    .clickable {
                        val camGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (camGranted && micGranted) {
                            try { cameraInstance?.cameraControl?.enableTorch(false) } catch (_: Exception) {}
                            try { cameraProviderRef?.unbindAll() } catch (_: Exception) {}
                            val facing = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"
                            SecretVideoRecorderManager.startRecording(context, facing)
                            Toast.makeText(
                                context,
                                if (appLanguage == "bn")
                                    "স্ক্রিন-অফ ভিডিও শুরু হয়েছে! এখন নিশ্চিন্তে পাওয়ার বাটন দিয়ে স্ক্রিন বন্ধ করতে পারেন।"
                                else
                                    "Screen-off video started! You can now turn off the screen with the power button.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            multiplePermissionsLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (appLanguage == "bn") "স্ক্রিন সম্পূর্ণ বন্ধ রেখে ব্যাকগ্রাউন্ডে রেকর্ড করুন" else "Record with Screen Completely OFF",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // True Screen-Off Active Background Video HUD
        if (isBgVideoRecording) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B132B).copy(alpha = 0.95f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFDC2626).copy(alpha = 0.2f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (appLanguage == "bn") "স্ক্রিন-অফ গোপন ভিডিও চলছে" else "Background Video Recording Active",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = String.format(Locale.US, "%02d:%02d", bgVideoSeconds / 60, bgVideoSeconds % 60),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEF4444)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1C2541),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (appLanguage == "bn")
                                    "ফোনের পাওয়ার বাটন চেপে স্ক্রিন সম্পূর্ণ বন্ধ করলেও ভিডিও রেকর্ড চলতে থাকবে। শেষ করতে নিচের বাটনে চাপুন।"
                                else
                                    "Turn off or lock your screen with the power button; recording continues uninterrupted. Tap below when done.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            SecretVideoRecorderManager.stopRecording(context)
                            Toast.makeText(
                                context,
                                if (appLanguage == "bn") "ভিডিও সংরক্ষিত হচ্ছে..." else "Saving secret video...",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "bn") "ভিডিও শেষ ও ভল্টে সেভ করুন" else "Stop & Save to Vault",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
