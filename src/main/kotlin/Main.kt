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

    application {
        val windowState = rememberWindowState(width = 1200.dp, height = 900.dp)
        
        // Use loaded preferences for initial state
        val initialLang = Language.entries.find { it.code == initialPrefs.languageCode() } ?: Language.EN
        val localizationManager = remember { LocalizationManager(initialLang) }
        var isDarkTheme by remember { mutableStateOf(initialPrefs.isDarkTheme()) }

        // Effect to save whenever theme or language changes
        LaunchedEffect(isDarkTheme, localizationManager.currentLanguage) {
            prefsService.save(AppPreferences(localizationManager.currentLanguage.code, isDarkTheme))
        }

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = localizationManager.get("app_name")
        ) {
            CompositionLocalProvider(LocalStrings provides localizationManager) {
                App(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    onExit = { exitApplication() }
                )
            }
        }
    }
}
