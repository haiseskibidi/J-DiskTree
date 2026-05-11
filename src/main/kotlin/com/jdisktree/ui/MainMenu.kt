package com.jdisktree.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import java.awt.Desktop
import java.net.URI

@Composable
fun MainMenu(
    isDarkTheme: Boolean,
    showStats: Boolean,
    hasData: Boolean,
    onThemeToggle: () -> Unit,
    onStatsToggle: () -> Unit,
    onNewScan: () -> Unit,
    onExport: (String, java.io.File) -> Unit,
    onCompareSnapshot: (java.io.File) -> Unit,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit
) {
    val strings = LocalStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp) // Slightly more compact height
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuButton(strings.get("file_menu")) { expanded ->
            DropdownMenuItem(onClick = { onNewScan(); expanded.value = false },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(strings.get("new_scan"), style = MaterialTheme.typography.body2)
            }
            
            Divider(color = Color.Gray.copy(alpha = 0.2f))

            // Snapshots Menu
            var snapshotSubMenu by remember { mutableStateOf(false) }
            DropdownMenuItem(
                onClick = { if (hasData) snapshotSubMenu = !snapshotSubMenu },
                enabled = hasData,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.get("snapshots_menu"), style = MaterialTheme.typography.body2, color = if (hasData) Color.Unspecified else Color.Gray)
                    Text(if (snapshotSubMenu) "▼" else "▶", style = MaterialTheme.typography.caption, color = Color.Gray)
                }
            }

            if (snapshotSubMenu && hasData) {
                DropdownMenuItem(
                    onClick = {
                        pickSaveFile("Save Snapshot", "snapshot.json")?.let { file -> onExport("JSON", file) }
                        expanded.value = false
                    },
                    contentPadding = PaddingValues(start = 24.dp, end = 12.dp)
                ) {
                    Text(strings.get("save_snapshot"), style = MaterialTheme.typography.body2)
                }
                DropdownMenuItem(
                    onClick = {
                        pickOpenFile("json")?.let { file -> onCompareSnapshot(file) }
                        expanded.value = false
                    },
                    contentPadding = PaddingValues(start = 24.dp, end = 12.dp)
                ) {
                    Text(strings.get("compare_snapshot"), style = MaterialTheme.typography.body2)
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))

            // Export Submenu
            var exportSubMenu by remember { mutableStateOf(false) }
            DropdownMenuItem(
                onClick = { if (hasData) exportSubMenu = !exportSubMenu },
                enabled = hasData,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.get("export_menu"), style = MaterialTheme.typography.body2, color = if (hasData) Color.Unspecified else Color.Gray)
                    Text(if (exportSubMenu) "▼" else "▶", style = MaterialTheme.typography.caption, color = Color.Gray)
                }
            }

            if (exportSubMenu && hasData) {
                DropdownMenuItem(
                    onClick = {
                        pickSaveFile("Save Report", "report.csv")?.let { file -> onExport("CSV", file) }
                        expanded.value = false
                    },
                    contentPadding = PaddingValues(start = 24.dp, end = 12.dp)
                ) {
                    Text(strings.get("export_csv"), style = MaterialTheme.typography.body2)
                }
                DropdownMenuItem(
                    onClick = {
                        pickSaveFile("Save Report", "report.json")?.let { file -> onExport("JSON", file) }
                        expanded.value = false
                    },
                    contentPadding = PaddingValues(start = 24.dp, end = 12.dp)
                ) {
                    Text(strings.get("export_json"), style = MaterialTheme.typography.body2)
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))
            
            DropdownMenuItem(onClick = { onExit(); expanded.value = false },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(strings.get("exit"), style = MaterialTheme.typography.body2)
            }
        }

        MenuButton(strings.get("view_menu")) { expanded ->
            var languageSubMenu by remember { mutableStateOf(false) }

            DropdownMenuItem(onClick = { languageSubMenu = !languageSubMenu },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.get("language"), style = MaterialTheme.typography.body2)
                    Text(if (languageSubMenu) "▼" else "▶", style = MaterialTheme.typography.caption, color = Color.Gray)
                }
            }

            if (languageSubMenu) {
                Language.entries.forEach { lang ->
                    DropdownMenuItem(
                        onClick = { 
                            strings.setLanguage(lang)
                            expanded.value = false 
                        },
                        contentPadding = PaddingValues(start = 24.dp, end = 12.dp)
                    ) {
                        Text(lang.displayName, style = MaterialTheme.typography.body2)
                    }
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))

            DropdownMenuItem(onClick = { onThemeToggle(); expanded.value = false },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(
                    if (isDarkTheme) strings.get("light_theme") else strings.get("dark_theme"),
                    style = MaterialTheme.typography.body2
                )
            }

            DropdownMenuItem(onClick = { onStatsToggle(); expanded.value = false },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.get("show_stats"), style = MaterialTheme.typography.body2)
                    if (showStats) {
                        Text("✓", color = MaterialTheme.colors.primary, style = MaterialTheme.typography.body2)
                    }
                }
            }
        }

        MenuButton(strings.get("settings_menu")) { expanded ->
            DropdownMenuItem(onClick = { onOpenSettings(); expanded.value = false },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(strings.get("settings_menu"), style = MaterialTheme.typography.body2)
            }
        }

        MenuButton(strings.get("help_menu")) { expanded ->
            DropdownMenuItem(
                onClick = {
                    try {
                        Desktop.getDesktop().browse(URI("https://github.com/haiseskibidi/J-DiskTree"))
                    } catch (e: Exception) { e.printStackTrace() }
                    expanded.value = false
                },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(strings.get("github"), style = MaterialTheme.typography.body2)
            }
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    content: @Composable (MutableState<Boolean>) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxHeight()) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxHeight()
                .clickable { expanded.value = true }
                .wrapContentHeight(Alignment.CenterVertically)
                .padding(horizontal = 10.dp),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )

        if (expanded.value) {
            Popup(
                onDismissRequest = { expanded.value = false },
                properties = PopupProperties(focusable = true),
                offset = IntOffset(0, 28) // Force it to open exactly at the bottom edge
            ) {
                Surface(
                    elevation = 8.dp,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colors.surface,
                    modifier = Modifier.width(IntrinsicSize.Max).widthIn(min = 160.dp)
                ) {
                    Column {
                        content(expanded)
                    }
                }
            }
        }
    }
}

private fun pickSaveFile(title: String, defaultFileName: String): java.io.File? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.SAVE)
    dialog.file = defaultFileName
    dialog.isVisible = true
    return if (dialog.file != null) java.io.File(dialog.directory, dialog.file) else null
}

private fun pickOpenFile(extension: String): java.io.File? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Open Snapshot", java.awt.FileDialog.LOAD)
    dialog.file = "*.$extension"
    dialog.isVisible = true
    return if (dialog.file != null) java.io.File(dialog.directory, dialog.file) else null
}
