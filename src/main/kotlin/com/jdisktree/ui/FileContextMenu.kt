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
        modifier = Modifier.widthIn(min = 180.dp)
    ) {
        DropdownMenuItem(
            onClick = { onOpenExplorer(); onDismiss() },
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(stringResource("open_explorer"), style = MaterialTheme.typography.body2)
        }
        DropdownMenuItem(
            onClick = { onCopyPath(); onDismiss() },
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(stringResource("copy_path"), style = MaterialTheme.typography.body2)
        }
        Divider(color = Color.Gray.copy(alpha = 0.3f))
        DropdownMenuItem(
            onClick = { onMoveToTrash(); onDismiss() },
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(stringResource("move_to_trash"), style = MaterialTheme.typography.body2)
        }
        DropdownMenuItem(
            onClick = { onDeletePermanently(); onDismiss() },
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(stringResource("delete_permanently"), color = Color(0xFFE57373), style = MaterialTheme.typography.body2)
        }
        Divider(color = Color.Gray.copy(alpha = 0.3f))
        DropdownMenuItem(
            onClick = { onShowProperties(); onDismiss() },
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(stringResource("properties"), style = MaterialTheme.typography.body2)
        }
    }
}
