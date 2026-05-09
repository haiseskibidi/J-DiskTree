package com.jdisktree.viewmodel;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.TreeMapRect;
import com.jdisktree.scanner.DiskScannerService;
import com.jdisktree.state.UiState;
import com.jdisktree.treemap.TreemapService;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * ViewModel that orchestrates disk scanning and treemap calculation.
 * Bridges the domain logic with the UI state.
 */
public class ScanViewModel {

    private final TreemapService treemapService = new TreemapService();
    private final Consumer<UiState> stateObserver;
    private UiState currentState = UiState.idle();
    private long lastProgressUpdate = 0;

    public ScanViewModel(Consumer<UiState> stateObserver) {
        this.stateObserver = stateObserver;
        updateState(currentState);
    }

    /**
     * Starts the scanning process for the given path.
     * Execution is asynchronous and does not block the caller.
     *
     * @param path   The directory to scan.
     * @param width  The target width for the treemap.
     * @param height The target height for the treemap.
     */
    public void startScan(Path path, double width, double height) {
        updateState(UiState.idle().withProgress(null)); // Set status to SCANNING

        CompletableFuture.runAsync(() -> {
            try {
                DiskScannerService scanner = new DiskScannerService(progress -> {
                    long now = System.currentTimeMillis();
                    // Throttle updates to ~30 FPS to avoid UI stutter
                    if (now - lastProgressUpdate > 33) {
                        updateState(currentState.withProgress(progress));
                        lastProgressUpdate = now;
                    }
                });

                FileNode root = scanner.scan(path);
                
                // Final progress update before switching to calculation
                updateState(currentState.withProgress(scanner.getProgress()));
                
                updateState(currentState.calculating());

                List<TreeMapRect> rects = treemapService.calculateLayout(root, 0, 0, width, height);
                
                updateState(currentState.withRects(rects));

            } catch (Exception e) {
                updateState(currentState.withError("Scan failed: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private synchronized void updateState(UiState newState) {
        this.currentState = newState;
        if (stateObserver != null) {
            stateObserver.accept(newState);
        }
    }

    public UiState getCurrentState() {
        return currentState;
    }
}
