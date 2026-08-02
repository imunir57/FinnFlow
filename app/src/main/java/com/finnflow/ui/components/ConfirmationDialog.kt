package com.finnflow.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.finnflow.ui.theme.FinnFlowTheme

/**
 * Shared confirmation dialog for destructive settings actions (restore, sign out, etc).
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = "Continue",
    dismissLabel: String = "Cancel",
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
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        }
    )
}
