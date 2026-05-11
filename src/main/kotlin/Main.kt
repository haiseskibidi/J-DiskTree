import androidx.compose.ui.window.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import com.jdisktree.ui.*
import com.jdisktree.state.PreferencesService
import com.jdisktree.state.AppPreferences

fun main() {
    System.setProperty("sun.java2d.uiScale.enabled", "true")
    
    val prefsService = PreferencesService()
    val initialPrefs = prefsService.load()
    val initialColors = prefsService.loadColors()
    val systemAccentColor = prefsService.windowsAccentColor

    application {
        val windowState = rememberWindowState(width = 1200.dp, height = 900.dp)
        
        // App State
        var appPrefs by remember { mutableStateOf(initialPrefs) }
        var fileColors by remember { mutableStateOf(initialColors) }

        // Derived UI values
        val initialLang = Language.entries.find { it.code == appPrefs.languageCode() } ?: Language.EN
        val localizationManager = remember(appPrefs.languageCode()) { LocalizationManager(initialLang) }
        var isDarkTheme by remember { mutableStateOf(appPrefs.isDarkTheme()) }
        var showTypeStats by remember { mutableStateOf(appPrefs.showTypeStats()) }
        var treeWeight by remember { mutableStateOf(appPrefs.treeWidthWeight()) }
        var statsWeight by remember { mutableStateOf(appPrefs.statsWidthWeight()) }

        // Effect to save whenever any preference changes (debounced)
        LaunchedEffect(isDarkTheme, localizationManager.currentLanguage, showTypeStats, treeWeight, statsWeight, appPrefs.scanExclusions()) {
            val newPrefs = AppPreferences(
                localizationManager.currentLanguage.code,
                isDarkTheme,
                showTypeStats,
                treeWeight,
                statsWeight,
                appPrefs.scanExclusions()
            )
            appPrefs = newPrefs
            kotlinx.coroutines.delay(1000) // Debounce save to avoid disk churn during drag
            prefsService.save(newPrefs)
        }
        
        LaunchedEffect(fileColors) {
            prefsService.saveColors(fileColors)
        }

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = localizationManager.get("app_name")
        ) {
            CompositionLocalProvider(LocalStrings provides localizationManager) {
                App(
                    appPrefs = appPrefs,
                    fileColors = fileColors,
                    isDarkTheme = isDarkTheme,
                    showTypeStatsInitial = showTypeStats,
                    treeWeightInitial = treeWeight,
                    statsWeightInitial = statsWeight,
                    systemAccentColorHex = systemAccentColor,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onStatsToggle = { showTypeStats = !showTypeStats },
                    onWeightsChange = { t, s -> 
                        treeWeight = t
                        statsWeight = s
                    },
                    onSettingsSave = { exclusions, colors ->
                        appPrefs = AppPreferences(
                            appPrefs.languageCode(),
                            appPrefs.isDarkTheme(),
                            appPrefs.showTypeStats(),
                            appPrefs.treeWidthWeight(),
                            appPrefs.statsWidthWeight(),
                            exclusions
                        )
                        fileColors = colors
                    },
                    onExit = { exitApplication() }
                )
            }
        }
    }
}
