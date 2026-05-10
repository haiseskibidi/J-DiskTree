package com.jdisktree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FileContextMenu(
    path: String,
    onDismiss: () -> Unit,
    onOpenExplorer: () -> Unit,
    onCopyPath: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDeletePermanently: () -> Unit,
    onShowProperties: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(200.dp)
    ) {
        DropdownMenuItem(onClick = { onOpenExplorer(); onDismiss() }) {
            Text("Open in Explorer", style = MaterialTheme.typography.body2)
        }
        DropdownMenuItem(onClick = { onCopyPath(); onDismiss() }) {
            Text("Copy Path", style = MaterialTheme.typography.body2)
        }
        Divider(color = Color.Gray.copy(alpha = 0.3f))
        DropdownMenuItem(onClick = { onMoveToTrash(); onDismiss() }) {
            Text("Move to Recycle Bin", style = MaterialTheme.typography.body2)
        }
        DropdownMenuItem(onClick = { onDeletePermanently(); onDismiss() }) {
            Text("Delete Permanently", color = Color(0xFFE57373), style = MaterialTheme.typography.body2)
        }
        Divider(color = Color.Gray.copy(alpha = 0.3f))
        DropdownMenuItem(onClick = { onShowProperties(); onDismiss() }) {
            Text("Properties", style = MaterialTheme.typography.body2)
        }
    }
}
