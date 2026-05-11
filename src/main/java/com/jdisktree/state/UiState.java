package com.jdisktree.state;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.FileTypeStat;
import com.jdisktree.domain.TreeMapRect;
import com.jdisktree.scanner.ScanProgress;
import com.jdisktree.treemap.index.SpatialGridIndex;

import java.util.List;
import java.util.Set;
import java.util.Collections;

/**
 * Immutable UI State for the J-DiskTree application.
 *
 * @param status        Current scan status.
 * @param progress      Current scan progress (null if not scanning).
 * @param rects         List of treemap rectangles to render (empty if not ready).
 * @param index         Spatial index for fast hover detection.
 * @param rootNode      The root domain node of the scanned directory tree.
 * @param typeStats     Statistics grouped by file extension.
 * @param selectedPaths Set of absolute paths currently selected by the user.
 * @param errorMessage  Error message if status is ERROR.
 */
public record UiState(
        ScanStatus status,
        ScanProgress progress,
        List<TreeMapRect> rects,
        SpatialGridIndex index,
        FileNode rootNode,
        List<FileTypeStat> typeStats,
        Set<String> selectedPaths,
        String errorMessage
) {
    public static UiState idle() {
        return new UiState(ScanStatus.IDLE, null, List.of(), null, null, List.of(), Collections.emptySet(), null);
    }

    public UiState withProgress(ScanProgress progress) {
        return new UiState(ScanStatus.SCANNING, progress, List.of(), null, null, List.of(), Collections.emptySet(), null);
    }

    public UiState withRects(List<TreeMapRect> rects, SpatialGridIndex index, FileNode rootNode, List<FileTypeStat> typeStats) {
        return new UiState(ScanStatus.COMPLETED, progress, rects, index, rootNode, typeStats, selectedPaths, null);
    }

    public UiState withSelectedPaths(Set<String> selectedPaths) {
        return new UiState(status, progress, rects, index, rootNode, typeStats, selectedPaths, errorMessage);
    }

    public UiState withError(String message) {
        return new UiState(ScanStatus.ERROR, progress, rects, index, rootNode, typeStats, selectedPaths, message);
    }

    public UiState calculating() {
        return new UiState(ScanStatus.CALCULATING_TREEMAP, progress, rects, index, null, List.of(), Collections.emptySet(), null);
    }
}
