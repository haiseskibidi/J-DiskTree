package com.jdisktree.ui

import androidx.compose.ui.graphics.Color
import com.jdisktree.domain.FileColorConfig

fun getColorForExtension(ext: String, customColors: List<FileColorConfig> = emptyList()): Color {
    val custom = customColors.find { it.extension.equals(ext, ignoreCase = true) }
    if (custom != null) {
        try {
            return Color(custom.hexColor.toLong(16))
        } catch (e: Exception) {}
    }
    
    return when (ext.lowercase()) {
        "dir_block" -> AppColors.DirBlock
        "exe", "dll", "sys", "msi", "com" -> AppColors.Executable
        "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp" -> AppColors.Image
        "mp4", "mkv", "avi", "mov", "flv", "webm" -> AppColors.Video
        "mp3", "wav", "flac", "ogg", "m4a" -> AppColors.Audio
        "pdf", "doc", "docx", "txt", "rtf", "md", "odt", "xls", "xlsx" -> AppColors.Document
        "zip", "rar", "7z", "tar", "gz", "bz2" -> AppColors.Archive
        "java", "kt", "py", "cpp", "c", "js", "html", "css", "ts", "json", "xml" -> AppColors.Code
        else -> AppColors.Unknown
    }
}

/**
 * Returns Color.Black or Color.White depending on the background's luminance.
 */
fun getContrastColor(background: Color): Color {
    val luminance = 0.2126 * background.red + 0.7152 * background.green + 0.0722 * background.blue
    return if (luminance > 0.5) Color.Black else Color.White
}
