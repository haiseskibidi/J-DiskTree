package com.jdisktree.scanner;

/**
 * Listener interface for receiving updates during disk scanning.
 */
@FunctionalInterface
public interface ScanProgressListener {
    /**
     * Called periodically during the scan.
     * Note: This may be called from background threads.
     *
     * @param progress The current progress snapshot.
     */
    void onProgress(ScanProgress progress);
}
