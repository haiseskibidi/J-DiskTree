package com.jdisktree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FileContextMenu(
    paths: Set<String>,
    onDismiss: () -> Unit,
    onOpenExplorer: (String) -> Unit,
    onCopyPath: (String) -> Unit,
    onMoveToTrash: (Set<String>) -> Unit,
    onDeletePermanently: (Set<String>) -> Unit,
    onShowProperties: (String) -> Unit
) {
    val singlePath = paths.firstOrNull()
    val isMultiple = paths.size > 1

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 180.dp)
    ) {
        if (!isMultiple && singlePath != null) {
            DropdownMenuItem(
                onClick = { onOpenExplorer(singlePath); onDismiss() },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(stringResource("open_explorer"), style = MaterialTheme.typography.body2)
            }
            DropdownMenuItem(
                onClick = { onCopyPath(singlePath); onDismiss() },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(stringResource("copy_path"), style = MaterialTheme.typography.body2)
            }
            Divider(color = Color.Gray.copy(alpha = 0.3f))
        }

        DropdownMenuItem(
            onClick = { onMoveToTrash(paths); onDismiss() },
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            val label = if (isMultiple) "${stringResource("move_to_trash")} (${paths.size})" else stringResource("move_to_trash")
            Text(label, style = MaterialTheme.typography.body2)
        }
        DropdownMenuItem(
            onClick = { onDeletePermanently(paths); onDismiss() },
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            val label = if (isMultiple) "${stringResource("delete_permanently")} (${paths.size})" else stringResource("delete_permanently")
            Text(label, color = Color(0xFFE57373), style = MaterialTheme.typography.body2)
        }

        if (!isMultiple && singlePath != null) {
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            DropdownMenuItem(
                onClick = { onShowProperties(singlePath); onDismiss() },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(stringResource("properties"), style = MaterialTheme.typography.body2)
            }
        }
    }
}
