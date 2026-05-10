package com.jdisktree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PropertiesDialog(
    path: String,
    onDismiss: () -> Unit
) {
    val attributes = remember(path) {
        try {
            Files.readAttributes(Paths.get(path), BasicFileAttributes::class.java)
        } catch (e: Exception) {
            null
        }
    }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("properties")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PropertyRow(stringResource("prop_path"), path)
                if (attributes != null) {
                    PropertyRow(stringResource("prop_size"), formatSize(attributes.size()))
                    PropertyRow(stringResource("prop_created"), formatter.format(attributes.creationTime().toInstant()))
                    PropertyRow(stringResource("prop_modified"), formatter.format(attributes.lastModifiedTime().toInstant()))
                    PropertyRow(stringResource("prop_type"), if (attributes.isDirectory) stringResource("prop_dir") else stringResource("prop_file"))
                } else {
                    Text(stringResource("prop_error"), color = MaterialTheme.colors.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("close_button"))
            }
        }
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.caption, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
    }
}
