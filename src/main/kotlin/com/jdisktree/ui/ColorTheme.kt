package com.jdisktree.ui

import androidx.compose.ui.graphics.Color

fun getColorForExtension(ext: String): Color {
    return when (ext.lowercase()) {
        "dir_block" -> Color(0xFF455A64) // Dark Grey/Blue for compressed folders
        "exe", "dll", "sys", "msi", "com" -> Color(0xFFEF5350)
        "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp" -> Color(0xFF66BB6A)
        "mp4", "mkv", "avi", "mov", "flv", "webm" -> Color(0xFF42A5F5)
        "mp3", "wav", "flac", "ogg", "m4a" -> Color(0xFFAB47BC)
        "pdf", "doc", "docx", "txt", "rtf", "md", "odt", "xls", "xlsx" -> Color(0xFFFFA726)
        "zip", "rar", "7z", "tar", "gz", "bz2" -> Color(0xFF8D6E63)
        "java", "kt", "py", "cpp", "c", "js", "html", "css", "ts", "json", "xml" -> Color(0xFF26A69A)
        else -> Color(0xFF78909C)
    }
}

/**
 * Returns Color.Black or Color.White depending on the background's luminance.
 */
fun getContrastColor(background: Color): Color {
    val luminance = 0.2126 * background.red + 0.7152 * background.green + 0.0722 * background.blue
    return if (luminance > 0.5) Color.Black else Color.White
}
