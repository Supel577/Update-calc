package com.example.calcvault.ui.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calcvault.data.security.VaultSecurityManager

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    securityManager: VaultSecurityManager,
    onVaultUnlocked: () -> Unit,
    onTriggerIntruderCapture: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("calculator_screen"),
        color = Color(0xFF000000) // True Pitch Black native OLED
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            // Top Bar: Clean stealth native calculator header
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Calculator",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF71717A),
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Calculation History icon button
                    IconButton(
                        onClick = { viewModel.openHistoryDialog() },
                        modifier = Modifier.testTag("btn_calculation_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Calculation History",
                            tint = Color(0xFF71717A)
                        )
                    }
                }

                // PIN Setup Mode Prompt
                AnimatedVisibility(
                    visible = uiState.pinSetupStep != PinSetupStep.COMPLETED && uiState.statusMessage.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF18181B)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF9F0A),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = uiState.statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFF4F4F5),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Display Section - Takes flexible space and aligns content towards the bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Secondary preview / expression
                if (uiState.resultPreview.isNotEmpty()) {
                    Text(
                        text = uiState.resultPreview,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFF71717A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Main expression / input
                val exprLength = uiState.expression.length
                val displayFontSize = when {
                    exprLength > 12 -> 34.sp
                    exprLength > 8 -> 46.sp
                    exprLength > 5 -> 58.sp
                    else -> 70.sp
                }

                Text(
                    text = if (uiState.expression.isEmpty()) "0" else uiState.expression,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = displayFontSize,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-1).sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("calculator_display")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Keypad Grid: Apple/Samsung Pro style circular keys
            val funcBg = Color(0xFFA5A5A5)
            val funcText = Color(0xFF000000)
            val digitBg = Color(0xFF333333)
            val digitText = Color(0xFFFFFFFF)
            val opBg = Color(0xFFFF9F0A)
            val opText = Color(0xFFFFFFFF)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: C, ⌫, %, ÷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CalcKey(
                        text = "AC",
                        modifier = Modifier.weight(1f),
                        backgroundColor = funcBg,
                        textColor = funcText,
                        onClick = { viewModel.onClearClick() }
                    )
                    CalcKeyIcon(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        modifier = Modifier.weight(1f),
                        backgroundColor = funcBg,
                        textColor = funcText,
                        onClick = { viewModel.onBackspaceClick() }
                    )
                    CalcKey(
                        text = "%",
                        modifier = Modifier.weight(1f),
                        backgroundColor = funcBg,
                        textColor = funcText,
                        onClick = { viewModel.onPercentClick() }
                    )
                    CalcKey(
                        text = "÷",
                        modifier = Modifier.weight(1f),
                        backgroundColor = opBg,
                        textColor = opText,
                        onClick = { viewModel.onOperatorClick("÷") }
                    )
                }

                // Row 2: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CalcKey(text = "7", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("7") })
                    CalcKey(text = "8", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("8") })
                    CalcKey(text = "9", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("9") })
                    CalcKey(
                        text = "×",
                        modifier = Modifier.weight(1f),
                        backgroundColor = opBg,
                        textColor = opText,
                        onClick = { viewModel.onOperatorClick("×") }
                    )
                }

                // Row 3: 4, 5, 6, -
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CalcKey(text = "4", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("4") })
                    CalcKey(text = "5", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("5") })
                    CalcKey(text = "6", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("6") })
                    CalcKey(
                        text = "-",
                        modifier = Modifier.weight(1f),
                        backgroundColor = opBg,
                        textColor = opText,
                        onClick = { viewModel.onOperatorClick("-") }
                    )
                }

                // Row 4: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CalcKey(text = "1", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("1") })
                    CalcKey(text = "2", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("2") })
                    CalcKey(text = "3", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("3") })
                    CalcKey(
                        text = "+",
                        modifier = Modifier.weight(1f),
                        backgroundColor = opBg,
                        textColor = opText,
                        onClick = { viewModel.onOperatorClick("+") }
                    )
                }

                // Row 5: 00, 0, ., =
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CalcKey(text = "00", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("00") })
                    CalcKey(text = "0", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDigitClick("0") })
                    CalcKey(text = ".", modifier = Modifier.weight(1f), backgroundColor = digitBg, textColor = digitText, onClick = { viewModel.onDecimalClick() })
                    CalcKey(
                        text = "=",
                        modifier = Modifier.weight(1f),
                        backgroundColor = opBg,
                        textColor = opText,
                        onClick = { viewModel.onEqualClick(onVaultUnlocked, onTriggerIntruderCapture) }
                    )
                }
            }
        }
    }

    // Calculation History Dialog
    if (uiState.showHistoryDialog) {
        CalculationHistoryDialog(
            history = uiState.calculationHistory,
            appLanguage = securityManager.appLanguage,
            onDismiss = { viewModel.dismissHistoryDialog() },
            onClear = { viewModel.clearHistory() }
        )
    }

    // Security Question / Recovery Dialog
    if (uiState.showRecoveryDialog) {
        RecoveryDialog(
            securityManager = securityManager,
            onDismiss = { viewModel.dismissRecoveryDialog() },
            onResetSuccess = { newPin ->
                viewModel.handleRecoveryReset(newPin)
            }
        )
    }
}

