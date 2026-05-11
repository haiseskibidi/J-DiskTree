package com.jdisktree.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jdisktree.domain.FileColorConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Type;
import java.util.List;
import java.util.ArrayList;

/**
 * Service responsible for saving and loading application preferences and colors from the user's home directory.
 */
public class PreferencesService {

    private static final String DIR_NAME = ".jdisktree";
    private static final String SETTINGS_FILE = "settings.json";
    private static final String COLORS_FILE = "colors.json";

    private final Path configPath;
    private final Path colorsPath;
    private final Gson gson;

    public PreferencesService() {
        String userHome = System.getProperty("user.home");
        Path dirPath = Paths.get(userHome, DIR_NAME);
        this.configPath = dirPath.resolve(SETTINGS_FILE);
        this.colorsPath = dirPath.resolve(COLORS_FILE);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Reads Windows registry to get the system accent color.
     * Returns standard AARRGGBB hex string (e.g., "FFD32F2F") or null if failed.
     */
    public String getWindowsAccentColor() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) return null;

        try {
            Process process = Runtime.getRuntime().exec("reg query \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\DWM\" /v AccentColor");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("AccentColor")) {
                        String[] parts = line.split("\\s+");
                        String hex = parts[parts.length - 1]; // e.g. 0xffd32f2f (AABBGGRR)
                        if (hex.startsWith("0x")) {
                            long colorLong = Long.decode(hex);
                            // Windows stores as ABGR. We need ARGB.
                            // Extract bytes
                            int r = (int) (colorLong & 0xFF);
                            int g = (int) ((colorLong >> 8) & 0xFF);
                            int b = (int) ((colorLong >> 16) & 0xFF);
                            int a = (int) ((colorLong >> 24) & 0xFF);
                            if (a == 0) a = 0xFF; // Ensure opaque if alpha is 0

                            return String.format("%02X%02X%02X%02X", a, r, g, b);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Loads preferences from disk. Returns defaults if file doesn't exist or is corrupted.
     */
    public AppPreferences load() {
        if (!Files.exists(configPath)) {
            return AppPreferences.defaults();
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            AppPreferences prefs = gson.fromJson(reader, AppPreferences.class);
            return prefs != null ? prefs : AppPreferences.defaults();
        } catch (Exception e) {
            e.printStackTrace();
            return AppPreferences.defaults();
        }
    }

    /**
     * Saves preferences to disk. Creates the directory if it doesn't exist.
     */
    public void save(AppPreferences prefs) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                gson.toJson(prefs, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads custom file colors from disk. Returns default populated list if not found.
     */
    public List<FileColorConfig> loadColors() {
        if (!Files.exists(colorsPath)) {
            return defaultColors();
        }
        try (Reader reader = Files.newBufferedReader(colorsPath)) {
            Type listType = new TypeToken<ArrayList<FileColorConfig>>(){}.getType();
            List<FileColorConfig> colors = gson.fromJson(reader, listType);
            return colors != null && !colors.isEmpty() ? colors : defaultColors();
        } catch (Exception e) {
            e.printStackTrace();
            return defaultColors();
        }
    }

    private List<FileColorConfig> defaultColors() {
        return new ArrayList<>(List.of(
                new FileColorConfig("exe", "FFEF5350"),
                new FileColorConfig("dll", "FFEF5350"),
                new FileColorConfig("sys", "FFEF5350"),
                new FileColorConfig("jpg", "FF66BB6A"),
                new FileColorConfig("png", "FF66BB6A"),
                new FileColorConfig("mp4", "FF42A5F5"),
                new FileColorConfig("mkv", "FF42A5F5"),
                new FileColorConfig("mp3", "FFAB47BC"),
                new FileColorConfig("pdf", "FFFFA726"),
                new FileColorConfig("docx", "FFFFA726"),
                new FileColorConfig("zip", "FF8D6E63"),
                new FileColorConfig("rar", "FF8D6E63"),
                new FileColorConfig("java", "FF26A69A"),
                new FileColorConfig("kt", "FF26A69A")
        ));
    }

    /**
     * Saves custom file colors to disk.
     */
    public void saveColors(List<FileColorConfig> colors) {
        try {
            Files.createDirectories(colorsPath.getParent());
            try (Writer writer = Files.newBufferedWriter(colorsPath)) {
                gson.toJson(colors, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
