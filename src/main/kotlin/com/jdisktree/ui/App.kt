package com.jdisktree.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jdisktree.domain.TreeMapRect
import com.jdisktree.state.ScanStatus
import com.jdisktree.state.UiState
import com.jdisktree.viewmodel.ScanViewModel

@Composable
fun App(
    isDarkTheme: Boolean = true,
    showTypeStatsInitial: Boolean = true,
    systemAccentColorHex: String? = null,
    onThemeToggle: () -> Unit = {},
    onStatsToggle: () -> Unit = {},
    onExit: () -> Unit = {}
) {
        var uiState by remember { mutableStateOf(UiState.idle()) }
        val viewModel = remember { ScanViewModel { newState -> uiState = newState } }
        var pathText by remember { mutableStateOf("C:\\") }
        var hoveredRect by remember { mutableStateOf<TreeMapRect?>(null) }
        var mousePosition by remember { mutableStateOf(Offset.Zero) }
        var selectedPath by remember { mutableStateOf<String?>(null) }
        var contextMenuPath by remember { mutableStateOf<String?>(null) }
        var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
        var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
        var showProperties by remember { mutableStateOf<String?>(null) }
        var highlightedExtension by remember { mutableStateOf<String?>(null) }
        var showTypeStats by remember { mutableStateOf(showTypeStatsInitial) }

        val systemAccentColor = remember(systemAccentColorHex) {
            systemAccentColorHex?.let {
                try { Color(it.toLong(16)) } catch (e: Exception) { null }
            }
        }

        val onAccentColor = systemAccentColor?.let { getContrastColor(it) } ?: Color.White

        val themeColors = if (isDarkTheme) {
            darkColors(
                primary = systemAccentColor ?: darkColors().primary,
                onPrimary = if (systemAccentColor != null) onAccentColor else darkColors().onPrimary,
                secondary = systemAccentColor ?: darkColors().secondary,
                onSecondary = if (systemAccentColor != null) onAccentColor else darkColors().onSecondary
            )
        } else {
            lightColors(
                primary = systemAccentColor ?: lightColors().primary,
                onPrimary = if (systemAccentColor != null) onAccentColor else lightColors().onPrimary,
                secondary = systemAccentColor ?: lightColors().secondary,
                onSecondary = if (systemAccentColor != null) onAccentColor else lightColors().onSecondary
            )
        }

        MaterialTheme(colors = themeColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start
            ) {
                // Modern Custom Menu Bar
                MainMenu(
                    isDarkTheme = isDarkTheme,
                    showStats = showTypeStats,
                    onThemeToggle = onThemeToggle,
                    onStatsToggle = onStatsToggle,
                    onNewScan = { 
                        DirectoryPicker.pickDirectory()?.let { pathText = it }
                    },
                    onExit = onExit
                )

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Toolbar(
                        uiState = uiState,
                        pathText = pathText,
                        onPathChange = { pathText = it },
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    StatusBanner(uiState)
                    Spacer(modifier = Modifier.height(16.dp))

                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, Color.Gray)) {
                        if (uiState.rects().isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Left Panel: Synchronized File Tree View
                                Box(modifier = Modifier.weight(0.25f).fillMaxHeight().border(1.dp, Color.DarkGray)) {
                                    FileTreeView(
                                        rootNode = uiState.rootNode(),
                                        selectedPath = selectedPath,
                                        onSelect = { selectedPath = it },
                                        onSecondaryClick = { path: String, offset: Offset ->
                                            contextMenuPath = path
                                            contextMenuOffset = offset
                                        }
                                    )
                                }

                                // Middle Panel: Treemap Canvas
                                val treemapWeight = if (showTypeStats) 0.5f else 0.75f
                                Box(modifier = Modifier.weight(treemapWeight).fillMaxHeight()) {
                                    TreemapCanvas(
                                        rects = uiState.rects(),
                                        index = uiState.index(),
                                        selectedPath = selectedPath,
                                        highlightedExtension = highlightedExtension,
                                        baseWidth = 1000.0,
                                        baseHeight = 1000.0,
                                        onHover = { rect, pos -> 
                                            hoveredRect = rect
                                            mousePosition = pos
                                        },
                                        onClick = { path ->
                                            selectedPath = path
                                        },
                                        onSecondaryClick = { path, offset ->
                                            contextMenuPath = path
                                            contextMenuOffset = offset
                                        }
                                    )
                                    
                                    hoveredRect?.let { rect ->
                                        Tooltip(rect, mousePosition)
                                    }
                                }

                                // Right Panel: Extension Statistics
                                if (showTypeStats) {
                                    Box(modifier = Modifier.weight(0.25f).fillMaxHeight().border(1.dp, Color.DarkGray)) {
                                        FileTypePanel(
                                            stats = uiState.typeStats(),
                                            selectedExtension = highlightedExtension,
                                            onSelect = { highlightedExtension = it }
                                        )
                                    }
                                }
                            }
                        } else if (uiState.status() == ScanStatus.SCANNING || uiState.status() == ScanStatus.CALCULATING_TREEMAP) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else if (uiState.status() == ScanStatus.IDLE) {
                            Text(stringResource("idle_prompt"), modifier = Modifier.align(Alignment.Center))
                        }

                        // Global Context Menu
                        contextMenuPath?.let { path ->
                            val density = LocalDensity.current
                            val offsetX = with(density) { contextMenuOffset.x.toDp() }
                            val offsetY = with(density) { contextMenuOffset.y.toDp() }

                            Box(modifier = Modifier.offset(offsetX, offsetY)) {
                                FileContextMenu(
                                    path = path,
                                    onDismiss = { contextMenuPath = null },
                                    onOpenExplorer = { viewModel.openInExplorer(path) },
                                    onCopyPath = { viewModel.copyPath(path) },
                                    onMoveToTrash = { viewModel.moveToTrash(path, 1000.0, 1000.0) },
                                    onDeletePermanently = { showDeleteConfirm = path },
                                    onShowProperties = { showProperties = path }
                                )
                            }
                        }                    }
                }
            }
        }

        // Dialogs
        showDeleteConfirm?.let { path ->
            DeleteConfirmationDialog(
                path = path,
                onConfirm = { 
                    viewModel.deletePermanently(path, 1000.0, 1000.0)
                    showDeleteConfirm = null
                },
                onDismiss = { showDeleteConfirm = null }
            )
        }

        showProperties?.let { path ->
            PropertiesDialog(
                path = path,
                onDismiss = { showProperties = null }
            )
        }
    }
}
