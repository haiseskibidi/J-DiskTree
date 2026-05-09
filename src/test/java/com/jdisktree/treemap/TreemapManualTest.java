package com.jdisktree.treemap;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.TreeMapRect;
import java.util.List;

public class TreemapManualTest {

    public static void main(String[] args) {
        // Mock structure
        // Root (1000)
        //  - A (600)
        //  - B (400)
        //    - B1 (200)
        //    - B2 (200)
        
        FileNode b1 = FileNode.file("B1", "/root/B/B1", 200);
        FileNode b2 = FileNode.file("B2", "/root/B/B2", 200);
        FileNode b = FileNode.directory("B", "/root/B", 400, List.of(b1, b2));
        FileNode a = FileNode.file("A", "/root/A", 600);
        FileNode root = FileNode.directory("root", "/root", 1000, List.of(a, b));

        TreemapService service = new TreemapService();
        double width = 1000;
        double height = 1000;
        List<TreeMapRect> rects = service.calculateLayout(root, 0, 0, width, height);

        System.out.println("Total rectangles: " + rects.size());
        
        double totalLeafArea = 0;
        for (TreeMapRect rect : rects) {
            System.out.printf("Path: %s, Rect: [%.1f, %.1f, %.1f, %.1f], isDir: %b%n", 
                rect.path(), rect.x(), rect.y(), rect.width(), rect.height(), rect.isDirectory());
            
            if (!rect.isDirectory()) {
                totalLeafArea += rect.width() * rect.height();
            }
        }

        double expectedArea = width * height;
        System.out.println("Total Leaf Area: " + totalLeafArea);
        System.out.println("Expected Area: " + expectedArea);

        if (Math.abs(totalLeafArea - expectedArea) < 0.001) {
            System.out.println("SUCCESS: Treemap area calculation is correct.");
        } else {
            System.out.println("FAILURE: Area mismatch!");
        }
    }
}
