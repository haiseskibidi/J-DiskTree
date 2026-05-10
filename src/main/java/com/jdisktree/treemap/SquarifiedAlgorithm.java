package com.jdisktree.treemap;

import com.jdisktree.domain.FileNode;

import java.util.ArrayList;
import java.util.List;

public class SquarifiedAlgorithm {

    public interface RectConsumer {
        void accept(FileNode node, double x, double y, double w, double h);
    }

    public static void compute(List<FileNode> children, double x, double y, double w, double h, long totalSize, RectConsumer childConsumer) {
        Rect bounds = new Rect(x, y, w, h);
        double remainingSize = (double) totalSize;
        int i = 0;
        int n = children.size();

        while (i < n) {
            List<FileNode> row = new ArrayList<>();
            double side = Math.min(bounds.w, bounds.h);
            double containerArea = bounds.w * bounds.h;
            
            double currentWorst = Double.MAX_VALUE;
            double rowTotalSize = 0;

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

            bounds = layoutRowIterative(row, rowTotalSize, bounds, remainingSize, totalSize, childConsumer);
            remainingSize -= rowTotalSize;
        }
    }

    private static double calculateWorstRatio(List<FileNode> row, FileNode nextChild, double rowArea, double side, double totalSize, double containerArea) {
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

    private static Rect layoutRowIterative(List<FileNode> row, double rowTotalSize, Rect bounds, double totalSize, long rootTotalSize, RectConsumer childConsumer) {
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

            childConsumer.accept(child, curX, curY, childWidth, childHeight);

            if (horizontal) curY += childHeight;
            else curX += childWidth;
        }

        if (horizontal) return new Rect(bounds.x + rowWidth, bounds.y, bounds.w - rowWidth, bounds.h);
        else return new Rect(bounds.x, bounds.y + rowHeight, bounds.w, bounds.h - rowHeight);
    }

    private record Rect(double x, double y, double w, double h) {}
}
