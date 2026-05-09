package com.jdisktree.treemap;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.TreeMapRect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calculating layout using the Squarified Treemap algorithm.
 * Based on Bruls, Huizing, and van Wijk.
 */
public class TreemapService {

    /**
     * Calculates the layout for a given FileNode tree within the specified bounds.
     *
     * @param root   The root FileNode.
     * @param x      Initial X coordinate.
     * @param y      Initial Y coordinate.
     * @param width  Container width.
     * @param height Container height.
     * @return List of calculated rectangles.
     */
    public List<TreeMapRect> calculateLayout(FileNode root, double x, double y, double width, double height) {
        List<TreeMapRect> result = new ArrayList<>();
        if (root.size() == 0) return result;

        calculateRecursive(root, x, y, width, height, result);
        return result;
    }

    private void calculateRecursive(FileNode node, double x, double y, double w, double h, List<TreeMapRect> result) {
        String extension = "";
        if (!node.isDirectory()) {
            int dotIndex = node.name().lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < node.name().length() - 1) {
                extension = node.name().substring(dotIndex + 1).toLowerCase();
            }
        }
        result.add(new TreeMapRect(node.absolutePath(), x, y, w, h, node.isDirectory(), extension));

        if (!node.isDirectory() || node.children().isEmpty()) {
            return;
        }

        List<FileNode> children = node.children().stream()
                .filter(c -> c.size() > 0)
                .sorted(Comparator.comparingLong(FileNode::size).reversed())
                .collect(Collectors.toList());

        if (children.isEmpty()) return;

        squarify(children, new ArrayList<>(), new Rect(x, y, w, h), node.size(), result);
    }

    private void squarify(List<FileNode> children, List<FileNode> row, Rect bounds, long totalSize, List<TreeMapRect> result) {
        if (children.isEmpty()) {
            if (!row.isEmpty()) {
                layoutRow(row, bounds, totalSize, result);
            }
            return;
        }

        FileNode nextChild = children.get(0);
        List<FileNode> rowWithNext = new ArrayList<>(row);
        rowWithNext.add(nextChild);

        double currentRatio = worstRatio(row, bounds, totalSize);
        double nextRatio = worstRatio(rowWithNext, bounds, totalSize);

        if (row.isEmpty() || currentRatio >= nextRatio) {
            // Keep adding to current row
            squarify(children.subList(1, children.size()), rowWithNext, bounds, totalSize, result);
        } else {
            // Layout current row and start a new one in the remaining space
            Rect remaining = layoutRow(row, bounds, totalSize, result);
            squarify(children, new ArrayList<>(), remaining, totalSize - sumSize(row), result);
        }
    }

    private double worstRatio(List<FileNode> row, Rect bounds, long totalSize) {
        if (row.isEmpty()) return Double.MAX_VALUE;

        double rowTotalSize = sumSize(row);
        double rowArea = (rowTotalSize / totalSize) * (bounds.w * bounds.h);
        double side = Math.min(bounds.w, bounds.h);
        
        double maxArea = row.stream().mapToDouble(c -> (double)c.size() / totalSize * (bounds.w * bounds.h)).max().orElse(0);
        double minArea = row.stream().mapToDouble(c -> (double)c.size() / totalSize * (bounds.w * bounds.h)).min().orElse(0);

        return Math.max(
                (side * side * maxArea) / (rowArea * rowArea),
                (rowArea * rowArea) / (side * side * minArea)
        );
    }

    private Rect layoutRow(List<FileNode> row, Rect bounds, long totalSize, List<TreeMapRect> result) {
        double rowTotalSize = sumSize(row);
        double rowArea = (rowTotalSize / totalSize) * (bounds.w * bounds.h);
        
        boolean horizontal = bounds.w >= bounds.h;
        double rowWidth = horizontal ? rowArea / bounds.h : bounds.w;
        double rowHeight = horizontal ? bounds.h : rowArea / bounds.w;

        double currentX = bounds.x;
        double currentY = bounds.y;

        for (FileNode child : row) {
            double childArea = ((double) child.size() / totalSize) * (bounds.w * bounds.h);
            double childWidth = horizontal ? rowWidth : childArea / rowHeight;
            double childHeight = horizontal ? childArea / rowWidth : rowHeight;

            calculateRecursive(child, currentX, currentY, childWidth, childHeight, result);

            if (horizontal) {
                currentY += childHeight;
            } else {
                currentX += childWidth;
            }
        }

        if (horizontal) {
            return new Rect(bounds.x + rowWidth, bounds.y, bounds.w - rowWidth, bounds.h);
        } else {
            return new Rect(bounds.x, bounds.y + rowHeight, bounds.w, bounds.h - rowHeight);
        }
    }

    private long sumSize(List<FileNode> nodes) {
        return nodes.stream().mapToLong(FileNode::size).sum();
    }

    private record Rect(double x, double y, double w, double h) {}
}
