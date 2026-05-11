package com.jdisktree.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jdisktree.domain.FileColorConfig
import com.jdisktree.domain.ScanExclusion
import javax.swing.JColorChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager
import java.awt.Color as AwtColor

enum class SettingsTab {
    EXCLUSIONS, COLORS
}

private fun showColorPicker(initialColorHex: String, isDarkTheme: Boolean, onColorSelected: (String) -> Unit) {
    SwingUtilities.invokeLater {
        try {
            if (isDarkTheme) {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf")
            } else {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf")
            }
        } catch (e: Exception) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) } catch (ex: Exception) {}
        }
        val initialAwtColor = try {
            val hex = if (initialColorHex.length == 8) initialColorHex.substring(2) else initialColorHex
            AwtColor(hex.toInt(16))
        } catch (e: Exception) {
            AwtColor.RED
        }
        
        val chooser = JColorChooser(initialAwtColor)
        val panels = chooser.chooserPanels
        for (panel in panels) {
            if (panel.javaClass.name.contains("HSV", ignoreCase = true)) {
                chooser.selectionModel.selectedColor = initialAwtColor
                try {
                    val field = JColorChooser::class.java.getDeclaredField("chooserPanels")
                    field.isAccessible = true
                    chooser.setChooserPanels(arrayOf(panel))
                    chooser.setChooserPanels(panels)
                } catch (e: Exception) {}
                break
            }
        }

        val newColor = JColorChooser.showDialog(null, "Select Color", initialAwtColor)
        if (newColor != null) {
            val hex = String.format("%02X%02X%02X", newColor.red, newColor.green, newColor.blue)
            onColorSelected(hex)
        }
    }
}

@Composable
fun SettingsDialog(
    initialExclusions: List<ScanExclusion>,
    initialColors: List<FileColorConfig>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<ScanExclusion>, List<FileColorConfig>) -> Unit
) {
    var selectedTab by remember { mutableStateOf(SettingsTab.EXCLUSIONS) }
    var exclusions by remember { mutableStateOf(initialExclusions) }
    var colors by remember { mutableStateOf(initialColors) }
    
    val strings = LocalStrings.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.size(750.dp, 500.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.background,
            elevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(strings.get("settings_menu"), style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                }
                
                Divider()

                Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                    Column(
                        modifier = Modifier
                            .width(160.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colors.surface.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        TabMenuItem(
                            text = strings.get("tab_exclusions"),
                            isSelected = selectedTab == SettingsTab.EXCLUSIONS,
                            onClick = { selectedTab = SettingsTab.EXCLUSIONS }
                        )
                        TabMenuItem(
                            text = strings.get("tab_colors"),
                            isSelected = selectedTab == SettingsTab.COLORS,
                            onClick = { selectedTab = SettingsTab.COLORS }
                        )
                    }
                    
                    Divider(modifier = Modifier.width(1.dp).fillMaxHeight())

                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                        when (selectedTab) {
                            SettingsTab.EXCLUSIONS -> {
                                ExclusionsPanel(
                                    exclusions = exclusions,
                                    onUpdate = { exclusions = it }
                                )
                            }
                            SettingsTab.COLORS -> {
                                ColorsPanel(
                                    colors = colors,
                                    isDarkTheme = isDarkTheme,
                                    onUpdate = { colors = it }
                                )
                            }
                        }
                    }
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.surface).padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(strings.get("cancel_button"))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(exclusions, colors) }) {
                        Text(strings.get("save_button"))
                    }
                }
            }
        }
    }
}

@Composable
private fun TabMenuItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = textColor, style = MaterialTheme.typography.body2, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(if (checked) MaterialTheme.colors.primary else Color.Gray.copy(alpha = 0.5f))
    val thumbPosition by animateFloatAsState(if (checked) 1f else 0f)

    Box(
        modifier = modifier
            .size(width = 34.dp, height = 20.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .offset(x = (14.dp * thumbPosition))
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun ExclusionsPanel(
    exclusions: List<ScanExclusion>,
    onUpdate: (List<ScanExclusion>) -> Unit
) {
    val strings = LocalStrings.current
    var newExclusionText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(strings.get("exclusions_desc"), style = MaterialTheme.typography.caption, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newExclusionText,
                onValueChange = { newExclusionText = it },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp), // HeightIn instead of fixed height
                label = { Text(strings.get("new_exclusion")) },
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colors.onSurface, fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newExclusionText.isNotBlank()) {
                        val newList = exclusions.toMutableList()
                        newList.add(ScanExclusion(newExclusionText.trim(), true))
                        onUpdate(newList)
                        newExclusionText = ""
                    }
                },
                modifier = Modifier.height(52.dp),
                enabled = newExclusionText.isNotBlank()
            ) {
                Text(strings.get("add_button"))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val gridState = rememberLazyGridState()
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(exclusions.sortedBy { it.pattern }) { exclusion ->
                    ExclusionChip(
                        exclusion = exclusion,
                        onCheckedChange = { checked ->
                            val newList = exclusions.map {
                                if (it.pattern == exclusion.pattern) ScanExclusion(it.pattern, checked) else it
                            }
                            onUpdate(newList)
                        },
                        onDelete = {
                            val newList = exclusions.toMutableList()
                            newList.remove(exclusion)
                            onUpdate(newList)
                        }
                    )
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp),
                adapter = rememberScrollbarAdapter(scrollState = gridState)
            )
        }
    }
}

