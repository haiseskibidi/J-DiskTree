package com.jdisktree.scanner;

import com.jdisktree.domain.FileNode;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance disk scanner using java.nio.file.Files.walkFileTree.
 * Designed for speed and minimal memory footprint.
 */
public class DiskScannerService {

    private final ScanProgressListener listener;
    private final AtomicLong totalFiles = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);

    public DiskScannerService(ScanProgressListener listener) {
        this.listener = listener;
    }

    /**
     * Scans the given path and returns the root FileNode.
     * Uses a single-pass bottom-up tree building algorithm.
     */
    public FileNode scan(Path rootPath) throws IOException {
        totalFiles.set(0);
        totalBytes.set(0);

        final FileNode[] rootResult = new FileNode[1];
        Map<Path, List<FileNode>> childrenMap = new HashMap<>();
        Map<Path, Long> sizeMap = new HashMap<>();

        Files.walkFileTree(rootPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                childrenMap.put(dir, new ArrayList<>(32));
                sizeMap.put(dir, 0L);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (Files.isSymbolicLink(file)) return FileVisitResult.CONTINUE;

                long size = attrs.size();
                String name = file.getFileName() != null ? file.getFileName().toString() : file.toString();
                FileNode node = FileNode.file(name, file.toAbsolutePath().toString(), size);
                
                Path parent = file.getParent();
                if (parent != null && childrenMap.containsKey(parent)) {
                    childrenMap.get(parent).add(node);
                    sizeMap.put(parent, sizeMap.get(parent) + size);
                }

                totalFiles.incrementAndGet();
                totalBytes.addAndGet(size);
                
                long currentCount = totalFiles.get();
                if (currentCount % 100 == 0) {
                    updateProgress(file.toAbsolutePath().toString());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                List<FileNode> children = childrenMap.remove(dir);
                long size = sizeMap.remove(dir);
                String name = dir.getFileName() != null ? dir.getFileName().toString() : dir.toString();
                
                FileNode dirNode = FileNode.directory(name, dir.toAbsolutePath().toString(), size, children);

                Path parent = dir.getParent();
                if (parent != null && childrenMap.containsKey(parent)) {
                    childrenMap.get(parent).add(dirNode);
                    sizeMap.put(parent, sizeMap.get(parent) + size);
                }

                if (dir.equals(rootPath)) {
                    rootResult[0] = dirNode;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        return rootResult[0];
    }

    public ScanProgress getProgress() {
        return new ScanProgress(totalFiles.get(), totalBytes.get(), "");
    }

    private void updateProgress(String currentPath) {
        if (listener != null) {
            listener.onProgress(new ScanProgress(totalFiles.get(), totalBytes.get(), currentPath));
        }
    }
}
