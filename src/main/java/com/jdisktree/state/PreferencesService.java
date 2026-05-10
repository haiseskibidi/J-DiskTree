package com.jdisktree.state;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
            return new AppPreferences(lang, isDark);
        } catch (IOException e) {
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

            try (OutputStream os = Files.newOutputStream(configPath)) {
                props.store(os, "J-DiskTree User Preferences");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
