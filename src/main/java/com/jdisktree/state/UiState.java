package com.jdisktree.state;

import com.jdisktree.domain.TreeMapRect;
import com.jdisktree.scanner.ScanProgress;

import java.util.List;

/**
 * Immutable UI State for the J-DiskTree application.
 *
 * @param status       Current scan status.
 * @param progress     Current scan progress (null if not scanning).
 * @param rects        List of treemap rectangles to render (empty if not ready).
 * @param errorMessage Error message if status is ERROR.
 */
public record UiState(
        ScanStatus status,
        ScanProgress progress,
        List<TreeMapRect> rects,
        String errorMessage
) {
    public static UiState idle() {
        return new UiState(ScanStatus.IDLE, null, List.of(), null);
    }

    public UiState withProgress(ScanProgress progress) {
        return new UiState(ScanStatus.SCANNING, progress, List.of(), null);
    }

    public UiState withRects(List<TreeMapRect> rects) {
        return new UiState(ScanStatus.COMPLETED, progress, rects, null);
    }

    public UiState withError(String message) {
        return new UiState(ScanStatus.ERROR, progress, rects, message);
    }

    public UiState calculating() {
        return new UiState(ScanStatus.CALCULATING_TREEMAP, progress, rects, null);
    }
}
