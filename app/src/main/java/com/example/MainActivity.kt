package com.example

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calcvault.data.backup.GoogleDriveBackupService
import com.example.calcvault.data.backup.VaultBackupManager
import com.example.calcvault.data.camouflage.AppCamouflageManager
import com.example.calcvault.data.db.VaultDatabase
import com.example.calcvault.data.intruder.IntruderManager
import com.example.calcvault.data.repository.VaultRepository
import com.example.calcvault.data.security.VaultSecurityManager
import com.example.calcvault.data.security.VaultSensorLockManager
import com.example.calcvault.data.storage.VaultFileManager
import com.example.calcvault.ui.calculator.CalculatorScreen
import android.view.KeyEvent
import androidx.compose.runtime.rememberCoroutineScope
import com.example.calcvault.data.audio.SecretAudioRecorderManager
import com.example.calcvault.data.video.SecretVideoRecorderManager
import kotlinx.coroutines.launch
import com.example.calcvault.ui.calculator.CalculatorViewModel
import com.example.calcvault.ui.vault.VaultDashboardScreen
import com.example.calcvault.ui.vault.VaultScreenTab
import com.example.calcvault.ui.vault.VaultViewModel
import com.example.calcvault.data.ads.AdMobManager
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var lastVolumeKeyTime = 0L
    private var volumePressCount = 0

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.repeatCount == 0 && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            val now = System.currentTimeMillis()
            if (now - lastVolumeKeyTime < 950L) {
                volumePressCount++
            } else {
                volumePressCount = 1
            }
            lastVolumeKeyTime = now

            if (volumePressCount == 2) {
                // Double press detected on Volume button - toggle stealth audio recording
                SecretAudioRecorderManager.toggleRecording(applicationContext, showFeedback = true)
                return true
            } else if (volumePressCount == 3) {
                // Triple press detected on Volume button - toggle true screen-off secret video recording
                SecretVideoRecorderManager.toggleRecording(applicationContext)
                volumePressCount = 0
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val database = VaultDatabase.getDatabase(applicationContext)
        val securityManager = VaultSecurityManager(applicationContext)
        val fileManager = VaultFileManager(applicationContext)
        val repository = VaultRepository(
            mediaDao = database.mediaDao(),
            noteDao = database.noteDao(),
            fileManager = fileManager,
            securityManager = securityManager
        )
        val intruderManager = IntruderManager(applicationContext)
        val camouflageManager = AppCamouflageManager(applicationContext)
        val backupManager = VaultBackupManager(
            context = applicationContext,
            mediaDao = database.mediaDao(),
            noteDao = database.noteDao()
        )
        val driveService = GoogleDriveBackupService(applicationContext)
        val adMobManager = AdMobManager(applicationContext)

        setContent {
            MyApplicationTheme {
                MainAppContent(
                    activity = this@MainActivity,
                    repository = repository,
                    securityManager = securityManager,
                    intruderManager = intruderManager,
                    camouflageManager = camouflageManager,
                    backupManager = backupManager,
                    driveService = driveService,
                    adMobManager = adMobManager
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    activity: Activity,
    repository: VaultRepository,
    securityManager: VaultSecurityManager,
    intruderManager: IntruderManager,
    camouflageManager: AppCamouflageManager,
    backupManager: VaultBackupManager,
    driveService: GoogleDriveBackupService,
    adMobManager: AdMobManager
) {
    var isVaultUnlocked by rememberSaveable { mutableStateOf(false) }

    val calculatorViewModel: CalculatorViewModel = viewModel(
        factory = CalculatorViewModel.Factory(securityManager, intruderManager)
    )
    val vaultViewModel: VaultViewModel = viewModel(
        factory = VaultViewModel.Factory(repository)
    )

    val vaultUiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val allowScreenshots by securityManager.allowScreenshotsFlow.collectAsStateWithLifecycle()
    val appScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        SecretAudioRecorderManager.onAudioRecordedCallback = { file, title ->
            appScope.launch {
                repository.saveSecretRecordedAudio(file, title)
            }
        }
        SecretVideoRecorderManager.onVideoRecordedCallback = {
            vaultViewModel.onVaultUnlocked()
        }
        onDispose {
            SecretAudioRecorderManager.onAudioRecordedCallback = null
            SecretVideoRecorderManager.onVideoRecordedCallback = null
        }
    }

    var lastUnlockTimestamp by remember { mutableStateOf(0L) }

    // Dynamic Screenshot / Screen Recording Toggle (WindowManager.LayoutParams.FLAG_SECURE)
    LaunchedEffect(isVaultUnlocked, allowScreenshots) {
        try {
            val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic") ||
                    android.os.Build.FINGERPRINT.startsWith("unknown") ||
                    android.os.Build.MODEL.contains("google_sdk") ||
                    android.os.Build.MODEL.contains("Emulator") ||
                    android.os.Build.MODEL.contains("Android SDK built for x86") ||
                    android.os.Build.HARDWARE.contains("goldfish") ||
                    android.os.Build.HARDWARE.contains("ranchu") ||
                    android.os.Build.PRODUCT.contains("sdk")

            if (!isEmulator && isVaultUnlocked && !allowScreenshots) {
                activity.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    // Panic / Stealth Hardware Sensor Lock Manager (Flip-Down & Shake)
    val sensorLockManager = remember { VaultSensorLockManager(context, securityManager) }
    val lockVaultImmediately: (String) -> Unit = { reason ->
        if (securityManager.isExternalActivityActive) {
            android.util.Log.d("MainActivity", "Ignored lock ($reason) because external activity is active")
        } else {
            isVaultUnlocked = false
            vaultViewModel.closeMediaView()
            vaultViewModel.closeNoteEditor()
            vaultViewModel.navigateTo(VaultScreenTab.DASHBOARD)
            calculatorViewModel.onClearClick()
            calculatorViewModel.checkPinStatus()
        }
    }

    DisposableEffect(isVaultUnlocked, lifecycleOwner) {
        if (isVaultUnlocked) {
            sensorLockManager.startListening { reason ->
                lockVaultImmediately(reason)
            }
        } else {
            sensorLockManager.stopListening()
        }
        onDispose {
            sensorLockManager.stopListening()
        }
    }

    // Auto-lock when the app is minimized or put into background (Lifecycle Event Observer)
    DisposableEffect(lifecycleOwner, isVaultUnlocked) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val elapsedSinceUnlock = System.currentTimeMillis() - lastUnlockTimestamp
                // Auto-lock only when minimized after unlock grace period
                if (isVaultUnlocked && !securityManager.isExternalActivityActive && elapsedSinceUnlock > 1500L) {
                    lockVaultImmediately("App minimized/exited")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle Back Button Navigation
    if (isVaultUnlocked) {
        BackHandler {
            when {
                vaultUiState.isViewingMediaDetail -> {
                    adMobManager.onUserActionTrigger(activity) {
                        vaultViewModel.closeMediaView()
                    }
                }
                vaultUiState.isCreatingNote || vaultUiState.isEditingNote -> {
                    adMobManager.onUserActionTrigger(activity) {
                        vaultViewModel.closeNoteEditor()
                    }
                }
                vaultUiState.currentTab != VaultScreenTab.DASHBOARD -> {
                    adMobManager.onUserActionTrigger(activity) {
                        vaultViewModel.navigateTo(VaultScreenTab.DASHBOARD)
                    }
                }
                else -> {
                    // On dashboard, back locks the vault and returns to calculator
                    adMobManager.onUserActionTrigger(activity) {
                        isVaultUnlocked = false
                        calculatorViewModel.onClearClick()
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF101216)
    ) {
        Crossfade(
            targetState = isVaultUnlocked,
            animationSpec = tween(durationMillis = 350),
            label = "VaultUnlockCrossfade"
        ) { unlocked ->
            if (unlocked) {
                VaultDashboardScreen(
                    viewModel = vaultViewModel,
                    securityManager = securityManager,
                    intruderManager = intruderManager,
                    camouflageManager = camouflageManager,
                    backupManager = backupManager,
                    driveService = driveService,
                    repository = repository,
                    adMobManager = adMobManager,
                    onLockNow = {
                        adMobManager.onUserActionTrigger(activity) {
                            isVaultUnlocked = false
                            vaultViewModel.navigateTo(VaultScreenTab.DASHBOARD)
                            calculatorViewModel.onClearClick()
                            calculatorViewModel.checkPinStatus()
                        }
                    }
                )
            } else {
                CalculatorScreen(
                    viewModel = calculatorViewModel,
                    securityManager = securityManager,
                    onVaultUnlocked = {
                        lastUnlockTimestamp = System.currentTimeMillis()
                        isVaultUnlocked = true
                        vaultViewModel.onVaultUnlocked()
                    },
                    onTriggerIntruderCapture = {
                        intruderManager.captureSilentSelfie(lifecycleOwner)
                    }
                )
            }
        }
    }
}
