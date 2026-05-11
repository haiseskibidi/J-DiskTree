package com.jdisktree.treemap;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.TreeMapRect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calculating layout using the Squarified Treemap algorithm.
 * Filters files mathematically to prevent rendering artifacts (slivers).
 */
public class TreemapService {

    public List<TreeMapRect> calculateLayout(FileNode root, double x, double y, double width, double height) {
        List<TreeMapRect> result = new ArrayList<>();
        if (root.size() > 0) {
            calculateRecursive(root, x, y, width, height, result);
        }
        return result;
    }

    private String getExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < name.length() - 1) {
            return name.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    private void calculateRecursive(FileNode node, double x, double y, double w, double h, List<TreeMapRect> result) {
        if (w < 3.0 || h < 3.0) {
            return;
        }

        String extension = node.isDirectory() ? "" : getExtension(node.name());
        result.add(new TreeMapRect(node.absolutePath(), x, y, w, h, node.isDirectory(), extension, node.size(), node.lastModified()));

        if (!node.isDirectory() || node.children().isEmpty()) {
            return;
        }

        // Calculate visible size for this specific layout pass
        double areaPerByte = (w * h) / (double) node.size();
        List<FileNode> visibleChildren = node.children().stream()
                .filter(c -> c.size() > 0 && (c.size() * areaPerByte) >= 1.0)
                .sorted(Comparator.comparingLong(FileNode::size).reversed())
                .toList();

        if (visibleChildren.isEmpty()) {
            return;
        }

        // IMPORTANT: Use the sum of visible children as the base for layout
        // to ensure they collectively fill exactly w * h.
        long visibleTotalSize = visibleChildren.stream().mapToLong(FileNode::size).sum();

        SquarifiedAlgorithm.compute(
            visibleChildren, x, y, w, h, visibleTotalSize,
            (childNode, cx, cy, cw, ch) -> calculateRecursive(childNode, cx, cy, cw, ch, result)
        );
    }
}
