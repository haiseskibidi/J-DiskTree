package com.jdisktree.scanner;

import com.jdisktree.domain.FileNode;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for high-performance multi-threaded disk scanning using Java NIO and ForkJoinPool.
 */
public class DiskScannerService {

    private final ForkJoinPool pool;
    private final ScanProgressListener listener;
    private final AtomicLong totalFiles = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);

    public DiskScannerService(ScanProgressListener listener) {
        this(ForkJoinPool.commonPool(), listener);
    }

    public DiskScannerService(ForkJoinPool pool, ScanProgressListener listener) {
        this.pool = pool;
        this.listener = listener;
    }

    /**
     * Scans the given path and returns the root FileNode.
     *
     * @param rootPath The path to start scanning from.
     * @return The immutable tree of FileNodes.
     */
    public FileNode scan(Path rootPath) {
        totalFiles.set(0);
        totalBytes.set(0);
        return pool.invoke(new ScanTask(rootPath));
    }

    public ScanProgress getProgress() {
        return new ScanProgress(totalFiles.get(), totalBytes.get(), "");
    }

    private class ScanTask extends RecursiveTask<FileNode> {
        private final Path path;

        ScanTask(Path path) {
            this.path = path;
        }

        @Override
        protected FileNode compute() {
            String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
            String absolutePath = path.toAbsolutePath().toString();

            if (Files.isSymbolicLink(path)) {
                return FileNode.file(name, absolutePath, 0);
            }

            if (Files.isDirectory(path)) {
                List<ScanTask> subTasks = new ArrayList<>();
                List<FileNode> children = new ArrayList<>();
                long dirSize = 0;

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path entry : stream) {
                        ScanTask task = new ScanTask(entry);
                        task.fork();
                        subTasks.add(task);
                    }
                } catch (AccessDeniedException e) {
                    // System-protected folder, skip silently as per mandate
                    return FileNode.directory(name, absolutePath, 0, List.of());
                } catch (IOException e) {
                    // Log or handle other IO errors
                    return FileNode.directory(name, absolutePath, 0, List.of());
                }

                for (ScanTask task : subTasks) {
                    FileNode child = task.join();
                    children.add(child);
                    dirSize += child.size();
                }

                updateProgress(absolutePath);
                return FileNode.directory(name, absolutePath, dirSize, children);
            } else {
                long size = 0;
                try {
                    size = Files.size(path);
                } catch (IOException e) {
                    // Ignore inaccessible file size
                }
                totalFiles.incrementAndGet();
                totalBytes.addAndGet(size);
                updateProgress(absolutePath);
                return FileNode.file(name, absolutePath, size);
            }
        }

        private void updateProgress(String currentPath) {
            if (listener != null) {
                // Throttling or strategic updates could be added here
                listener.onProgress(new ScanProgress(
                        totalFiles.get(),
                        totalBytes.get(),
                        currentPath
                ));
            }
        }
    }
}
