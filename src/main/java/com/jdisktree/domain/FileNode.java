package com.jdisktree.domain;

import java.util.Collections;
import java.util.List;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

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
        long lastModified,
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
     * Batch version of prune.
     */
    public FileNode prune(Collection<String> targetPaths) {
        Set<String> targets = new HashSet<>(targetPaths);
        return pruneInternal(targets);
    }

    /**
     * Convenience constructor for a file node (no children).
     */
    public static FileNode file(String name, String absolutePath, long size, long lastModified) {
        return new FileNode(name, absolutePath, size, lastModified, false, Collections.emptyList());
    }

    /**
     * Convenience constructor for a directory node.
     */
    public static FileNode directory(String name, String absolutePath, long size, long lastModified, List<FileNode> children) {
        return new FileNode(name, absolutePath, size, lastModified, true, children);
    }

    // Overload for pruning (carrying over lastModified)
    private FileNode pruneInternal(Set<String> targets) {
        if (targets.contains(this.absolutePath)) {
            return null;
        }

        if (!isDirectory) {
            return this;
        }

        // Optimization: only recurse if any target is a descendant of this directory
        boolean hasDescendant = false;
        String normalizedCurrent = this.absolutePath.replace('\\', '/');
        if (!normalizedCurrent.endsWith("/")) normalizedCurrent += "/";
        
        for (String target : targets) {
            if (target.replace('\\', '/').startsWith(normalizedCurrent)) {
                hasDescendant = true;
                break;
            }
        }

        if (!hasDescendant) {
            return this;
        }

        List<FileNode> newChildren = children.stream()
                .map(child -> child.pruneInternal(targets))
                .filter(java.util.Objects::nonNull)
                .toList();

        long newSize = newChildren.stream().mapToLong(FileNode::size).sum();
        
        if (newSize == this.size && newChildren.size() == this.children.size()) {
            return this; 
        }

        return new FileNode(name, absolutePath, newSize, lastModified, true, newChildren);
    }
}
