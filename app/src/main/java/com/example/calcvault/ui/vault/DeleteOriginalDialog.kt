package com.example.calcvault.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calcvault.data.i18n.VaultStrings

@Composable
fun DeleteOriginalDialog(
    prompt: DeleteOriginalPrompt?,
    appLanguage: String = "bn",
    onConfirm: (DeleteOriginalPrompt) -> Unit,
    onDismiss: () -> Unit
) {
    if (prompt == null) return

    val itemLabel = if (prompt.isDocument) {
        if (prompt.count > 1) {
            if (appLanguage == "bn") "${prompt.count} টি ডকুমেন্ট" else "${prompt.count} documents"
        } else {
            if (appLanguage == "bn") "ডকুমেন্ট" else "document"
        }
    } else if (prompt.isVideo) {
        if (prompt.count > 1) {
            if (appLanguage == "bn") "${prompt.count} টি ভিডিও" else "${prompt.count} videos"
        } else {
            if (appLanguage == "bn") "ভিডিও" else "video"
        }
    } else {
        if (prompt.count > 1) {
            if (appLanguage == "bn") "${prompt.count} টি ছবি" else "${prompt.count} photos"
        } else {
            if (appLanguage == "bn") "ছবি" else "photo"
        }
    }

    val sourceLocation = if (prompt.isDocument) {
        VaultStrings.get(appLanguage, "delete_orig_loc_storage")
    } else {
        VaultStrings.get(appLanguage, "delete_orig_loc_gallery")
    }

    val titleFormatted = String.format(
        VaultStrings.get(appLanguage, "delete_orig_title"),
        sourceLocation
    )
    val desc1Formatted = String.format(
        VaultStrings.get(appLanguage, "delete_orig_desc1"),
        itemLabel
    )
    val desc2Formatted = VaultStrings.get(appLanguage, "delete_orig_desc2")
    val confirmText = VaultStrings.get(appLanguage, "delete_orig_confirm")
    val keepText = VaultStrings.get(appLanguage, "delete_orig_keep")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .testTag("delete_original_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF1E293B),
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0x28EF4444),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete Original",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = titleFormatted,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Success Badge Card
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = desc1Formatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = desc2Formatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Start,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Vertical Stacked Action Buttons to prevent any border breaking or horizontal text clipping
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onConfirm(prompt) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dialog_delete_original_confirm")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = confirmText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFCBD5E1)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dialog_delete_original_cancel")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = keepText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
