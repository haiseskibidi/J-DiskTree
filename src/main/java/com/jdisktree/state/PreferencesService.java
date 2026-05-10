package com.jdisktree.state;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Service responsible for saving and loading application preferences from the user's home directory.
 */
public class PreferencesService {

    private static final String DIR_NAME = ".jdisktree";
    private static final String FILE_NAME = "settings.properties";

    private final Path configPath;

    public PreferencesService() {
        String userHome = System.getProperty("user.home");
        this.configPath = Paths.get(userHome, DIR_NAME, FILE_NAME);
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

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(configPath)) {
            props.load(is);
            String lang = props.getProperty("language", "en");
            boolean isDark = Boolean.parseBoolean(props.getProperty("dark_theme", "true"));
            boolean showStats = Boolean.parseBoolean(props.getProperty("show_stats", "true"));
            float treeWeight = Float.parseFloat(props.getProperty("tree_weight", "0.25"));
            float statsWeight = Float.parseFloat(props.getProperty("stats_weight", "0.25"));
            return new AppPreferences(lang, isDark, showStats, treeWeight, statsWeight);
        } catch (IOException | NumberFormatException e) {
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
            Properties props = new Properties();
            props.setProperty("language", prefs.languageCode());
            props.setProperty("dark_theme", String.valueOf(prefs.isDarkTheme()));
            props.setProperty("show_stats", String.valueOf(prefs.showTypeStats()));
            props.setProperty("tree_weight", String.valueOf(prefs.treeWidthWeight()));
            props.setProperty("stats_weight", String.valueOf(prefs.statsWidthWeight()));

            try (OutputStream os = Files.newOutputStream(configPath)) {
                props.store(os, "J-DiskTree User Preferences");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
