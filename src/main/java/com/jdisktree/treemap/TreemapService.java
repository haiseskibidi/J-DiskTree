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

        // Recursive Precision: Stop only if the block is too small to be meaningful on screen
        if ((double) node.size() / totalRootSize < MIN_SIZE_FRACTION || w < 4 || h < 4) {
            return;
        }

        List<FileNode> children = node.children().stream()
                .filter(c -> c.size() > 0)
                .sorted(Comparator.comparingLong(FileNode::size).reversed())
                .collect(Collectors.toList());

        if (children.isEmpty()) return;

        squarifyIterative(children, new Rect(x, y, w, h), node.size(), result, totalRootSize);
    }

    private void squarifyIterative(List<FileNode> children, Rect initialBounds, long totalSize, List<TreeMapRect> result, long totalRootSize) {
        Rect bounds = initialBounds;
        double remainingSize = (double) totalSize;
        int i = 0;
        int n = children.size();

        while (i < n) {
            List<FileNode> row = new ArrayList<>();
            double side = Math.min(bounds.w, bounds.h);
            double containerArea = bounds.w * bounds.h;
            
            double currentWorst = Double.MAX_VALUE;
            double rowTotalSize = 0;

            // Try adding elements to the row as long as the worst aspect ratio improves
            while (i < n) {
                FileNode nextChild = children.get(i);
                double nextRowTotalSize = rowTotalSize + nextChild.size();
                
                double rowArea = (nextRowTotalSize / remainingSize) * containerArea;
                double nextWorst = calculateWorstRatio(row, nextChild, rowArea, side, remainingSize, containerArea);

                if (row.isEmpty() || currentWorst >= nextWorst) {
                    row.add(nextChild);
                    rowTotalSize = nextRowTotalSize;
                    currentWorst = nextWorst;
                    i++;
                } else {
                    break;
                }
            }

            // Layout the completed row
            bounds = layoutRowIterative(row, rowTotalSize, bounds, remainingSize, result, totalRootSize);
            remainingSize -= rowTotalSize;
        }
    }

    private double calculateWorstRatio(List<FileNode> row, FileNode nextChild, double rowArea, double side, double totalSize, double containerArea) {
        double maxWeight = nextChild.size();
        double minWeight = nextChild.size();
        
        for (FileNode node : row) {
            maxWeight = Math.max(maxWeight, node.size());
            minWeight = Math.min(minWeight, node.size());
        }

        double maxArea = (maxWeight / totalSize) * containerArea;
        double minArea = (minWeight / totalSize) * containerArea;

        return Math.max(
            (side * side * maxArea) / (rowArea * rowArea),
            (rowArea * rowArea) / (side * side * minArea)
        );
    }

    private Rect layoutRowIterative(List<FileNode> row, double rowTotalSize, Rect bounds, double totalSize, List<TreeMapRect> result, long totalRootSize) {
        double rowArea = (rowTotalSize / totalSize) * (bounds.w * bounds.h);
        boolean horizontal = bounds.w >= bounds.h;
        
        double rowWidth = horizontal ? rowArea / bounds.h : bounds.w;
        double rowHeight = horizontal ? bounds.h : rowArea / bounds.w;

        double curX = bounds.x;
        double curY = bounds.y;

        double containerArea = bounds.w * bounds.h;

        for (FileNode child : row) {
            double childArea = ((double) child.size() / totalSize) * containerArea;
            double childWidth = horizontal ? rowWidth : childArea / rowHeight;
            double childHeight = horizontal ? childArea / rowWidth : rowHeight;

            calculateRecursive(child, curX, curY, childWidth, childHeight, result, totalRootSize);

            if (horizontal) curY += childHeight;
            else curX += childWidth;
        }

        if (horizontal) return new Rect(bounds.x + rowWidth, bounds.y, bounds.w - rowWidth, bounds.h);
        else return new Rect(bounds.x, bounds.y + rowHeight, bounds.w, bounds.h - rowHeight);
    }

    private record Rect(double x, double y, double w, double h) {}
}
