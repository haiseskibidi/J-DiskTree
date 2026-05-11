package com.jdisktree.domain;

import java.util.List;

/**
 * Represents a node in a differential tree between two snapshots.
 */
public record DiffNode(
    String name,
    String absolutePath,
    long currentSize,
    long deltaSize,
    DiffStatus status,
    boolean isDirectory,
    List<DiffNode> children
) {
    public static DiffNode added(FileNode node) {
        return new DiffNode(node.name(), node.absolutePath(), node.size(), node.size(), DiffStatus.ADDED, node.isDirectory(), 
            node.children().stream().map(DiffNode::added).toList());
    }

    public static DiffNode removed(FileNode node) {
        return new DiffNode(node.name(), node.absolutePath(), 0, -node.size(), DiffStatus.REMOVED, node.isDirectory(), 
            node.children().stream().map(DiffNode::removed).toList());
    }
}
