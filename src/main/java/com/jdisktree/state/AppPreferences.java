package com.jdisktree.state;

import com.jdisktree.domain.ScanExclusion;
import java.util.List;

/**
 * Record holding user-configurable application preferences.
 */
public record AppPreferences(
        String languageCode,
        boolean isDarkTheme,
        boolean showTypeStats,
        float treeWidthWeight,
        float statsWidthWeight,
        List<ScanExclusion> scanExclusions
) {
    public AppPreferences {
        if (scanExclusions == null) {
            scanExclusions = defaultExclusions();
        }
        if (languageCode == null) {
            languageCode = "en";
        }
    }

    public static AppPreferences defaults() {
        return new AppPreferences(
                "en",
                true,
                true,
                0.25f,
                0.25f,
                defaultExclusions()
        );
    }

    private static List<ScanExclusion> defaultExclusions() {
        return List.of(
                new ScanExclusion(".git", false),
                new ScanExclusion("node_modules", false),
                new ScanExclusion(".idea", false),
                new ScanExclusion("build", false),
                new ScanExclusion("out", false),
                new ScanExclusion("target", false),
                new ScanExclusion(".gradle", false),
                new ScanExclusion("venv", false),
                new ScanExclusion("__pycache__", false),
                new ScanExclusion(".DS_Store", false),
                new ScanExclusion("tmp", false),
                new ScanExclusion("temp", false),
                new ScanExclusion("dist", false),
                new ScanExclusion("bin", false),
                new ScanExclusion("obj", false),
                new ScanExclusion(".mp4", false)
        );
    }
}
