package com.jdisktree.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.jdisktree.domain.TreeMapRect
import com.jdisktree.treemap.index.SpatialGridIndex

import com.jdisktree.domain.FileColorConfig

@Composable
fun TreemapWithTooltip(
    stableData: StableTreemapData,
    index: SpatialGridIndex?,
    selectedPaths: Set<String>,
    highlightedExtension: String?,
    searchQuery: String = "",
    ageFilterDays: Int = 0,
    customColors: List<FileColorConfig> = emptyList(),
    isResizing: Boolean = false,
    onSelect: (String?, Boolean) -> Unit,
    onSecondaryClick: (Set<String>, Offset) -> Unit
) {
    var hoverRect by remember { mutableStateOf<TreeMapRect?>(null) }
    var mousePos by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        TreemapCanvas(
            rects = stableData.rects,
            index = index,
            selectedPaths = selectedPaths,
            highlightedExtension = highlightedExtension,
            searchQuery = searchQuery,
            ageFilterDays = ageFilterDays,
            customColors = customColors,
            baseWidth = 1000.0,
            baseHeight = 1000.0,
            isResizing = isResizing,
            onHover = { rect, pos ->
                hoverRect = rect
                mousePos = pos
            },
            onClick = onSelect,
            onSecondaryClick = onSecondaryClick
        )

        // Only show tooltip if NOT resizing for better performance
        if (!isResizing) {
            hoverRect?.let { rect ->
                Tooltip(rect, mousePos)
            }
        }
    }
}
