package com.jdisktree.treemap;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.TreeMapRect;
import org.junit.Test;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class TreemapServiceTest {

    @Test
    public void testCalculateLayoutBasic() {
        // Mock structure:
        // root (1000 bytes, lastModified = 100)
        //  - fileA (600 bytes, lastModified = 100)
        //  - dirB (400 bytes, lastModified = 100)
        //    - fileB1 (200 bytes, lastModified = 100)
        //    - fileB2 (200 bytes, lastModified = 100)
        
        long now = 100L;
        FileNode b1 = FileNode.file("B1", "/root/B/B1", 200, now);
        FileNode b2 = FileNode.file("B2", "/root/B/B2", 200, now);
        FileNode b = FileNode.directory("B", "/root/B", 400, now, List.of(b1, b2));
        FileNode a = FileNode.file("A", "/root/A", 600, now);
        FileNode root = FileNode.directory("root", "/root", 1000, now, List.of(a, b));

        TreemapService service = new TreemapService();
        double width = 1000;
        double height = 1000;
        List<TreeMapRect> rects = service.calculateLayout(root, 0, 0, width, height);

        assertNotNull(rects);
        assertFalse(rects.isEmpty());

        // Verify total area of leaf elements matches the layout bounds
        double totalLeafArea = 0;
        for (TreeMapRect rect : rects) {
            if (!rect.isDirectory()) {
                totalLeafArea += rect.width() * rect.height();
            }
        }
        assertEquals("Total leaf area must equal the layout bounds area", width * height, totalLeafArea, 0.001);
    }

    @Test
    public void testClippingSliverLayout() {
        // If the bounds given to a directory are extremely small (width or height < 3.0),
        // the layout calculations should stop recursing to avoid sub-pixel slivers.
        long now = 100L;
        FileNode file1 = FileNode.file("file1.txt", "/root/dir1/file1.txt", 500, now);
        FileNode file2 = FileNode.file("file2.txt", "/root/dir1/file2.txt", 500, now);
        FileNode dir1 = FileNode.directory("dir1", "/root/dir1", 1000, now, List.of(file1, file2));
        FileNode root = FileNode.directory("root", "/root", 1000, now, List.of(dir1));

        TreemapService service = new TreemapService();
        // Give it a layout width of 2.0 (less than 3.0)
        List<TreeMapRect> rects = service.calculateLayout(root, 0, 0, 2.0, 1000.0);

        // It should contain the root node and dir1, but none of the files inside dir1 since w < 3.0 triggers clipping
        assertNotNull(rects);
        for (TreeMapRect rect : rects) {
            assertNotEquals("/root/dir1/file1.txt", rect.path());
            assertNotEquals("/root/dir1/file2.txt", rect.path());
        }
    }
}
