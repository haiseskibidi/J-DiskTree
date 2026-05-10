package com.jdisktree.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jdisktree.domain.TreeMapRect
import com.jdisktree.state.ScanStatus
import com.jdisktree.state.UiState
import com.jdisktree.viewmodel.ScanViewModel

@Composable
fun App() {
    var uiState by remember { mutableStateOf(UiState.idle()) }
    val viewModel = remember { ScanViewModel { newState -> uiState = newState } }
    var pathText by remember { mutableStateOf("C:\\") }
    var hoveredRect by remember { mutableStateOf<TreeMapRect?>(null) }
    var mousePosition by remember { mutableStateOf(Offset.Zero) }
    var selectedPath by remember { mutableStateOf<String?>(null) }

    MaterialTheme(colors = darkColors()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
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

                Box(modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, Color.Gray)) {
                    if (uiState.rects().isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Panel: Synchronized File Tree View
                            Box(modifier = Modifier.weight(0.3f).fillMaxHeight().border(1.dp, Color.DarkGray)) {
                                FileTreeView(
                                    rootNode = uiState.rootNode(),
                                    selectedPath = selectedPath,
                                    onSelect = { selectedPath = it }
                                )
                            }

                            // Right Panel: Treemap Canvas
                            Box(modifier = Modifier.weight(0.7f).fillMaxHeight()) {
                                TreemapCanvas(
                                    rects = uiState.rects(),
                                    index = uiState.index(),
                                    onHover = { rect, pos -> 
                                        hoveredRect = rect
                                        mousePosition = pos
                                    },
                                    onClick = { path ->
                                        selectedPath = path
                                    }
                                )
                                
                                hoveredRect?.let { rect ->
                                    Tooltip(rect, mousePosition)
                                }
                            }
                        }
                    } else if (uiState.status() == ScanStatus.SCANNING || uiState.status() == ScanStatus.CALCULATING_TREEMAP) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (uiState.status() == ScanStatus.IDLE) {
                        Text("Select a directory and press 'Scan'", modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}
