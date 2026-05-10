package com.jdisktree.state;

/**
 * Record holding user-configurable application preferences.
 */
public record AppPreferences(
        String languageCode,
        boolean isDarkTheme,
        boolean showTypeStats
) {
    public static AppPreferences defaults() {
        return new AppPreferences("en", true, true);
    }
}
