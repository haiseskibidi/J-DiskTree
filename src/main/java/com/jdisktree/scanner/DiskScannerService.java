package com.jdisktree.scanner;

import com.jdisktree.domain.FileNode;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

/**
 * Extreme-performance parallel disk scanner using ForkJoinPool.
 * Utilizes "Directory-Granular Parallelism" to saturate NVMe queues.
 * Decouples UI reporting into a separate thread to guarantee zero contention and prevent UI freezes.
 */
public class DiskScannerService {

    private final ScanProgressListener listener;
    private final LongAdder totalFiles = new LongAdder();
    private final LongAdder totalBytes = new LongAdder();
    private volatile String currentPath = "";

    public DiskScannerService(ScanProgressListener listener) {
        this.listener = listener;
    }

    public FileNode scan(Path rootPath) {
        totalFiles.reset();
        totalBytes.reset();
        currentPath = rootPath.toString();

        Thread reporter = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(100);
                    if (listener != null) {
                        listener.onProgress(new ScanProgress(totalFiles.sum(), totalBytes.sum(), currentPath));
                    }
                }
            } catch (InterruptedException e) {
                // Scan finished
            }
        });
        reporter.setDaemon(true);
        reporter.start();

        try (ForkJoinPool pool = new ForkJoinPool()) {
            FileNode root = pool.invoke(new DirectoryScanTask(rootPath));
            reporter.interrupt();
            
            // Final progress update
            if (listener != null) {
                listener.onProgress(new ScanProgress(totalFiles.sum(), totalBytes.sum(), "Finished"));
            }
            return root;
        }
    }

    public ScanProgress getProgress() {
        return new ScanProgress(totalFiles.sum(), totalBytes.sum(), "");
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
            long[] dirSize = {0};
            boolean[] isRoot = {true};

            try {
                Files.walkFileTree(dirPath, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (isRoot[0]) {
                            isRoot[0] = false;
                            return FileVisitResult.CONTINUE;
                        } else {
                            if (!attrs.isSymbolicLink()) {
                                subTasks.add(new DirectoryScanTask(dir));
                            }
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (attrs.isSymbolicLink()) return FileVisitResult.CONTINUE;

                        if (attrs.isDirectory()) {
                            subTasks.add(new DirectoryScanTask(file));
                        } else {
                            long size = attrs.size();
                            String name = file.getFileName() != null ? file.getFileName().toString() : file.toString();
                            childNodes.add(FileNode.file(name, file.toAbsolutePath().toString(), size));
                            dirSize[0] += size;
                            
                            totalFiles.increment();
                            totalBytes.add(size);

                            // Randomly sample path for UI (extremely fast, zero locks)
                            if (ThreadLocalRandom.current().nextInt(1000) == 0) {
                                currentPath = file.toString();
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception e) {
                // Silently swallow ALL exceptions (protected/system folders)
            }

            if (!subTasks.isEmpty()) {
                invokeAll(subTasks);
                for (DirectoryScanTask task : subTasks) {
                    FileNode subDirNode = task.join();
                    if (subDirNode != null) {
                        childNodes.add(subDirNode);
                        dirSize[0] += subDirNode.size();
                    }
                }
            }

            String name = dirPath.getFileName() != null ? dirPath.getFileName().toString() : dirPath.toString();
            return FileNode.directory(name, dirPath.toAbsolutePath().toString(), dirSize[0], childNodes);
        }
    }
}
