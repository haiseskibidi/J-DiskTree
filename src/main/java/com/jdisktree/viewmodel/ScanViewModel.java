package com.jdisktree.viewmodel;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.TreeMapRect;
import com.jdisktree.scanner.DiskScannerService;
import com.jdisktree.state.UiState;
import com.jdisktree.treemap.TreemapService;
import com.jdisktree.treemap.index.SpatialGridIndex;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * ViewModel that orchestrates disk scanning and treemap calculation.
 * Bridges the domain logic with the UI state.
 */
public class ScanViewModel {

    private final TreemapService treemapService = new TreemapService();
    private final Consumer<UiState> stateObserver;
    private volatile UiState currentState = UiState.idle();
    private long lastProgressUpdate = 0;

    public ScanViewModel(Consumer<UiState> stateObserver) {
        this.stateObserver = stateObserver;
        emitState(currentState);
    }

    public void startScan(Path path, double width, double height) {
        updateState(s -> UiState.idle().withProgress(null)); 

        CompletableFuture.runAsync(() -> {
            try {
                DiskScannerService scanner = new DiskScannerService(progress -> {
                    long now = System.currentTimeMillis();
                    if (now - lastProgressUpdate > 50) { // Slightly more relaxed throttling
                        updateState(s -> s.withProgress(progress));
                        lastProgressUpdate = now;
                    }
                });

                long scanStart = System.currentTimeMillis();
                FileNode root = scanner.scan(path);
                long scanEnd = System.currentTimeMillis();
                System.out.println("Scan completed in: " + (scanEnd - scanStart) + " ms. Nodes: " + scanner.getProgress().filesScanned());
                
                updateState(s -> s.withProgress(scanner.getProgress()).calculating());

                long treemapStart = System.currentTimeMillis();
                List<TreeMapRect> rects = treemapService.calculateLayout(root, 0, 0, width, height);
                long treemapEnd = System.currentTimeMillis();
                System.out.println("Treemap calculation completed in: " + (treemapEnd - treemapStart) + " ms. Rects: " + rects.size());
                
                // Build Spatial Index
                SpatialGridIndex index = new SpatialGridIndex(100, 100, width, height);
                for (TreeMapRect rect : rects) {
                    index.add(rect);
                }

                long updateStart = System.currentTimeMillis();
                updateState(s -> s.withRects(rects, index));
                long updateEnd = System.currentTimeMillis();
                System.out.println("State update (EDT handoff) took: " + (updateEnd - updateStart) + " ms");

            } catch (Exception e) {
                updateState(s -> s.withError("Scan failed: " + e.getMessage()));
                e.printStackTrace();
            }
        });
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
