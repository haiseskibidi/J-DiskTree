package com.jdisktree.state;

import com.jdisktree.domain.DiffNode;
import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.FileTypeStat;
import com.jdisktree.domain.TreeMapRect;
import com.jdisktree.scanner.ScanProgress;
import com.jdisktree.treemap.index.SpatialGridIndex;
import com.jdisktree.domain.DiskSpaceInfo;

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
 * @param diffNode      Differential tree (null if not in comparison mode).
 * @param typeStats     Statistics grouped by file extension.
 * @param selectedPaths Set of absolute paths currently selected by the user.
 * @param errorMessage  Error message if status is ERROR.
 * @param diskSpaceInfo Disk space info of the current root.
 */
public record UiState(
        ScanStatus status,
        ScanProgress progress,
        List<TreeMapRect> rects,
        SpatialGridIndex index,
        FileNode rootNode,
        DiffNode diffNode,
        List<FileTypeStat> typeStats,
        Set<String> selectedPaths,
        Set<String> expandedPaths,
        String searchQuery,
        int ageFilterDays,
        String errorMessage,
        DiskSpaceInfo diskSpaceInfo
) {
    public static UiState idle() {
        return new UiState(ScanStatus.IDLE, null, List.of(), null, null, null, List.of(), Collections.emptySet(), Collections.emptySet(), "", 0, null, null);
    }

    public UiState withProgress(ScanProgress progress) {
        return new UiState(ScanStatus.SCANNING, progress, List.of(), null, null, null, List.of(), Collections.emptySet(), expandedPaths, searchQuery, ageFilterDays, null, diskSpaceInfo);
    }

    public UiState withRects(List<TreeMapRect> rects, SpatialGridIndex index, FileNode rootNode, List<FileTypeStat> typeStats) {
        Set<String> newExpanded = new java.util.HashSet<>(expandedPaths);
        if (rootNode != null) newExpanded.add(rootNode.absolutePath());
        return new UiState(ScanStatus.COMPLETED, progress, rects, index, rootNode, null, typeStats, selectedPaths, newExpanded, searchQuery, ageFilterDays, null, diskSpaceInfo);
    }

    public UiState withDiff(DiffNode diffNode, List<TreeMapRect> rects, SpatialGridIndex index, List<FileTypeStat> typeStats) {
        Set<String> newExpanded = new java.util.HashSet<>(expandedPaths);
        if (diffNode != null) newExpanded.add(diffNode.absolutePath());
        return new UiState(ScanStatus.COMPLETED, progress, rects, index, rootNode, diffNode, typeStats, selectedPaths, newExpanded, searchQuery, ageFilterDays, null, diskSpaceInfo);
    }

    public UiState withSelectedPaths(Set<String> selectedPaths) {
        return new UiState(status, progress, rects, index, rootNode, diffNode, typeStats, selectedPaths, expandedPaths, searchQuery, ageFilterDays, errorMessage, diskSpaceInfo);
    }

    public UiState withExpandedPaths(Set<String> expandedPaths) {
        return new UiState(status, progress, rects, index, rootNode, diffNode, typeStats, selectedPaths, expandedPaths, searchQuery, ageFilterDays, errorMessage, diskSpaceInfo);
    }

    public UiState withSearchQuery(String searchQuery) {
        return new UiState(status, progress, rects, index, rootNode, diffNode, typeStats, selectedPaths, expandedPaths, searchQuery, ageFilterDays, errorMessage, diskSpaceInfo);
    }

    public UiState withAgeFilter(int ageFilterDays) {
        return new UiState(status, progress, rects, index, rootNode, diffNode, typeStats, selectedPaths, expandedPaths, searchQuery, ageFilterDays, errorMessage, diskSpaceInfo);
    }

    public UiState withError(String message) {
        return new UiState(ScanStatus.ERROR, progress, rects, index, rootNode, diffNode, typeStats, selectedPaths, expandedPaths, searchQuery, ageFilterDays, message, diskSpaceInfo);
    }

    public UiState calculating() {
        return new UiState(ScanStatus.CALCULATING_TREEMAP, progress, rects, index, null, null, List.of(), Collections.emptySet(), expandedPaths, searchQuery, ageFilterDays, null, diskSpaceInfo);
    }

    public UiState withDiskSpaceInfo(DiskSpaceInfo diskSpaceInfo) {
        return new UiState(status, progress, rects, index, rootNode, diffNode, typeStats, selectedPaths, expandedPaths, searchQuery, ageFilterDays, errorMessage, diskSpaceInfo);
    }
}
