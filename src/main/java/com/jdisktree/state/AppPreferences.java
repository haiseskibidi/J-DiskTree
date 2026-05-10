package com.jdisktree.state;

/**
 * Record holding user-configurable application preferences.
 */
public record AppPreferences(
        String languageCode,
        boolean isDarkTheme,
        boolean showTypeStats,
        float treeWidthWeight,
        float statsWidthWeight
) {
    public static AppPreferences defaults() {
        return new AppPreferences("en", true, true, 0.25f, 0.25f);
    }
}
