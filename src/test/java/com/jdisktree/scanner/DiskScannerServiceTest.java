package com.jdisktree.scanner;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.ScanExclusion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class DiskScannerServiceTest {

    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("jdisktree-scan-test-");
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testBasicScanAggregation() throws IOException {
        // Create mock structure:
        // /tempDir
        //   /dir1 (100 bytes)
        //     file1.txt (100 bytes)
        //   /dir2 (300 bytes)
        //     file2.txt (200 bytes)
        //     file3.txt (100 bytes)
        //   root_file.txt (50 bytes)
        // Expected total size: 450 bytes
        // Expected files scanned: 4
        
        Path dir1 = tempDir.resolve("dir1");
        Files.createDirectory(dir1);
        Files.write(dir1.resolve("file1.txt"), new byte[100]);

        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectory(dir2);
        Files.write(dir2.resolve("file2.txt"), new byte[200]);
        Files.write(dir2.resolve("file3.txt"), new byte[100]);

        Files.write(tempDir.resolve("root_file.txt"), new byte[50]);

        List<ScanProgress> progressUpdates = Collections.synchronizedList(new ArrayList<>());
        DiskScannerService scanner = new DiskScannerService(progressUpdates::add, Collections.emptyList());

        FileNode root = scanner.scan(tempDir);

        assertNotNull("Root node must not be null", root);
        assertEquals("Aggregate size must match expected sum", 450L, root.size());
        
        ScanProgress lastProgress = scanner.getProgress();
        assertEquals("Progress files scanned count must be correct", 4, lastProgress.filesScanned());
        assertEquals("Progress bytes scanned count must be correct", 450L, lastProgress.bytesScanned());
    }

    @Test
    public void testExclusions() throws IOException {
        // Create files with different extensions and patterns
        Path dir = tempDir.resolve("media");
        Files.createDirectory(dir);
        Files.write(dir.resolve("video.mp4"), new byte[1000]);
        Files.write(dir.resolve("image.png"), new byte[500]);
        Files.write(dir.resolve("doc.pdf"), new byte[200]);

        // Define exclusions
        List<ScanExclusion> exclusions = new ArrayList<>();
        exclusions.add(new ScanExclusion("*.mp4", true));
        exclusions.add(new ScanExclusion(".pdf", true));

        DiskScannerService scanner = new DiskScannerService(null, exclusions);
        FileNode root = scanner.scan(tempDir);

        assertNotNull(root);
        // Size should only include image.png (500 bytes) since mp4 and pdf are excluded
        assertEquals(500L, root.size());
    }

    @Test
    public void testCycleDetectionTerminates() throws IOException {
        // Create directory structure:
        // /tempDir
        //   /parent
        //     /child
        //       /linkToParent -> cyclic link pointing back to /parent
        Path parent = tempDir.resolve("parent");
        Files.createDirectory(parent);
        Path child = parent.resolve("child");
        Files.createDirectory(child);

        Path linkToParent = child.resolve("linkToParent");

        boolean linkCreated = false;
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // On Windows, directory junction (mklink /j) doesn't require Administrator privileges
                Process process = Runtime.getRuntime().exec(new String[]{
                    "cmd.exe", "/c", "mklink", "/j", 
                    linkToParent.toAbsolutePath().toString(), 
                    parent.toAbsolutePath().toString()
                });
                int exitCode = process.waitFor();
                linkCreated = (exitCode == 0);
            } else {
                // On Unix, standard symlinks don't require special privileges
                Files.createSymbolicLink(linkToParent, parent);
                linkCreated = true;
            }
        } catch (Exception e) {
            System.err.println("Could not create cyclic link/junction for cycle test: " + e.getMessage());
        }

        // If we couldn't create the cyclic link (e.g. system permissions or non-supported OS/FS),
        // we skip the cycle check but pass the test gracefully.
        if (!linkCreated) {
            System.out.println("Skipping cyclic symlink test as link could not be created on this environment.");
            return;
        }

        // Run scanner on the parent directory. It should detect the loop and stop safely
        DiskScannerService scanner = new DiskScannerService(null, Collections.emptyList());
        
        // Use a separate thread to ensure we don't hang forever if there's a regression
        Thread testThread = new Thread(() -> {
            scanner.scan(parent);
        });

        testThread.start();
        try {
            testThread.join(3000); // 3 seconds timeout
            if (testThread.isAlive()) {
                testThread.interrupt();
                fail("Scanner failed to detect recursion loop and hung (infinite loop detected!)");
            }
        } catch (InterruptedException e) {
            fail("Test interrupted");
        } finally {
            if (linkCreated) {
                try {
                    Files.deleteIfExists(linkToParent);
                } catch (IOException e) {
                    System.err.println("Could not delete cyclic link: " + e.getMessage());
                }
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            // Check if directory is a symbolic link or junction to prevent deleting linked targets
            if (!Files.isSymbolicLink(path)) {
                try (var stream = Files.newDirectoryStream(path)) {
                    for (Path entry : stream) {
                        deleteDirectory(entry);
                    }
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
