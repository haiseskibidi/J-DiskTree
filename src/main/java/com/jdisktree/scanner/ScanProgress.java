package com.jdisktree.scanner;

/**
 * Snapshot of the current scanning progress.
 *
 * @param filesScanned  Total number of files and directories processed so far.
 * @param bytesScanned  Total size of all processed files.
 * @param currentPath   The path currently being scanned.
 */
public record ScanProgress(
        long filesScanned,
        long bytesScanned,
        String currentPath
) {}
