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
    onThemeToggle: () -> Unit,
    onNewScan: () -> Unit,
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
            DropdownMenuItem(
                onClick = { onNewScan(); expanded.value = false },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(strings.get("new_scan"), style = MaterialTheme.typography.body2)
            }
            Divider(color = Color.Gray.copy(alpha = 0.2f))
            DropdownMenuItem(
                onClick = { onExit(); expanded.value = false },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(strings.get("exit"), style = MaterialTheme.typography.body2)
            }
        }

        MenuButton(strings.get("view_menu")) { expanded ->
            var languageSubMenu by remember { mutableStateOf(false) }

            DropdownMenuItem(
                onClick = { languageSubMenu = !languageSubMenu },
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

            DropdownMenuItem(
                onClick = { onThemeToggle(); expanded.value = false },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(
                    if (isDarkTheme) strings.get("light_theme") else strings.get("dark_theme"),
                    style = MaterialTheme.typography.body2
                )
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
