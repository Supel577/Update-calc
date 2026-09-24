package com.example.calcvault.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calcvault.data.intruder.IntruderManager
import com.example.calcvault.data.security.VaultSecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PinSetupStep {
    NOT_STARTED,
    ENTER_PIN,
    CONFIRM_PIN,
    COMPLETED
}

data class CalculatorUiState(
    val expression: String = "",
    val resultPreview: String = "",
    val isEvaluated: Boolean = false,
    val pinSetupStep: PinSetupStep = PinSetupStep.NOT_STARTED,
    val statusMessage: String = "",
    val showRecoveryDialog: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val calculationHistory: List<String> = emptyList(),
    val recoverySuccess: Boolean = false
)

class CalculatorViewModel(
    private val securityManager: VaultSecurityManager,
    private val intruderManager: IntruderManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var candidatePin: String = ""

    init {
        checkPinStatus()
    }

    fun checkPinStatus() {
        if (!securityManager.isPinConfigured()) {
            if (securityManager.detectPersistentVaultOnDevice()) {
                _uiState.update {
                    it.copy(
                        pinSetupStep = PinSetupStep.ENTER_PIN,
                        statusMessage = "Previous Vault Detected! Enter previous PIN to restore."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        pinSetupStep = PinSetupStep.ENTER_PIN,
                        statusMessage = "Set a 4-digit PIN and press =. (Forgot PIN? Type 112233=)"
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    pinSetupStep = PinSetupStep.COMPLETED,
                    statusMessage = ""
                )
            }
        }
    }

    fun onDigitClick(digit: String) {
        _uiState.update { state ->
            val newExpr = if (state.isEvaluated) digit else state.expression + digit
            val preview = if (hasOperators(newExpr)) CalculatorEngine.evaluate(newExpr) else ""
            state.copy(
                expression = newExpr,
                resultPreview = preview,
                isEvaluated = false
            )
        }
    }

    fun onOperatorClick(op: String) {
        _uiState.update { state ->
            if (state.expression.isEmpty() && op != "-") {
                return@update state
            }
            // If last char is operator, replace it
            val lastChar = state.expression.lastOrNull()
            val newExpr = if (lastChar != null && isOperatorChar(lastChar)) {
                state.expression.dropLast(1) + op
            } else {
                state.expression + op
            }
            state.copy(
                expression = newExpr,
                isEvaluated = false
            )
        }
    }

    fun onDecimalClick() {
        _uiState.update { state ->
            val expr = if (state.isEvaluated) "" else state.expression
            val lastNumberToken = expr.split("+", "-", "×", "÷", "%").lastOrNull() ?: ""
            if (!lastNumberToken.contains(".")) {
                val newExpr = if (expr.isEmpty() || isOperatorChar(expr.last())) expr + "0." else expr + "."
                state.copy(expression = newExpr, isEvaluated = false)
            } else {
                state
            }
        }
    }

    fun onPercentClick() {
        _uiState.update { state ->
            if (state.expression.isEmpty()) return@update state
            val newExpr = state.expression + "%"
            val eval = CalculatorEngine.evaluate(newExpr)
            state.copy(
                expression = eval,
                resultPreview = "",
                isEvaluated = true
            )
        }
    }

    fun onClearClick() {
        _uiState.update {
            it.copy(
                expression = "",
                resultPreview = "",
                isEvaluated = false
            )
        }
    }

    fun onBackspaceClick() {
        _uiState.update { state ->
            if (state.expression.isNotEmpty()) {
                val newExpr = state.expression.dropLast(1)
                val preview = if (hasOperators(newExpr)) CalculatorEngine.evaluate(newExpr) else ""
                state.copy(
                    expression = newExpr,
                    resultPreview = preview,
                    isEvaluated = false
                )
            } else {
                state
            }
        }
    }

    fun onEqualClick(onUnlock: () -> Unit, onTriggerIntruderCapture: () -> Unit = {}) {
        val currentExpr = _uiState.value.expression.trim()
        val currentStep = _uiState.value.pinSetupStep

        // Check for recovery code
        if (currentExpr == VaultSecurityManager.RECOVERY_CODE || currentExpr == "11223344") {
            _uiState.update {
                it.copy(
                    expression = "",
                    resultPreview = "",
                    showRecoveryDialog = true
                )
            }
            return
        }

        when (currentStep) {
            PinSetupStep.ENTER_PIN -> {
                // If previous vault exists and entered PIN matches, restore directly!
                if (securityManager.verifyPin(currentExpr)) {
                    _uiState.update {
                        it.copy(
                            expression = "",
                            resultPreview = "",
                            pinSetupStep = PinSetupStep.COMPLETED,
                            statusMessage = "Vault restored successfully!"
                        )
                    }
                    onUnlock()
                    return
                }

                if (currentExpr.length >= 4 && currentExpr.all { it.isDigit() }) {
                    candidatePin = currentExpr
                    _uiState.update {
                        it.copy(
                            expression = "",
                            resultPreview = "",
                            pinSetupStep = PinSetupStep.CONFIRM_PIN,
                            statusMessage = "Confirm PIN ($currentExpr) and press ="
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            statusMessage = "PIN must be at least 4 digits. Press ="
                        )
                    }
                }
            }

            PinSetupStep.CONFIRM_PIN -> {
                if (currentExpr == candidatePin) {
                    securityManager.setupPin(candidatePin)
                    if (!securityManager.hasSecurityQuestion()) {
                        securityManager.setSecurityQuestion("What is your favorite color?", "blue")
                    }
                    _uiState.update {
                        it.copy(
                            expression = "",
                            resultPreview = "",
                            pinSetupStep = PinSetupStep.COMPLETED,
                            statusMessage = "PIN saved successfully!"
                        )
                    }
                    onUnlock()
                } else {
                    candidatePin = ""
                    _uiState.update {
                        it.copy(
                            expression = "",
                            resultPreview = "",
                            pinSetupStep = PinSetupStep.ENTER_PIN,
                            statusMessage = "PINs did not match. Set PIN and press ="
                        )
                    }
                }
            }

            PinSetupStep.COMPLETED, PinSetupStep.NOT_STARTED -> {
                // Check if Emergency Self-Destruct Panic PIN was entered
                if (currentExpr.length >= 4 && securityManager.verifyPanicPin(currentExpr)) {
                    viewModelScope.launch {
                        securityManager.executeSelfDestructWipe()
                    }
                    // Act strictly as normal calculator! No unlock, no suspicious dialogs!
                    val result = CalculatorEngine.evaluate(currentExpr)
                    val historyEntry = "$currentExpr = $result"
                    _uiState.update {
                        val updatedHistory = listOf(historyEntry) + it.calculationHistory
                        it.copy(
                            expression = result,
                            resultPreview = "",
                            isEvaluated = true,
                            calculationHistory = updatedHistory.take(50)
                        )
                    }
                    return
                }

                // Check if PIN matches
                if (currentExpr.length >= 4 && securityManager.verifyPin(currentExpr)) {
                    intruderManager?.resetFailedAttempts()
                    _uiState.update {
                        it.copy(
                            expression = "",
                            resultPreview = "",
                            isEvaluated = false
                        )
                    }
                    onUnlock()
                } else {
                    // Check if 4 digit PIN attempt failed
                    if (currentExpr.length == 4 && currentExpr.all { it.isDigit() }) {
                        val failed = intruderManager?.recordFailedAttempt() ?: 0
                        if (failed >= 3) {
                            onTriggerIntruderCapture()
                        }
                    }

                    // Behave strictly as normal calculator!
                    val result = CalculatorEngine.evaluate(currentExpr)
                    val historyEntry = "$currentExpr = $result"
                    _uiState.update {
                        val updatedHistory = listOf(historyEntry) + it.calculationHistory
                        it.copy(
                            expression = result,
                            resultPreview = "",
                            isEvaluated = true,
                            calculationHistory = updatedHistory.take(50)
                        )
                    }
                }
            }
        }
    }

    fun openHistoryDialog() {
        _uiState.update { it.copy(showHistoryDialog = true) }
    }

    fun dismissHistoryDialog() {
        _uiState.update { it.copy(showHistoryDialog = false) }
    }

    fun clearHistory() {
        _uiState.update { it.copy(calculationHistory = emptyList()) }
    }

    fun openRecoveryDialog() {
        _uiState.update { it.copy(showRecoveryDialog = true) }
    }

    fun dismissRecoveryDialog() {
        _uiState.update { it.copy(showRecoveryDialog = false) }
    }

    fun handleRecoveryReset(newPin: String) {
        securityManager.setupPin(newPin)
        _uiState.update {
            it.copy(
                showRecoveryDialog = false,
                pinSetupStep = PinSetupStep.COMPLETED,
                statusMessage = "PIN reset successfully! Enter $newPin and press ="
            )
        }
    }

    private fun hasOperators(expr: String): Boolean {
        return expr.any { it == '+' || it == '-' || it == '×' || it == '÷' }
    }

    private fun isOperatorChar(c: Char): Boolean {
        return c == '+' || c == '-' || c == '×' || c == '÷'
    }

    class Factory(
        private val securityManager: VaultSecurityManager,
        private val intruderManager: IntruderManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalculatorViewModel(securityManager, intruderManager) as T
        }
    }
}
