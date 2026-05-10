package com.jdisktree.scanner;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Service for performing file system operations like deletion and opening in explorer.
 */
public class FileOperationsService {

    /**
     * Opens Windows Explorer and selects the specified file/folder.
     */
    public void openInExplorer(String path) {
        try {
            new ProcessBuilder("explorer.exe", "/select,", path).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Moves a file or directory to the system Recycle Bin.
     */
    public boolean moveToTrash(String path) {
        if (!Desktop.isDesktopSupported()) return false;
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) return false;

        try {
            return desktop.moveToTrash(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a file or directory permanently from the disk.
     */
    public boolean deletePermanently(String path) {
        Path p = Paths.get(path);
        try {
            if (Files.isDirectory(p)) {
                try (Stream<Path> walk = Files.walk(p)) {
                    walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                }
            } else {
                Files.delete(p);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Copies the given text to the system clipboard.
     */
    public void copyToClipboard(String text) {
        java.awt.Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(text), null);
    }
}
