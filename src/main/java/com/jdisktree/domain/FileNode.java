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
     * Recursively creates a new tree with the specified target path removed.
     * All parent directory sizes are automatically updated.
     * Returns null if this node itself is the target to be removed.
     */
    public FileNode prune(String targetPath) {
        if (this.absolutePath.equals(targetPath)) {
            return null;
        }

        if (!isDirectory) {
            return this;
        }

        // Optimization: only recurse if targetPath is a descendant of this directory
        // Use normalized paths and a trailing separator to prevent "Folder" matching "Folder2"
        String normalizedTarget = targetPath.replace('\\', '/');
        String normalizedCurrent = this.absolutePath.replace('\\', '/');
        if (!normalizedCurrent.endsWith("/")) normalizedCurrent += "/";

        if (!normalizedTarget.startsWith(normalizedCurrent)) {
            return this;
        }

        List<FileNode> newChildren = children.stream()
                .map(child -> child.prune(targetPath))
                .filter(java.util.Objects::nonNull)
                .toList();

        long newSize = newChildren.stream().mapToLong(FileNode::size).sum();
        
        // If size changed, we MUST return a new node instance to trigger UI updates
        if (newSize == this.size && newChildren.size() == this.children.size()) {
            return this; 
        }

        return new FileNode(name, absolutePath, newSize, true, newChildren);
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
