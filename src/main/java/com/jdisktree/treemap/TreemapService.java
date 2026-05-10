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
        // PHYSICAL RECURSION CLIPPING
        // Stop completely if the allocated space is too thin (prevents slivers and barcode lines).
        // The empty space will naturally be filled by the parent directory's background color.
        if (w < 3.0 || h < 3.0) {
            return;
        }

        String extension = node.isDirectory() ? "" : getExtension(node.name());
        result.add(new TreeMapRect(node.absolutePath(), x, y, w, h, node.isDirectory(), extension, node.size()));

        if (!node.isDirectory() || node.children().isEmpty()) {
            return;
        }

        double areaPerByte = (w * h) / (double) node.size();

        List<FileNode> allChildren = node.children().stream()
                .filter(c -> c.size() > 0 && (c.size() * areaPerByte) >= 1.0)
                .sorted(Comparator.comparingLong(FileNode::size).reversed())
                .collect(Collectors.toList());

        if (allChildren.isEmpty()) {
            return;
        }

        SquarifiedAlgorithm.compute(
            allChildren, x, y, w, h, node.size(),
            (childNode, cx, cy, cw, ch) -> calculateRecursive(childNode, cx, cy, cw, ch, result)
        );
    }
}
