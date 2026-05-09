package com.jdisktree.domain;

/**
 * Geometric representation of a file or directory for rendering.
 *
 * @param path        The absolute path of the node.
 * @param x           The X coordinate of the top-left corner.
 * @param y           The Y coordinate of the top-left corner.
 * @param width       Width of the rectangle.
 * @param height      Height of the rectangle.
 * @param isDirectory True if this node represents a directory.
 */
public record TreeMapRect(
        String path,
        double x,
        double y,
        double width,
        double height,
        boolean isDirectory
) {}
