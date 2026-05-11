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
import com.jdisktree.domain.FileNode
import java.nio.file.Paths

import com.jdisktree.domain.FileColorConfig

data class FlatNode(
    val fileNode: FileNode,
    val level: Int,
    val isExpanded: Boolean
)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun FileTreeView(
    stableRoot: StableFileTree,
    selectedPaths: Set<String>,
    selectionAnchor: String?,
    customColors: List<FileColorConfig> = emptyList(),
    onSelect: (String, Boolean, Boolean, List<String>) -> Unit, // path, isCtrl, isShift, allVisiblePaths
    onSecondaryClick: (Set<String>, Offset) -> Unit
) {
    val rootNode = stableRoot.root
    if (rootNode == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource("no_data"), color = Color.Gray)
        }
        return
    }

    var expandedPaths by remember { mutableStateOf(setOf(rootNode.absolutePath())) }

    // Auto-expand when a single new path is selected (e.g. from treemap)
    LaunchedEffect(selectedPaths) {
        if (selectedPaths.size == 1) {
            val path = selectedPaths.first()
            val newExpanded = expandedPaths.toMutableSet()
            var currentPath = Paths.get(path).parent
            while (currentPath != null) {
                val pathStr = currentPath.toAbsolutePath().toString()
                if (newExpanded.add(pathStr)) {
                    currentPath = currentPath.parent
                } else break
            }
            expandedPaths = newExpanded
        }
    }

    // Flatten the tree for LazyColumn
    val flatNodes = remember(rootNode, expandedPaths) {
        val result = mutableListOf<FlatNode>()
        fun traverse(node: FileNode, level: Int) {
            val isExpanded = expandedPaths.contains(node.absolutePath())
            result.add(FlatNode(node, level, isExpanded))

            if (isExpanded && node.isDirectory) {
                node.children().sortedByDescending { child -> child.size() }.forEach { child ->
                    traverse(child, level + 1)
                }
            }
        }
        traverse(rootNode, 0)
        result
    }
    
    val allVisiblePaths = remember(flatNodes) { flatNodes.map { it.fileNode.absolutePath() } }

    val listState = rememberLazyListState()

    // Simple Centered Scroll: Always put the selected file in the middle
    LaunchedEffect(selectedPaths, flatNodes) {
        if (selectedPaths.size == 1) {
            val path = selectedPaths.first()
            val fileIndex = flatNodes.indexOfFirst { node -> node.fileNode.absolutePath() == path }
            if (fileIndex >= 0) {
                val layoutInfo = listState.layoutInfo
                val visibleItemsCount = layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 20
                
                val targetIndex = (fileIndex - (visibleItemsCount / 2)).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
            items(flatNodes, key = { node -> node.fileNode.absolutePath() }) { flatNode ->
                val node = flatNode.fileNode
                val isSelected = selectedPaths.contains(node.absolutePath())
                var rowPosition by remember { mutableStateOf(Offset.Zero) }

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
                                        // If right-clicked on an unselected item, select ONLY it (standard OS behavior)
                                        val singleSet = setOf(node.absolutePath())
                                        onSelect(node.absolutePath(), false, false, allVisiblePaths)
                                        singleSet
                                    }
                                    // Use absolute window coordinates for the context menu
                                    onSecondaryClick(finalSelection, rowPosition + change.position)
                                } else {
                                    if (node.isDirectory && !isCtrl && !isShift) {
                                        expandedPaths = if (flatNode.isExpanded) {
                                            expandedPaths - node.absolutePath()
                                        } else {
                                            expandedPaths + node.absolutePath()
                                        }
                                    }
                                    onSelect(node.absolutePath(), isCtrl, isShift, allVisiblePaths)
                                }
                            }
                        }
                        .padding(start = (flatNode.level * 16 + 4).dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (node.isDirectory) {
                        Text(
                            text = if (flatNode.isExpanded) "▼" else "▶",
                            color = Color.LightGray,
                            modifier = Modifier.width(16.dp),
                            style = MaterialTheme.typography.caption
                        )
                    } else {
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Spacer(modifier = Modifier.width(Dimens.SpacingSmall))

                    // Color block representing file type
                    val ext = if (node.isDirectory) "dir_block" else {
                        val dotIndex = node.name().lastIndexOf('.')
                        if (dotIndex > 0) node.name().substring(dotIndex + 1) else ""
                    }
                    val color = getColorForExtension(ext, customColors)

                    Box(modifier = Modifier.size(Dimens.IconSmall).background(color, RoundedCornerShape(Dimens.RadiusSmall)))

                    Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

                    Text(
                        text = node.name(),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body2
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatSize(node.size()),
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