@Composable
fun CalcKey(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF333333),
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .testTag("key_$text"),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (text.length > 2) 20.sp else 28.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif
                ),
                color = textColor
            )
        }
    }
}

@Composable
fun CalcKeyIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFA5A5A5),
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .testTag("key_backspace"),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Backspace",
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun RecoveryDialog(
    securityManager: VaultSecurityManager,
    onDismiss: () -> Unit,
    onResetSuccess: (String) -> Unit
) {
    val appLanguage = securityManager.appLanguage
    val defaultQuestion = if (appLanguage == "bn") "আপনার প্রিয় রং কোনটি?" else "What is your favorite color?"
    val question = securityManager.getSecurityQuestion() ?: defaultQuestion
    var recoveryMethod by remember { mutableIntStateOf(0) } // 0: Security Question, 1: 12-Word Seed
    var answerInput by remember { mutableStateOf("") }
    var seedWordsInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181B),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFA1A1AA),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (recoveryMethod == 0) Icons.Default.Security else Icons.Default.Key,
                    contentDescription = null,
                    tint = Color(0xFFFF9F0A)
                )
                Text(if (appLanguage == "bn") "পিন পুনরুদ্ধার (Recovery)" else "PIN Recovery")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabRow(
                    selectedTabIndex = recoveryMethod,
                    containerColor = Color(0xFF27272A),
                    contentColor = Color(0xFFFF9F0A),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[recoveryMethod]),
                            color = Color(0xFFFF9F0A)
                        )
                    }
                ) {
                    Tab(
                        selected = recoveryMethod == 0,
                        onClick = { recoveryMethod = 0; errorMessage = "" },
                        text = { Text(if (appLanguage == "bn") "সিকিউরিটি প্রশ্ন" else "Security Question", fontSize = 12.sp, color = if (recoveryMethod == 0) Color.White else Color(0xFFA1A1AA)) }
                    )
                    Tab(
                        selected = recoveryMethod == 1,
                        onClick = { recoveryMethod = 1; errorMessage = "" },
                        text = { Text(if (appLanguage == "bn") "১২-শব্দের সিড" else "12-Word Seed", fontSize = 12.sp, color = if (recoveryMethod == 1) Color.White else Color(0xFFA1A1AA)) }
                    )
                }

                if (recoveryMethod == 0) {
                    Text(
                        text = if (appLanguage == "bn") "গোপন পিন রিসেট করতে আপনার নিরাপত্তা প্রশ্নের উত্তর দিন:" else "Answer your security question to reset the secret vault PIN:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA)
                    )

                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = {
                            answerInput = it
                            errorMessage = ""
                        },
                        label = { Text(if (appLanguage == "bn") "নিরাপত্তা উত্তর" else "Security Answer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = if (appLanguage == "bn") "আপনার ১২-শব্দের পেপার কি লিখুন বা পেস্ট করুন (স্পেস দিয়ে আলাদা):" else "Enter or paste your 12 recovery seed words (separated by spaces):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA)
                    )

                    OutlinedTextField(
                        value = seedWordsInput,
                        onValueChange = {
                            seedWordsInput = it
                            errorMessage = ""
                        },
                        label = { Text(if (appLanguage == "bn") "১২-শব্দের সিড ফ্রেজ" else "12 Recovery Words") },
                        placeholder = { Text(if (appLanguage == "bn") "যেমন: anchor cipher dragon falcon..." else "e.g. anchor cipher dragon falcon...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = {
                        if (it.length <= 8 && it.all { c -> c.isDigit() }) {
                            newPinInput = it
                            errorMessage = ""
                        }
                    },
                    label = { Text(if (appLanguage == "bn") "নতুন পিন (কমপক্ষে ৪ সংখ্যা)" else "New PIN (min 4 digits)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPinInput,
                    onValueChange = {
                        if (it.length <= 8 && it.all { c -> c.isDigit() }) {
                            confirmPinInput = it
                            errorMessage = ""
                        }
                    },
                    label = { Text(if (appLanguage == "bn") "নতুন পিন নিশ্চিত করুন" else "Confirm New PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (recoveryMethod == 0) {
                        if (answerInput.isBlank()) {
                            errorMessage = if (appLanguage == "bn") "অনুগ্রহ করে আপনার নিরাপত্তা উত্তর লিখুন।" else "Please enter your security answer."
                            return@Button
                        }
                        if (!securityManager.verifySecurityAnswer(answerInput)) {
                            errorMessage = if (appLanguage == "bn") "নিরাপত্তা উত্তরটি সঠিক নয়।" else "Incorrect security answer."
                            return@Button
                        }
                    } else {
                        val words = seedWordsInput.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                        if (words.size != 12) {
                            errorMessage = if (appLanguage == "bn") "অনুগ্রহ করে ঠিক ১২টি শব্দ লিখুন।" else "Please enter exactly 12 recovery words."
                            return@Button
                        }
                        if (!securityManager.verifyRecoverySeed(words)) {
                            errorMessage = if (appLanguage == "bn") "১২-শব্দের সিড কি সঠিক নয়।" else "Incorrect 12-word seed phrase."
                            return@Button
                        }
                    }

                    if (newPinInput.length < 4) {
                        errorMessage = if (appLanguage == "bn") "নতুন পিন কমপক্ষে ৪ সংখ্যার হতে হবে।" else "New PIN must be at least 4 digits."
                        return@Button
                    }
                    if (newPinInput != confirmPinInput) {
                        errorMessage = if (appLanguage == "bn") "নতুন পিন দুটি মেলেনি।" else "PINs do not match."
                        return@Button
                    }

                    securityManager.setupPin(newPinInput)
                    onResetSuccess(newPinInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A))
            ) {
                Text(if (appLanguage == "bn") "পিন রিসেট করুন" else "Reset PIN", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (appLanguage == "bn") "বাতিল" else "Cancel", color = Color(0xFFA1A1AA))
            }
        }
    )
}

@Composable
fun CalculationHistoryDialog(
    history: List<String>,
    appLanguage: String = "en",
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181B),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFA1A1AA),
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
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFFFF9F0A)
                    )
                    Text(if (appLanguage == "bn") "হিস্ট্রি" else "History")
                }
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text(if (appLanguage == "bn") "মুছে ফেলুন" else "Clear", color = Color(0xFFEF4444))
                    }
                }
            }
        },
        text = {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (appLanguage == "bn") "কোনো পূর্ববর্তী হিসাব নেই" else "No recent calculations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF71717A)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF27272A),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFF4F4F5),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A))
            ) {
                Text(if (appLanguage == "bn") "বন্ধ করুন" else "Close", color = Color.White)
            }
        }
    )
}
