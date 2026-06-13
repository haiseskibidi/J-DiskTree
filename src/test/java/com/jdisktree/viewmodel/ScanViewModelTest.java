package com.jdisktree.viewmodel;

import com.jdisktree.state.ScanStatus;
import com.jdisktree.state.UiState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class ScanViewModelTest {

    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("jdisktree-vm-test-");
        Files.write(tempDir.resolve("file1.txt"), new byte[100]); // 100 bytes
        Path subdir = tempDir.resolve("subdir");
        Files.createDirectory(subdir);
        Files.write(subdir.resolve("file2.png"), new byte[200]); // 200 bytes
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testStartScanTransitions() throws InterruptedException {
        CountDownLatch completedLatch = new CountDownLatch(1);
        AtomicReference<UiState> finalState = new AtomicReference<>();

        ScanViewModel viewModel = new ScanViewModel(state -> {
            if (state.status() == ScanStatus.COMPLETED) {
                finalState.set(state);
                completedLatch.countDown();
            }
        });

        // Verify initial state
        assertEquals(ScanStatus.IDLE, viewModel.getCurrentState().status());

        // Start scanning with exclusions list empty
        viewModel.startScan(tempDir, 800, 600, Collections.emptyList());

        // Wait for scan to complete and state to be updated
        boolean completed = completedLatch.await(5, TimeUnit.SECONDS);
        assertTrue("Scan did not complete within timeout", completed);

        UiState state = finalState.get();
        assertNotNull(state);
        assertEquals(ScanStatus.COMPLETED, state.status());
        assertNotNull(state.rootNode());
        assertEquals(300L, state.rootNode().size()); // 100 + 200 = 300 bytes

        // Check that Treemap rectangles are generated
        assertFalse("Treemap rectangles list should not be empty", state.rects().isEmpty());

        // Check extension stats aggregation (txt and png)
        assertNotNull(state.typeStats());
        assertFalse(state.typeStats().isEmpty());
        
        boolean hasTxt = state.typeStats().stream().anyMatch(stat -> stat.extension().equals("txt") && stat.count() == 1 && stat.totalSize() == 100);
        boolean hasPng = state.typeStats().stream().anyMatch(stat -> stat.extension().equals("png") && stat.count() == 1 && stat.totalSize() == 200);
        
        assertTrue("Type stats must contain txt info", hasTxt);
        assertTrue("Type stats must contain png info", hasPng);
    }

    @Test
    public void testSearchQueryHighlighting() {
        ScanViewModel viewModel = new ScanViewModel(null);
        viewModel.setSearchQuery("txt");
        
        assertEquals("txt", viewModel.getCurrentState().searchQuery());
    }

    @Test
    public void testAgeFilterDays() {
        ScanViewModel viewModel = new ScanViewModel(null);
        viewModel.setAgeFilter(30);
        
        assertEquals(30, viewModel.getCurrentState().ageFilterDays());
    }

    private void deleteDirectory(Path path) throws IOException {
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
