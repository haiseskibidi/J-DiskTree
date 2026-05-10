package com.jdisktree.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jdisktree.domain.FileNode
import com.jdisktree.domain.TreeMapRect

/**
 * Wrappers to force Compose stability on Java classes.
 * This is critical for skipping recomposition of heavy components like the File Tree and Treemap.
 */

@Immutable
data class StableFileTree(
    val root: FileNode?
)

@Immutable
data class StableTreemapData(
    val rects: List<TreeMapRect>
)

@Stable
class LayoutWeights(
    tree: Float,
    stats: Float
) {
    var treeWeight by mutableStateOf(tree)
    var statsWeight by mutableStateOf(stats)
}
