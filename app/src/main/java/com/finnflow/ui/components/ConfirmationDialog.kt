package com.finnflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.finnflow.ui.theme.FinnFlowTheme

/**
 * Shared confirmation dialog for destructive settings actions (restore, sign out, etc).
 *
 * [neutralLabel] adds a third, non-destructive escape hatch beside Cancel — used to offer a
 * backup before an action that erases data.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = "Continue",
    dismissLabel: String = "Cancel",
    neutralLabel: String? = null,
    onNeutral: () -> Unit = {},
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = FinnFlowTheme.colors.expense)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (neutralLabel != null) {
                    TextButton(onClick = onNeutral) {
                        Text(neutralLabel)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel)
                }
            }
        }
    )
}
