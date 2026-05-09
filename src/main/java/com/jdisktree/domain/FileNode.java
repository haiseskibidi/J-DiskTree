package com.jdisktree.domain;

import java.util.Collections;
import java.util.List;

/**
 * Immutable representation of a file or directory in the disk tree.
 *
 * @param name         The name of the file or directory.
 * @param absolutePath The full path on the disk.
 * @param size         The size in bytes. For directories, this is the aggregated size of all children.
 * @param isDirectory  True if this node represents a directory.
 * @param children     List of child nodes. Empty for files or empty directories.
 */
public record FileNode(
        String name,
        String absolutePath,
        long size,
        boolean isDirectory,
        List<FileNode> children
) {
    public FileNode {
        // Ensure children list is immutable
        if (children == null) {
            children = Collections.emptyList();
        } else {
            children = List.copyOf(children);
        }
    }

    /**
     * Convenience constructor for a file node (no children).
     */
    public static FileNode file(String name, String absolutePath, long size) {
        return new FileNode(name, absolutePath, size, false, Collections.emptyList());
    }

    /**
     * Convenience constructor for a directory node.
     */
    public static FileNode directory(String name, String absolutePath, long size, List<FileNode> children) {
        return new FileNode(name, absolutePath, size, true, children);
    }
}
