package com.jdisktree.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jdisktree.domain.TreeMapRect
import com.jdisktree.state.ScanStatus
import com.jdisktree.state.UiState
import com.jdisktree.viewmodel.ScanViewModel

enum class ResizingSide { NONE, TREE, STATS }

@Composable
fun App(
    isDarkTheme: Boolean = true,
    showTypeStatsInitial: Boolean = true,
    treeWeightInitial: Float = 0.25f,
    statsWeightInitial: Float = 0.25f,
    systemAccentColorHex: String? = null,
    onThemeToggle: () -> Unit = {},
    onStatsToggle: () -> Unit = {},
    onWeightsChange: (Float, Float) -> Unit = { _, _ -> },
    onExit: () -> Unit = {}
) {
    var uiState by remember { mutableStateOf(UiState.idle()) }
    val viewModel = remember { ScanViewModel { newState -> uiState = newState } }
    var pathText by remember { mutableStateOf("C:\\") }
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var contextMenuPath by remember { mutableStateOf<String?>(null) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showProperties by remember { mutableStateOf<String?>(null) }
    var highlightedExtension by remember { mutableStateOf<String?>(null) }
    var showTypeStats by remember { mutableStateOf(showTypeStatsInitial) }
    
    // Panel weights
    var treeWeight by remember { mutableStateOf(treeWeightInitial) }
    var statsWeight by remember { mutableStateOf(statsWeightInitial) }
    var resizingSide by remember { mutableStateOf(ResizingSide.NONE) }

    // --- STABLE CALLBACKS ---
    val onWeightsChangeState = rememberUpdatedState(onWeightsChange)
    val onSelect = remember { { path: String -> selectedPath = path } }
    val onSecondaryClick = remember { { path: String, offset: Offset ->
        contextMenuPath = path
        contextMenuOffset = offset
    } }
    val onStatsSelect = remember { { ext: String? -> highlightedExtension = ext } }

    // Theme logic
    val systemAccentColor = remember(systemAccentColorHex) {
        systemAccentColorHex?.let {
            try { Color(it.toLong(16)) } catch (e: Exception) { null }
        }
    }
    val onAccentColor = systemAccentColor?.let { getContrastColor(it) } ?: Color.White
    val themeColors = remember(isDarkTheme, systemAccentColor) {
        if (isDarkTheme) {
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
    }

    MaterialTheme(colors = themeColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Custom Menu Bar
                MainMenu(
                    isDarkTheme = isDarkTheme,
                    showStats = showTypeStats,
                    onThemeToggle = onThemeToggle,
                    onStatsToggle = { 
                        showTypeStats = !showTypeStats
                        onStatsToggle()
                    },
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

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray)
                    ) {
                        val totalWidth = constraints.maxWidth.toFloat()
                        
                        if (uiState.rects().isNotEmpty()) {
                            @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onPointerEvent(PointerEventType.Move) { event ->
                                        if (resizingSide != ResizingSide.NONE) {
                                            val mouseX = event.changes.first().position.x
                                            if (resizingSide == ResizingSide.TREE) {
                                                treeWeight = (mouseX / totalWidth).coerceIn(0.1f, 0.5f)
                                            } else if (resizingSide == ResizingSide.STATS) {
                                                statsWeight = ((totalWidth - mouseX) / totalWidth).coerceIn(0.1f, 0.5f)
                                            }
                                        }
                                    }
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    // 1. File Tree Panel
                                    Box(modifier = Modifier.weight(treeWeight).fillMaxHeight().border(1.dp, Color.DarkGray)) {
                                        key(uiState.rootNode()) {
                                            FileTreeView(
                                                rootNode = uiState.rootNode(),
                                                selectedPath = selectedPath,
                                                onSelect = onSelect,
                                                onSecondaryClick = onSecondaryClick
                                            )
                                        }
                                    }

                                    VerticalSplitter(
                                        onDragStart = { resizingSide = ResizingSide.TREE },
                                        onDragEnd = { 
                                            resizingSide = ResizingSide.NONE
                                            onWeightsChangeState.value(treeWeight, statsWeight)
                                        }
                                    )

                                    // 2. Treemap Panel (ISOLATED)
                                    val treemapWeight = 1f - treeWeight - (if (showTypeStats) statsWeight else 0f)
                                    Box(modifier = Modifier.weight(treemapWeight).fillMaxHeight()) {
                                        key(uiState.rects(), highlightedExtension, selectedPath) {
                                            TreemapWithTooltip(
                                                rects = uiState.rects(),
                                                index = uiState.index(),
                                                selectedPath = selectedPath,
                                                highlightedExtension = highlightedExtension,
                                                isResizing = resizingSide != ResizingSide.NONE,
                                                onSelect = onSelect,
                                                onSecondaryClick = onSecondaryClick
                                            )
                                        }
                                    }

                                    if (showTypeStats) {
                                        VerticalSplitter(
                                            onDragStart = { resizingSide = ResizingSide.STATS },
                                            onDragEnd = { 
                                                resizingSide = ResizingSide.NONE
                                                onWeightsChangeState.value(treeWeight, statsWeight)
                                            }
                                        )

                                        // 3. Statistics Panel
                                        Box(modifier = Modifier.weight(statsWeight).fillMaxHeight().border(1.dp, Color.DarkGray)) {
                                            key(uiState.typeStats()) {
                                                FileTypePanel(
                                                    stats = uiState.typeStats(),
                                                    selectedExtension = highlightedExtension,
                                                    onSelect = onStatsSelect
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (uiState.status() == ScanStatus.SCANNING || uiState.status() == ScanStatus.CALCULATING_TREEMAP) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (uiState.status() == ScanStatus.IDLE) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource("idle_prompt"))
                            }
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
                        }
                    }
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
