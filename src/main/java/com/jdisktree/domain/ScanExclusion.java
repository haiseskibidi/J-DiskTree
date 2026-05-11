package com.jdisktree.domain;

/**
 * Immutable record representing a directory or file exclusion pattern for the disk scanner.
 */
public record ScanExclusion(String pattern, boolean isEnabled) {
}
