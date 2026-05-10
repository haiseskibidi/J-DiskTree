package com.jdisktree.domain;

/**
 * Record holding statistics for a group of files with the same extension.
 */
public record FileTypeStat(
        String extension,
        long totalSize,
        int count,
        double percentage
) {}
