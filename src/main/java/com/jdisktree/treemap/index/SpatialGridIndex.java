package com.jdisktree.treemap.index;

import com.jdisktree.domain.TreeMapRect;
import java.util.ArrayList;
import java.util.List;

/**
 * A spatial index that partitions the 2D space into a grid for fast lookups.
 * Reduces hover detection from O(N) to O(1) on average.
 */
public class SpatialGridIndex {
    private final int rows;
    private final int cols;
    private final double width;
    private final double height;
    private final List<TreeMapRect>[][] grid;

    @SuppressWarnings("unchecked")
    public SpatialGridIndex(int rows, int cols, double width, double height) {
        this.rows = rows;
        this.cols = cols;
        this.width = width;
        this.height = height;
        this.grid = new ArrayList[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new ArrayList<>(8);
            }
        }
    }

    /**
     * Adds a rectangle to all grid cells it covers.
     */
    public void add(TreeMapRect rect) {
        int startCol = (int) (rect.x() / width * cols);
        int endCol = (int) ((rect.x() + rect.width()) / width * cols);
        int startRow = (int) (rect.y() / height * rows);
        int endRow = (int) ((rect.y() + rect.height()) / height * rows);

        // Clamp to grid bounds
        startCol = Math.max(0, Math.min(cols - 1, startCol));
        endCol = Math.max(0, Math.min(cols - 1, endCol));
        startRow = Math.max(0, Math.min(rows - 1, startRow));
        endRow = Math.max(0, Math.min(rows - 1, endRow));

        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                grid[r][c].add(rect);
            }
        }
    }

    /**
     * Returns the list of rectangles in the grid cell corresponding to the given coordinates.
     */
    public List<TreeMapRect> query(double x, double y) {
        int col = (int) (x / width * cols);
        int row = (int) (y / height * rows);

        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return List.of();
        }

        return grid[row][col];
    }
}
