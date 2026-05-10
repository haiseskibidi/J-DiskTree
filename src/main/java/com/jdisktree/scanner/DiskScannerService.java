package com.jdisktree.scanner;

import com.jdisktree.domain.FileNode;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Extreme-performance parallel disk scanner using ForkJoinPool.
 * Utilizes "Directory-Granular Parallelism" to saturate NVMe queues
 * without the memory overhead of per-file Task allocations.
 */
public class DiskScannerService {

    private final ScanProgressListener listener;
    private final AtomicLong totalFiles = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong lastUpdate = new AtomicLong(System.currentTimeMillis());

    public DiskScannerService(ScanProgressListener listener) {
        this.listener = listener;
    }

    public FileNode scan(Path rootPath) {
        totalFiles.set(0);
        totalBytes.set(0);

        // Use the common pool to utilize all available processor cores for I/O scheduling
        try (ForkJoinPool pool = new ForkJoinPool()) {
            return pool.invoke(new DirectoryScanTask(rootPath));
        }
    }

    public ScanProgress getProgress() {
        return new ScanProgress(totalFiles.get(), totalBytes.get(), "");
    }

    private class DirectoryScanTask extends RecursiveTask<FileNode> {
        private final Path dirPath;

        DirectoryScanTask(Path dirPath) {
            this.dirPath = dirPath;
        }

        @Override
        protected FileNode compute() {
            List<FileNode> childNodes = new ArrayList<>();
            List<DirectoryScanTask> subTasks = new ArrayList<>();
            long dirSize = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                for (Path entry : stream) {
                    if (Files.isSymbolicLink(entry)) {
                        continue;
                    }

                    BasicFileAttributes attrs;
                    try {
                        attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                    } catch (IOException e) {
                        continue; // Skip files we cannot read attributes for
                    }

                    if (attrs.isDirectory()) {
                        subTasks.add(new DirectoryScanTask(entry));
                    } else {
                        long size = attrs.size();
                        String name = entry.getFileName() != null ? entry.getFileName().toString() : entry.toString();
                        childNodes.add(FileNode.file(name, entry.toAbsolutePath().toString(), size));
                        dirSize += size;
                        
                        totalFiles.incrementAndGet();
                        totalBytes.addAndGet(size);
                        
                        // Throttled UI updates (avoid locking in parallel streams)
                        long now = System.currentTimeMillis();
                        long last = lastUpdate.get();
                        if (now - last > 50 && lastUpdate.compareAndSet(last, now)) {
                            if (listener != null) {
                                listener.onProgress(new ScanProgress(totalFiles.get(), totalBytes.get(), entry.toAbsolutePath().toString()));
                            }
                        }
                    }
                }
            } catch (AccessDeniedException e) {
                // Silently swallow protected system folders
            } catch (IOException e) {
                // Silently swallow other I/O errors
            }

            // Using invokeAll for optimal work-stealing and parallelism across cores
            if (!subTasks.isEmpty()) {
                invokeAll(subTasks);
                for (DirectoryScanTask task : subTasks) {
                    FileNode subDirNode = task.join();
                    if (subDirNode != null) {
                        childNodes.add(subDirNode);
                        dirSize += subDirNode.size();
                    }
                }
            }

            String name = dirPath.getFileName() != null ? dirPath.getFileName().toString() : dirPath.toString();
            return FileNode.directory(name, dirPath.toAbsolutePath().toString(), dirSize, childNodes);
        }
    }
}
