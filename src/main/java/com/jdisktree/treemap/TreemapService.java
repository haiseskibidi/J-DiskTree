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
    private static final double MIN_SIZE_FRACTION = 0.0001; // 0.01% threshold - show more details

    public List<TreeMapRect> calculateLayout(FileNode root, double x, double y, double width, double height) {
        List<TreeMapRect> result = new ArrayList<>(2048); 
        if (root.size() == 0) return result;

        calculateRecursive(root, x, y, width, height, result, root.size());
        return result;
    }

    private void calculateRecursive(FileNode node, double x, double y, double w, double h, List<TreeMapRect> result, long totalRootSize) {
        String extension = "";
        if (!node.isDirectory()) {
            int dotIndex = node.name().lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < node.name().length() - 1) {
                extension = node.name().substring(dotIndex + 1).toLowerCase();
            }
        }
        result.add(new TreeMapRect(node.absolutePath(), x, y, w, h, node.isDirectory(), extension, node.size()));

        if (!node.isDirectory() || node.children().isEmpty()) {
            return;
        }

        // Geometric Stop-Guard: If the container is too thin or small, stop recursing to avoid "sticks"
        double aspectRatio = w > h ? w / h : h / w;
        if ((double) node.size() / totalRootSize < MIN_SIZE_FRACTION || w < 10 || h < 10 || aspectRatio > 15.0) {
            return;
        }

        List<FileNode> allChildren = node.children().stream()
                .filter(c -> c.size() > 0)
                .sorted(Comparator.comparingLong(FileNode::size).reversed())
                .collect(Collectors.toList());

        if (allChildren.isEmpty()) return;

        // Inclusive Smart Grouping: BOTH files and directories smaller than 1% are grouped
        double groupingThreshold = 0.01; 
        List<FileNode> significantChildren = new ArrayList<>();
        long otherSize = 0;
        int otherCount = 0;

        for (FileNode child : allChildren) {
            // Group if the child is smaller than 1% OR we already have too many significant items
            if ((double) child.size() / node.size() < groupingThreshold || significantChildren.size() >= 40) {
                otherSize += child.size();
                otherCount++;
            } else {
                significantChildren.add(child);
            }
        }

        if (otherSize > 0) {
            significantChildren.add(FileNode.file(
                "[" + otherCount + " others]", 
                node.absolutePath() + "/[others]", 
                otherSize
            ));
        }

        // Monolithic layout: No internal padding for directories
        double padding = 0.0; 
        double innerX = x + padding;
        double innerY = y + padding;
        double innerW = w - padding * 2;
        double innerH = h - padding * 2;

        long effectiveTotalSize = significantChildren.stream().mapToLong(FileNode::size).sum();
        squarify(significantChildren, new ArrayList<>(), new Rect(innerX, innerY, innerW, innerH), effectiveTotalSize, result, totalRootSize);
    }

    private void squarify(List<FileNode> children, List<FileNode> row, Rect bounds, long totalSize, List<TreeMapRect> result, long totalRootSize) {
        if (children.isEmpty()) {
            if (!row.isEmpty()) {
                layoutRow(row, bounds, totalSize, result, totalRootSize);
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
            squarify(children.subList(1, children.size()), rowWithNext, bounds, totalSize, result, totalRootSize);
        } else {
            // Layout current row and start a new one in the remaining space
            Rect remaining = layoutRow(row, bounds, totalSize, result, totalRootSize);
            squarify(children, new ArrayList<>(), remaining, totalSize - sumSize(row), result, totalRootSize);
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

    private Rect layoutRow(List<FileNode> row, Rect bounds, long totalSize, List<TreeMapRect> result, long totalRootSize) {
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

            calculateRecursive(child, currentX, currentY, childWidth, childHeight, result, totalRootSize);

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
