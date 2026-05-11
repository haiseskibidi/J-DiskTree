package com.jdisktree.ui

fun formatSize(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    val absBytes = if (bytes < 0) -bytes else bytes
    if (absBytes < 1024) return "$bytes B"
    val exp = (Math.log(absBytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    val formatted = String.format("%.1f %sB", absBytes / Math.pow(1024.0, exp.toDouble()), pre)
    return if (bytes < 0) "-$formatted" else formatted
}
