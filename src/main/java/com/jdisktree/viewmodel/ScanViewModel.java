package com.jdisktree.viewmodel;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.FileTypeStat;
import com.jdisktree.domain.TreeMapRect;
import com.jdisktree.scanner.DiskScannerService;
import com.jdisktree.scanner.FileOperationsService;
import com.jdisktree.state.UiState;
import com.jdisktree.treemap.TreemapService;
import com.jdisktree.treemap.index.SpatialGridIndex;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * ViewModel that orchestrates disk scanning and treemap calculation.
 * Bridges the domain logic with the UI state.
 */
public class ScanViewModel {

    private final TreemapService treemapService = new TreemapService();
    private final FileOperationsService fileOps = new FileOperationsService();
    private final com.jdisktree.scanner.ExportService exportService = new com.jdisktree.scanner.ExportService();
    private final Consumer<UiState> stateObserver;
    private volatile UiState currentState = UiState.idle();
    private long lastProgressUpdate = 0;

    public ScanViewModel(Consumer<UiState> stateObserver) {
        this.stateObserver = stateObserver;
        emitState(currentState);
    }

    public void exportData(String format, java.io.File targetFile) {
        FileNode root = currentState.rootNode();
        if (root == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                if ("CSV".equalsIgnoreCase(format)) {
                    exportService.exportToCsv(root, targetFile.toPath());
                } else if ("JSON".equalsIgnoreCase(format)) {
                    exportService.exportToJson(root, targetFile.toPath());
                }
            } catch (java.io.IOException e) {
                updateState(s -> s.withError("Export failed: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    public void openInExplorer(String path) {
        fileOps.openInExplorer(path);
    }

    public void copyPath(String path) {
        fileOps.copyToClipboard(path);
    }

    public void setSelectedPaths(Set<String> paths) {
        updateState(s -> s.withSelectedPaths(paths));
    }

    public void setSearchQuery(String query) {
        updateState(s -> s.withSearchQuery(query != null ? query : ""));
    }

    public void moveSelectedToTrash(Collection<String> paths, double width, double height) {
        CompletableFuture.runAsync(() -> {
            for (String path : paths) {
                fileOps.moveToTrash(path);
            }
            applyBatchPruning(paths, width, height);
        });
    }

    public void deleteSelectedPermanently(Collection<String> paths, double width, double height) {
        CompletableFuture.runAsync(() -> {
            for (String path : paths) {
                fileOps.deletePermanently(path);
            }
            applyBatchPruning(paths, width, height);
        });
    }

    public void moveToTrash(String path, double width, double height) {
        moveSelectedToTrash(Collections.singletonList(path), width, height);
    }

    public void deletePermanently(String path, double width, double height) {
        deleteSelectedPermanently(Collections.singletonList(path), width, height);
    }

    private void applyBatchPruning(Collection<String> targetPaths, double width, double height) {
        FileNode currentRoot = currentState.rootNode();
        if (currentRoot == null) return;

        FileNode newRoot = currentRoot.prune(targetPaths);
        
        if (newRoot == null) {
            updateState(s -> UiState.idle());
            return;
        }

        // Recalculate layout for the new pruned tree
        List<TreeMapRect> rects = treemapService.calculateLayout(newRoot, 0, 0, width, height);
        SpatialGridIndex index = new SpatialGridIndex(100, 100, width, height);
        for (TreeMapRect rect : rects) {
            index.add(rect);
        }

        List<FileTypeStat> stats = calculateTypeStats(newRoot);

        // Clear selection if selected items were pruned
        updateState(s -> {
            Set<String> newSelection = new java.util.HashSet<>(s.selectedPaths());
            newSelection.removeAll(targetPaths);
            return s.withRects(rects, index, newRoot, stats).withSelectedPaths(newSelection);
        });
    }

    private void applyPruning(String targetPath, double width, double height) {
        applyBatchPruning(Collections.singletonList(targetPath), width, height);
    }

    public void startScan(Path path, double width, double height, List<com.jdisktree.domain.ScanExclusion> exclusions) {
        updateState(s -> UiState.idle().withProgress(null)); 

        CompletableFuture.runAsync(() -> {
            try {
                DiskScannerService scanner = new DiskScannerService(progress -> {
                    long now = System.currentTimeMillis();
                    if (now - lastProgressUpdate > 50) { // Slightly more relaxed throttling
                        updateState(s -> s.withProgress(progress));
                        lastProgressUpdate = now;
                    }
                }, exclusions);

                long scanStart = System.currentTimeMillis();
                FileNode root = scanner.scan(path);
                long scanEnd = System.currentTimeMillis();
                System.out.println("Scan completed in: " + (scanEnd - scanStart) + " ms. Nodes: " + scanner.getProgress().filesScanned());
                
                updateState(s -> s.withProgress(scanner.getProgress()).calculating());

                long treemapStart = System.currentTimeMillis();
                List<TreeMapRect> rects = treemapService.calculateLayout(root, 0, 0, width, height);
                long treemapEnd = System.currentTimeMillis();
                System.out.println("Treemap calculation completed in: " + (treemapEnd - treemapStart) + " ms. Rects: " + rects.size());
                
                // Calculate extension stats
                List<FileTypeStat> stats = calculateTypeStats(root);

                // Build Spatial Index
                SpatialGridIndex index = new SpatialGridIndex(100, 100, width, height);
                for (TreeMapRect rect : rects) {
                    index.add(rect);
                }

                long updateStart = System.currentTimeMillis();
                updateState(s -> s.withRects(rects, index, root, stats));
                long updateEnd = System.currentTimeMillis();
                System.out.println("State update (EDT handoff) took: " + (updateEnd - updateStart) + " ms");

            } catch (Exception e) {
                updateState(s -> s.withError("Scan failed: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private List<FileTypeStat> calculateTypeStats(FileNode root) {
        Map<String, TypeAccumulator> aggregationMap = new HashMap<>();
        collectExtensions(root, aggregationMap);

        long totalBytes = root.size();
        List<FileTypeStat> results = new ArrayList<>();
        
        aggregationMap.forEach((ext, acc) -> {
            double percentage = totalBytes > 0 ? (double) acc.size / totalBytes : 0;
            results.add(new FileTypeStat(ext, acc.size, acc.count, percentage));
        });

        results.sort((a, b) -> Long.compare(b.totalSize(), a.totalSize()));
        return results;
    }

    private void collectExtensions(FileNode node, Map<String, TypeAccumulator> map) {
        if (!node.isDirectory()) {
            String name = node.name();
            int dotIndex = name.lastIndexOf('.');
            String ext = (dotIndex > 0 && dotIndex < name.length() - 1) 
                    ? name.substring(dotIndex + 1).toLowerCase() 
                    : "no extension";
            
            TypeAccumulator acc = map.computeIfAbsent(ext, k -> new TypeAccumulator());
            acc.size += node.size();
            acc.count++;
        } else {
            for (FileNode child : node.children()) {
                collectExtensions(child, map);
            }
        }
    }

    private static class TypeAccumulator {
        long size = 0;
        int count = 0;
    }

    private synchronized void updateState(UnaryOperator<UiState> updateFn) {
        this.currentState = updateFn.apply(this.currentState);
        emitState(this.currentState);
    }

    private void emitState(UiState state) {
        if (stateObserver != null) {
            SwingUtilities.invokeLater(() -> stateObserver.accept(state));
        }
    }

    public UiState getCurrentState() {
        return currentState;
    }
}
