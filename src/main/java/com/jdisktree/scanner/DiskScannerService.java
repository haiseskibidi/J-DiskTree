package com.jdisktree.scanner;

import com.jdisktree.domain.FileNode;
import com.jdisktree.domain.ScanExclusion;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

/**
 * Extreme-performance parallel disk scanner using ForkJoinPool.
 * Implements an industrial cycle detector using unique file keys (inodes)
 * to prevent infinite loops caused by symbolic links or junctions.
 */
public class DiskScannerService {

    private static final int MAX_DEPTH = 128; // Industry-standard safe depth
    private final ScanProgressListener listener;
    private final List<ScanExclusion> exclusions;
    private final LongAdder totalFiles = new LongAdder();
    private final LongAdder totalBytes = new LongAdder();
    private final ConcurrentHashMap<String, Boolean> visitedCanonicalPaths = new ConcurrentHashMap<>();
    private volatile String currentPath = "";

    public DiskScannerService(ScanProgressListener listener, List<ScanExclusion> exclusions) {
        this.listener = listener;
        this.exclusions = exclusions != null ? exclusions : new ArrayList<>();
    }

    public FileNode scan(Path rootPath) {
        totalFiles.reset();
        totalBytes.reset();
        visitedCanonicalPaths.clear();
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
            FileNode root = pool.invoke(new DirectoryScanTask(rootPath, 0));
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
        private final int depth;

        DirectoryScanTask(Path dirPath, int depth) {
            this.dirPath = dirPath;
            this.depth = depth;
        }

        private boolean isExcluded(Path path) {
            if (exclusions.isEmpty()) return false;
            String fileName = path.getFileName() != null ? path.getFileName().toString() : "";
            String nameLower = fileName.toLowerCase();
            
            for (ScanExclusion exclusion : exclusions) {
                if (!exclusion.isEnabled()) continue;
                String pat = exclusion.pattern().toLowerCase();
                
                if (pat.startsWith("*.")) {
                    if (nameLower.endsWith(pat.substring(1))) return true;
                } else if (pat.startsWith(".")) {
                    if (nameLower.endsWith(pat)) return true;
                } else if (nameLower.equals(pat)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected FileNode compute() {
            if (depth > MAX_DEPTH) return null;
            
            // Check exclusion before resolving canonical path to save I/O
            if (isExcluded(dirPath)) {
                return null;
            }

            try {
                // THE ONLY RELIABLE WAY ON WINDOWS: Resolve the CANONICAL (real) path.
                // This resolves all junctions, symlinks, and shortcuts to their TRUE physical location.
                String canonical = dirPath.toRealPath().toString().toLowerCase();
                
                // If we've already been at this PHYSICAL location, abort the loop.
                if (visitedCanonicalPaths.putIfAbsent(canonical, Boolean.TRUE) != null) {
                    return null;
                }
            } catch (IOException | SecurityException e) {
                // If we can't verify the path, we skip it for safety
                return null;
            }

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
                            if (!isExcluded(dir)) {
                                subTasks.add(new DirectoryScanTask(dir, depth + 1));
                            }
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isExcluded(file)) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (attrs.isDirectory()) {
                            subTasks.add(new DirectoryScanTask(file, depth + 1));
                        } else {
                            long size = attrs.size();
                            String name = file.getFileName() != null ? file.getFileName().toString() : file.toString();
                            childNodes.add(FileNode.file(name, file.toAbsolutePath().toString(), size));
                            dirSize[0] += size;
                            
                            totalFiles.increment();
                            totalBytes.add(size);

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
                // Silently swallow
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
