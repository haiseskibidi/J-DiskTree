package com.jdisktree.scanner;

import com.jdisktree.domain.FileNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScannerManualTest {

    public static void main(String[] args) throws IOException {
        Path tempDir = Files.createTempDirectory("jdisktree-test-");
        try {
            // Create a mock structure:
            // /root
            //   /dir1 (100 bytes)
            //     file1.txt (100 bytes)
            //   /dir2 (300 bytes)
            //     file2.txt (200 bytes)
            //     file3.txt (100 bytes)
            //   root_file.txt (50 bytes)
            // Total: 450 bytes, 4 files, 3 directories = 7 nodes

            Path dir1 = tempDir.resolve("dir1");
            Files.createDirectory(dir1);
            Files.write(dir1.resolve("file1.txt"), new byte[100]);

            Path dir2 = tempDir.resolve("dir2");
            Files.createDirectory(dir2);
            Files.write(dir2.resolve("file2.txt"), new byte[200]);
            Files.write(dir2.resolve("file3.txt"), new byte[100]);

            Files.write(tempDir.resolve("root_file.txt"), new byte[50]);

            List<ScanProgress> progressUpdates = Collections.synchronizedList(new ArrayList<>());
            DiskScannerService scanner = new DiskScannerService(progressUpdates::add);

            System.out.println("Starting scan of: " + tempDir);
            FileNode root = scanner.scan(tempDir);

            System.out.println("Scan complete.");
            System.out.println("Root name: " + root.name());
            System.out.println("Total size: " + root.size() + " bytes (Expected: 450)");
            System.out.println("Progress updates received: " + progressUpdates.size());

            if (root.size() == 450) {
                System.out.println("SUCCESS: Size aggregation is correct.");
            } else {
                System.out.println("FAILURE: Size aggregation mismatch!");
            }

            // Verify progress updates
            if (!progressUpdates.isEmpty()) {
                ScanProgress lastProgress = progressUpdates.get(progressUpdates.size() - 1);
                System.out.println("Last progress update: Files=" + lastProgress.filesScanned() + ", Bytes=" + lastProgress.bytesScanned());
            }

        } finally {
            // Cleanup would go here in a real test, but for manual check we can leave it or clean up
            deleteDirectory(tempDir);
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    deleteDirectory(entry);
                }
            }
        }
        Files.delete(path);
    }
}