@Composable
private fun ExclusionChip(
    exclusion: ScanExclusion,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(42.dp),
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
        color = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomCheckbox(
                checked = exclusion.isEnabled,
                onCheckedChange = onCheckedChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = exclusion.pattern,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (exclusion.isEnabled) MaterialTheme.colors.onSurface else Color.Gray
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = "Delete", 
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f), 
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorsPanel(
    colors: List<FileColorConfig>,
    isDarkTheme: Boolean,
    onUpdate: (List<FileColorConfig>) -> Unit
) {
    val strings = LocalStrings.current
    var newExtension by remember { mutableStateOf("") }
    var newColor by remember { mutableStateOf("FF0000") }

    LaunchedEffect(newExtension) {
        val ext = newExtension.removePrefix(".").lowercase()
        val existing = colors.find { it.extension == ext }
        if (existing != null) {
            newColor = existing.hexColor.removePrefix("FF")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(strings.get("colors_desc"), style = MaterialTheme.typography.caption, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newExtension,
                onValueChange = { newExtension = it.lowercase() },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                label = { Text(strings.get("extension_label")) },
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colors.onSurface, fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            val composeColor = try { Color("FF$newColor".toLong(16)) } catch(e: Exception) { Color.Red }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(composeColor)
                    .clickable {
                        showColorPicker(newColor, isDarkTheme) { hex ->
                            newColor = hex
                        }
                    }
                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (newExtension.isNotBlank() && newColor.length == 6) {
                        val ext = newExtension.removePrefix(".")
                        val newList = colors.toMutableList()
                        newList.removeAll { it.extension == ext }
                        newList.add(FileColorConfig(ext, "FF$newColor"))
                        onUpdate(newList)
                        newExtension = ""
                        newColor = "FF0000"
                    }
                },
                modifier = Modifier.height(52.dp),
                enabled = newExtension.isNotBlank() && newColor.length == 6
            ) {
                Text(strings.get("add_button"))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val gridState = rememberLazyGridState()
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                val sortedColors = colors.sortedWith(compareBy<FileColorConfig> {
                    getColorSortValues(it.hexColor)[0] // Hue
                }.thenBy {
                    getColorSortValues(it.hexColor)[1] // Saturation
                }.thenBy {
                    getColorSortValues(it.hexColor)[2] // Brightness
                })
                items(sortedColors) { colorConfig ->
                    ColorChip(
                        colorConfig = colorConfig,
                        isDarkTheme = isDarkTheme,
                        onColorChange = { hex ->
                            val newList = colors.toMutableList()
                            val idx = newList.indexOf(colorConfig)
                            if (idx != -1) {
                                newList[idx] = FileColorConfig(colorConfig.extension, "FF$hex")
                                onUpdate(newList)
                            }
                        },
                        onDelete = {
                            val newList = colors.toMutableList()
                            newList.remove(colorConfig)
                            onUpdate(newList)
                        }
                    )
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp),
                adapter = rememberScrollbarAdapter(scrollState = gridState)
            )
        }
    }
}

@Composable
private fun ColorChip(
    colorConfig: FileColorConfig,
    isDarkTheme: Boolean,
    onColorChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val rowComposeColor = try {
        Color(colorConfig.hexColor.toLong(16))
    } catch (e: Exception) { Color.Gray }

    Surface(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
        color = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(rowComposeColor)
                    .clickable {
                        showColorPicker(colorConfig.hexColor, isDarkTheme, onColorChange)
                    }
                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                ".${colorConfig.extension}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = "Delete", 
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f), 
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getColorSortValues(hex: String): FloatArray {
    return try {
        val argb = hex.toLong(16).toInt()
        val c = AwtColor(argb, true)
        val hsb = FloatArray(3)
        AwtColor.RGBtoHSB(c.red, c.green, c.blue, hsb)
        hsb
    } catch (e: Exception) {
        floatArrayOf(0f, 0f, 0f)
    }
}
