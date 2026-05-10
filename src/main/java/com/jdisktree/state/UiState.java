package com.jdisktree.state;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.TreeMapRect;
import com.jdisktree.scanner.ScanProgress;
import com.jdisktree.treemap.index.SpatialGridIndex;

import java.util.List;

/**
 * Immutable UI State for the J-DiskTree application.
 *
 * @param status       Current scan status.
 * @param progress     Current scan progress (null if not scanning).
 * @param rects        List of treemap rectangles to render (empty if not ready).
 * @param index        Spatial index for fast hover detection.
 * @param rootNode     The root domain node of the scanned directory tree.
 * @param errorMessage Error message if status is ERROR.
 */
public record UiState(
        ScanStatus status,
        ScanProgress progress,
        List<TreeMapRect> rects,
        SpatialGridIndex index,
        FileNode rootNode,
        String errorMessage
) {
    public static UiState idle() {
        return new UiState(ScanStatus.IDLE, null, List.of(), null, null, null);
    }

    public UiState withProgress(ScanProgress progress) {
        return new UiState(ScanStatus.SCANNING, progress, List.of(), null, null, null);
    }

    public UiState withRects(List<TreeMapRect> rects, SpatialGridIndex index, FileNode rootNode) {
        return new UiState(ScanStatus.COMPLETED, progress, rects, index, rootNode, null);
    }

    public UiState withError(String message) {
        return new UiState(ScanStatus.ERROR, progress, rects, index, rootNode, message);
    }

    public UiState calculating() {
        return new UiState(ScanStatus.CALCULATING_TREEMAP, progress, rects, index, null, null);
    }
}
