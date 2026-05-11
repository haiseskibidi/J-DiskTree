package com.jdisktree.domain;

/**
 * Immutable record representing a custom color for a specific file extension.
 */
public record FileColorConfig(String extension, String hexColor) {
}
