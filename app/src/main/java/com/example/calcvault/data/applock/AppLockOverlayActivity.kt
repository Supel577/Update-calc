package com.example.calcvault.data.applock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.calcvault.data.security.VaultSecurityManager

class AppLockOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_target_package"
        @Volatile
        var isShowing: Boolean = false
        @Volatile
        var currentShowingPackage: String? = null
    }

    private lateinit var appLockManager: AppLockManager
    private lateinit var securityManager: VaultSecurityManager
    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appLockManager = AppLockManager.getInstance(this)
        securityManager = VaultSecurityManager(this)
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME)

        if (targetPackage == null) {
            isShowing = false
            currentShowingPackage = null
            finish()
            return
        }

        isShowing = true
        currentShowingPackage = targetPackage

        setContent {
            AppLockOverlayContent(
                targetPackage = targetPackage!!,
                onSuccess = {
                    isShowing = false
                    currentShowingPackage = null
                    appLockManager.unlockForSession(targetPackage!!)
                    finish()
                },
                onCancel = {
                    goToHomeScreen()
                },
                securityManager = securityManager
            )
        }
    }

    override fun onDestroy() {
        isShowing = false
        currentShowingPackage = null
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        goToHomeScreen()
    }

    private fun goToHomeScreen() {
        isShowing = false
        val pkg = targetPackage
        if (pkg != null) {
            appLockManager.lockAgain(pkg)
        }
        currentShowingPackage = null
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun AppLockOverlayContent(
    targetPackage: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    securityManager: VaultSecurityManager
) {
    val context = LocalContext.current
    val pm = context.packageManager

    var appName by remember { mutableStateOf("Protected App") }
    var appIconDrawable by remember { mutableStateOf<Drawable?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetPackage) {
        try {
            val appInfo = pm.getApplicationInfo(targetPackage, 0)
            appName = pm.getApplicationLabel(appInfo).toString()
            appIconDrawable = pm.getApplicationIcon(appInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val lang = securityManager.appLanguage
    val incorrectPinMsg = if (lang == "bn") "ভুল পিন! পুনরায় চেষ্টা করুন।" else "Incorrect PIN. Try again."

    fun submitPin(pin: String) {
        if (securityManager.verifyPin(pin)) {
            onSuccess()
        } else {
            errorMessage = incorrectPinMsg
            pinInput = ""
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFF090D16)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with App Icon and Lock Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1E293B))
                        .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIconDrawable != null) {
                        Image(
                            bitmap = appIconDrawable!!.toBitmap(160, 160).asImageBitmap(),
                            contentDescription = appName,
                            modifier = Modifier.size(52.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    // Little lock badge
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF0284C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (lang == "bn") "$appName লক করা আছে" else "$appName is Locked",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (lang == "bn") "ক্যালকুলেটর ভল্ট দ্বারা সুরক্ষিত\nপ্রবেশ করতে আপনার গোপন পিন দিন" else "Protected by HideU Calculator Vault\nEnter your secret PIN to access",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4.coerceAtLeast(pinInput.length)) { index ->
                        val filled = index < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) Color(0xFF38BDF8) else Color(0xFF334155))
                                .border(
                                    1.dp,
                                    if (filled) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    CircleShape
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFF87171),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("cancel", "0", "backspace")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (digit in row) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when (digit) {
                                            "cancel" -> Color(0xFF334155).copy(alpha = 0.4f)
                                            "backspace" -> Color(0xFF334155).copy(alpha = 0.5f)
                                            else -> Color(0xFF1E293B)
                                        }
                                    )
                                    .clickable {
                                        when (digit) {
                                            "cancel" -> onCancel()
                                            "backspace" -> {
                                                if (pinInput.isNotEmpty()) {
                                                    pinInput = pinInput.dropLast(1)
                                                    errorMessage = null
                                                }
                                            }
                                            else -> {
                                                if (pinInput.length < 12) {
                                                    val newPin = pinInput + digit
                                                    pinInput = newPin
                                                    errorMessage = null
                                                    if (securityManager.verifyPin(newPin)) {
                                                        onSuccess()
                                                    } else if (newPin.length == 4) {
                                                        // Check if 4 digit pin is incorrect
                                                        // If incorrect, also check if it's incorrect and give immediate feedback
                                                        errorMessage = incorrectPinMsg
                                                        try {
                                                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                                            } else {
                                                                @Suppress("DEPRECATION")
                                                                vibrator?.vibrate(200)
                                                            }
                                                        } catch (_: Exception) {}
                                                        pinInput = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (digit) {
                                    "cancel" -> Text(
                                        text = if (lang == "bn") "বের হন" else "Exit",
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    "backspace" -> Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFCBD5E1),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    else -> Text(
                                        text = digit,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 24.sp
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
