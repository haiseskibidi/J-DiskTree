package com.jdisktree.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import com.jdisktree.domain.DiffNode
import com.jdisktree.domain.DiffStatus
import com.jdisktree.domain.FileNode
import com.jdisktree.state.UiState
import java.nio.file.Paths
import com.jdisktree.domain.FileColorConfig
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

data class FlatNode(
    val path: String,
    val name: String,
    val size: Long,
    val deltaSize: Long = 0,
    val isDirectory: Boolean,
    val level: Int,
    val isExpanded: Boolean,
    val diffStatus: DiffStatus? = null
)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun FileTreeView(
    stableRoot: StableFileTree,
    uiState: UiState,
    customColors: List<FileColorConfig> = emptyList(),
    onSelect: (String?, Boolean, Boolean, List<String>) -> Unit, // path, isCtrl, isShift, allVisiblePaths
    onToggleExpansion: (String) -> Unit,
    onSecondaryClick: (Set<String>, Offset) -> Unit
) {
    val rootNode = stableRoot.root
    val diffRoot = uiState.diffNode()
    val selectedPaths = uiState.selectedPaths()
    val expandedPaths = uiState.expandedPaths()
    
    if (rootNode == null && diffRoot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource("no_data"), color = Color.Gray)
        }
        return
    }

    // Auto-expand when a single new path is selected (e.g. from treemap)
    LaunchedEffect(selectedPaths) {
        if (selectedPaths.size == 1) {
            val path = selectedPaths.first()
            var currentPath = try { Paths.get(path).parent } catch (e: Exception) { null }
            while (currentPath != null) {
                val pathStr = currentPath.toAbsolutePath().toString()
                if (!expandedPaths.contains(pathStr)) {
                    onToggleExpansion(pathStr)
                }
                currentPath = currentPath.parent
            }
        }
    }

    // Flatten the tree for LazyColumn
    val flatNodes = remember(rootNode, diffRoot, expandedPaths) {
        val result = mutableListOf<FlatNode>()
        
        if (diffRoot != null) {
            fun traverseDiff(node: DiffNode, level: Int) {
                val isExpanded = expandedPaths.contains(node.absolutePath)
                result.add(FlatNode(node.absolutePath, node.name, node.currentSize, node.deltaSize, node.isDirectory, level, isExpanded, node.status))

                if (isExpanded && node.isDirectory) {
                    node.children.sortedWith(compareByDescending<DiffNode> { it.deltaSize != 0L }
                        .thenByDescending { Math.abs(it.deltaSize) }
                        .thenByDescending { it.currentSize }
                    ).forEach { child ->
                        traverseDiff(child, level + 1)
                    }
                }
            }
            traverseDiff(diffRoot, 0)
        } else if (rootNode != null) {
            fun traverse(node: FileNode, level: Int) {
                val isExpanded = expandedPaths.contains(node.absolutePath())
                result.add(FlatNode(node.absolutePath(), node.name(), node.size(), 0, node.isDirectory, level, isExpanded))

                if (isExpanded && node.isDirectory) {
                    node.children().sortedByDescending { it.size() }.forEach { child ->
                        traverse(child, level + 1)
                    }
                }
            }
            traverse(rootNode, 0)
        }
        result
    }
    
    val allVisiblePaths = remember(flatNodes) { flatNodes.map { it.path } }

    val listState = rememberLazyListState()

    // Simple Centered Scroll
    LaunchedEffect(selectedPaths, flatNodes) {
        if (selectedPaths.size == 1) {
            val path = selectedPaths.first()
            val fileIndex = flatNodes.indexOfFirst { it.path == path }
            if (fileIndex >= 0) {
                val layoutInfo = listState.layoutInfo
                val visibleItemsCount = layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 20
                
                val targetIndex = (fileIndex - (visibleItemsCount / 2)).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState, 
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp)
                .pointerInput(Unit) {
                    detectTapGestures {
                        onSelect(null, false, false, allVisiblePaths)
                    }
                }
        ) {
            items(flatNodes, key = { it.path + it.level }) { flatNode ->
                val isSelected = selectedPaths.contains(flatNode.path)
                var rowPosition by remember { mutableStateOf(Offset.Zero) }
                
                // Status colors
                val statusColor = when (flatNode.diffStatus) {
                    DiffStatus.ADDED -> Color(0xFF2ECC71)
                    DiffStatus.REMOVED -> Color(0xFFE74C3C)
                    DiffStatus.MODIFIED -> Color(0xFFF1C40F)
                    else -> null
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .clip(RoundedCornerShape(Dimens.RadiusMedium))
                        .background(if (isSelected) AppColors.SelectionGreen.copy(alpha = 0.4f) else Color.Transparent)
                        .onGloballyPositioned { rowPosition = it.positionInWindow() }
                        .onPointerEvent(PointerEventType.Press) { event ->
                            val change = event.changes.first()
                            if (change.pressed) {
                                val isCtrl = event.keyboardModifiers.isCtrlPressed
                                val isShift = event.keyboardModifiers.isShiftPressed
                                
                                if (event.buttons.isSecondaryPressed) {
                                    val finalSelection = if (isSelected) {
                                        selectedPaths
                                    } else {
                                        onSelect(flatNode.path, false, false, allVisiblePaths)
                                        setOf(flatNode.path)
                                    }
                                    onSecondaryClick(finalSelection, rowPosition + change.position)
                                } else {
                                    if (flatNode.isDirectory && !isCtrl && !isShift) {
                                        onToggleExpansion(flatNode.path)
                                    }
                                    onSelect(flatNode.path, isCtrl, isShift, allVisiblePaths)
                                }
                                change.consume()
                            }
                        }
                        .padding(start = (flatNode.level * 16 + 4).dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (flatNode.isDirectory) {
                        Text(
                            text = if (flatNode.isExpanded) "▼" else "▶",
                            color = statusColor ?: Color.LightGray,
                            modifier = Modifier.width(16.dp),
                            style = MaterialTheme.typography.caption
                        )
                    } else {
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Spacer(modifier = Modifier.width(Dimens.SpacingSmall))

                    val ext = if (flatNode.isDirectory) "dir_block" else {
                        val dotIndex = flatNode.name.lastIndexOf('.')
                        if (dotIndex > 0) flatNode.name.substring(dotIndex + 1) else ""
                    }
                    val color = if (statusColor != null) statusColor else getColorForExtension(ext, customColors)

                    Box(modifier = Modifier.size(Dimens.IconSmall).background(color, RoundedCornerShape(Dimens.RadiusSmall)))

                    Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

                    Text(
                        text = flatNode.name,
                        color = statusColor ?: Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body2
                    )

                    if (flatNode.deltaSize != 0L) {
                        Text(
                            text = (if (flatNode.deltaSize > 0) "+" else "") + formatSize(flatNode.deltaSize),
                            color = statusColor ?: Color.Gray,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatSize(flatNode.size),
                        color = Color.Gray,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
        
        androidx.compose.foundation.VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = androidx.compose.foundation.rememberScrollbarAdapter(scrollState = listState)
        )
    }
}
