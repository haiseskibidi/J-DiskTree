package com.jdisktree.scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jdisktree.domain.FileNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Stack;

/**
 * Service for exporting the scanned file tree to various formats.
 */
public class ExportService {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Exports the file tree to a structured JSON file.
     */
    public void exportToJson(FileNode root, Path target) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(target)) {
            gson.toJson(root, writer);
        }
    }

    /**
     * Exports the file tree to a flat CSV file.
     * Format: Name, Path, Size (Bytes), IsDirectory
     */
    public void exportToCsv(FileNode root, Path target) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(target)) {
            // Add UTF-8 BOM for Windows/Excel compatibility
            writer.write('\uFEFF');

            // Header
            writer.write("Name,Path,Size(Bytes),IsDirectory");
            writer.newLine();

            // DFS Traversal to keep memory usage low even for massive trees
            Stack<FileNode> stack = new Stack<>();
            stack.push(root);

            while (!stack.isEmpty()) {
                FileNode node = stack.pop();
                
                // Write CSV line (escaping quotes for safety)
                String line = String.format("\"%s\",\"%s\",%d,%b",
                        escapeCsv(node.name()),
                        escapeCsv(node.absolutePath()),
                        node.size(),
                        node.isDirectory()
                );
                writer.write(line);
                writer.newLine();

                // Add children to stack
                if (node.isDirectory() && node.children() != null) {
                    for (FileNode child : node.children()) {
                        stack.push(child);
                    }
                }
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
