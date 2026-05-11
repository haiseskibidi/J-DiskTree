package com.jdisktree.ui

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DeleteConfirmationDialog(
    paths: Set<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = if (paths.size == 1) {
        stringResource("delete_confirm_text", paths.first())
    } else {
        "${stringResource("delete_confirm_title")} (${paths.size} ${stringResource("prop_files")})"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("delete_confirm_title")) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
            ) {
                Text(stringResource("delete_button"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel_button"))
            }
        }
    )
}
