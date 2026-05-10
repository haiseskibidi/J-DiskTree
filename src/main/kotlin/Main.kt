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
    val systemAccentColor = prefsService.windowsAccentColor

    application {
        val windowState = rememberWindowState(width = 1200.dp, height = 900.dp)
        
        // Use loaded preferences for initial state
        val initialLang = Language.entries.find { it.code == initialPrefs.languageCode() } ?: Language.EN
        val localizationManager = remember { LocalizationManager(initialLang) }
        var isDarkTheme by remember { mutableStateOf(initialPrefs.isDarkTheme()) }
        var showTypeStats by remember { mutableStateOf(initialPrefs.showTypeStats()) }
        var treeWeight by remember { mutableStateOf(initialPrefs.treeWidthWeight()) }
        var statsWeight by remember { mutableStateOf(initialPrefs.statsWidthWeight()) }

        // Effect to save whenever any preference changes (debounced)
        LaunchedEffect(isDarkTheme, localizationManager.currentLanguage, showTypeStats, treeWeight, statsWeight) {
            if (isDarkTheme != initialPrefs.isDarkTheme() || 
                localizationManager.currentLanguage.code != initialPrefs.languageCode() ||
                showTypeStats != initialPrefs.showTypeStats() ||
                treeWeight != initialPrefs.treeWidthWeight() ||
                statsWeight != initialPrefs.statsWidthWeight()) {
                
                kotlinx.coroutines.delay(1000) // Debounce save to avoid disk churn during drag
                prefsService.save(AppPreferences(
                    localizationManager.currentLanguage.code, 
                    isDarkTheme,
                    showTypeStats,
                    treeWeight,
                    statsWeight
                ))
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = localizationManager.get("app_name")
        ) {
            CompositionLocalProvider(LocalStrings provides localizationManager) {
                App(
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
                    onExit = { exitApplication() }
                )
            }
        }
    }
}
