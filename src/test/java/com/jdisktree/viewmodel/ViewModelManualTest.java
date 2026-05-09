package com.jdisktree.viewmodel;

import com.jdisktree.state.ScanStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ViewModelManualTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("vm-test-");
        Files.write(tempDir.resolve("file1.txt"), new byte[1024]);
        Files.createDirectory(tempDir.resolve("subdir"));
        Files.write(tempDir.resolve("subdir").resolve("file2.txt"), new byte[2048]);

        CountDownLatch latch = new CountDownLatch(1);
        
        ScanViewModel viewModel = new ScanViewModel(state -> {
            System.out.println("State changed: " + state.status());
            if (state.progress() != null) {
                System.out.println("  Progress: " + state.progress().filesScanned() + " files");
            }
            if (state.status() == ScanStatus.COMPLETED) {
                System.out.println("  Rectangles: " + state.rects().size());
                latch.countDown();
            }
            if (state.status() == ScanStatus.ERROR) {
                System.err.println("  Error: " + state.errorMessage());
                latch.countDown();
            }
        });

        System.out.println("Starting scan...");
        viewModel.startScan(tempDir, 800, 600);

        if (latch.await(5, TimeUnit.SECONDS)) {
            System.out.println("SUCCESS: Scan completed through ViewModel.");
        } else {
            System.err.println("FAILURE: Scan timed out!");
        }

        deleteDirectory(tempDir);
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
