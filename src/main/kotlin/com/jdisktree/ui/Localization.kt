package com.jdisktree.ui

import androidx.compose.runtime.*
import java.util.*

enum class Language(val code: String, val displayName: String) {
    EN("en", "English"),
    RU("ru", "Русский")
}

class LocalizationManager(
    initialLanguage: Language = Language.EN,
    private val onLanguageChanged: (Language) -> Unit = {}
) {
    var currentLanguage by mutableStateOf(initialLanguage)
    
    // Making bundle part of the state so recomposition triggers when it changes
    var bundle by mutableStateOf(ResourceBundle.getBundle("i18n.strings", Locale.of(initialLanguage.code)))

    fun setLanguage(language: Language) {
        currentLanguage = language
        bundle = ResourceBundle.getBundle("i18n.strings", Locale.of(language.code))
        onLanguageChanged(language)
    }

    fun get(key: String, vararg args: Any): String {
        return try {
            val pattern = bundle.getString(key)
            if (args.isEmpty()) pattern else pattern.format(*args)
        } catch (e: Exception) {
            "!$key!"
        }
    }
}

val LocalStrings = staticCompositionLocalOf<LocalizationManager> {
    error("No LocalizationManager provided")
}

@Composable
fun stringResource(key: String, vararg args: Any): String {
    return LocalStrings.current.get(key, *args)
}
