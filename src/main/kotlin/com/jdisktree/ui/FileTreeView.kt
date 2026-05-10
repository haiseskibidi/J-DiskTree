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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import com.jdisktree.domain.FileNode
import java.nio.file.Paths

data class FlatNode(
    val fileNode: FileNode,
    val level: Int,
    val isExpanded: Boolean
)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun FileTreeView(
    rootNode: FileNode?,
    selectedPath: String?,
    onSelect: (String) -> Unit,
    onSecondaryClick: (String, Offset) -> Unit
) {
    if (rootNode == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource("no_data"), color = Color.Gray)
        }
        return
    }

    var expandedPaths by remember { mutableStateOf(setOf(rootNode.absolutePath())) }

    // Auto-expand when selectedPath changes
    LaunchedEffect(selectedPath) {
        if (selectedPath != null) {
            val newExpanded = expandedPaths.toMutableSet()
            var currentPath = Paths.get(selectedPath).parent
            while (currentPath != null) {
                val pathStr = currentPath.toAbsolutePath().toString()
                newExpanded.add(pathStr)
                currentPath = currentPath.parent
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
                // Sort children by size descending for a better UX
                node.children().sortedByDescending { it.size() }.forEach { child ->
                    traverse(child, level + 1)
                }
            }
        }
        traverse(rootNode, 0)
        result
    }

    val listState = rememberLazyListState()

    // Smart Scroll: Intelligently decide where to scroll based on nesting depth
    LaunchedEffect(selectedPath, flatNodes) {
        if (selectedPath != null) {
            val fileIndex = flatNodes.indexOfFirst { it.fileNode.absolutePath() == selectedPath }
            if (fileIndex >= 0) {
                val rootNioPath = Paths.get(rootNode.absolutePath())
                var currentPath = Paths.get(selectedPath)
                var rootPlusOnePath = selectedPath

                // Find the "Root + 1" folder (macro context)
                while (currentPath != null && currentPath != rootNioPath) {
                    val parent = currentPath.parent
                    if (parent == rootNioPath) {
                        rootPlusOnePath = currentPath.toAbsolutePath().toString()
                        break
                    }
                    currentPath = parent
                }

                val rootPlusOneIndex = flatNodes.indexOfFirst { it.fileNode.absolutePath() == rootPlusOnePath }
                
                // DECISION LOGIC:
                // If the distance between the top-level folder and the selected file is small (< 20 rows),
                // scroll to the folder so the user sees the macro context.
                // If the file is too deep, scroll to the file with a small offset (buffer)
                // so it's not glued to the very top of the screen.
                val viewportThreshold = 20 
                val bufferOffset = 3

                val targetIndex = if (rootPlusOneIndex >= 0 && (fileIndex - rootPlusOneIndex) < viewportThreshold) {
                    rootPlusOneIndex
                } else {
                    // Scroll to 3 items above the file for better visual context
                    (fileIndex - bufferOffset).coerceAtLeast(0)
                }
                
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
            items(flatNodes, key = { it.fileNode.absolutePath() }) { flatNode ->
                val node = flatNode.fileNode
                val isSelected = node.absolutePath() == selectedPath

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) Color(0xFF388E3C).copy(alpha = 0.4f) else Color.Transparent)
                        .onPointerEvent(PointerEventType.Press) { event ->
                            val change = event.changes.first()
                            if (change.pressed) {
                                if (event.buttons.isSecondaryPressed) {
                                    onSecondaryClick(node.absolutePath(), change.position)
                                } else {
                                    if (node.isDirectory) {
                                        expandedPaths = if (flatNode.isExpanded) {
                                            expandedPaths - node.absolutePath()
                                        } else {
                                            expandedPaths + node.absolutePath()
                                        }
                                    }
                                    onSelect(node.absolutePath())
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

                    Spacer(modifier = Modifier.width(4.dp))

                    // Color block representing file type
                    val ext = if (node.isDirectory) "dir_block" else {
                        val dotIndex = node.name().lastIndexOf('.')
                        if (dotIndex > 0) node.name().substring(dotIndex + 1) else ""
                    }
                    val color = getColorForExtension(ext)

                    Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))

                    Spacer(modifier = Modifier.width(8.dp))

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
