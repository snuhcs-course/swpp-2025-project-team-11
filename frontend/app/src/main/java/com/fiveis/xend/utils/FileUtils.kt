package com.fiveis.xend.utils

import java.util.Locale

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = size.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) {
        "${size}B"
    } else {
        String.format(Locale.getDefault(), "%.1f%s", value, units[index])
    }
}

fun shortenFilename(name: String, maxLength: Int = 30): String {
    if (name.length <= maxLength) return name
    val dotIndex = name.lastIndexOf('.')
    val hasExt = dotIndex in 1 until name.lastIndex
    val ext = if (hasExt) name.substring(dotIndex) else ""
    val baseMax = (maxLength - ext.length - 3).coerceAtLeast(4)
    val base = name.take(baseMax)
    return "$base...$ext"
}
