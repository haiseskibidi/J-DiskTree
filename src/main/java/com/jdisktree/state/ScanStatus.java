package com.jdisktree.state;

/**
 * Represents the current status of the disk scanning process.
 */
public enum ScanStatus {
    IDLE,
    SCANNING,
    CALCULATING_TREEMAP,
    COMPLETED,
    ERROR
}
