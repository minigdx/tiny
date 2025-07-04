package com.github.minigdx.tiny.cli.command.utils

object ColorUtils {
    fun brightness(hexColor: String): Float {
        // Remove the '#' prefix
        val colorWithoutHash = hexColor.removePrefix("#")

        // Parse R, G, B components from the hex string
        // Use 16 as the radix for hexadecimal parsing
        val r = colorWithoutHash.substring(0, 2).toInt(16)
        val g = colorWithoutHash.substring(2, 4).toInt(16)
        val b = colorWithoutHash.substring(4, 6).toInt(16)

        // Calculate brightness. A common formula for perceived brightness (luminance) is:
        // 0.299*R + 0.587*G + 0.114*B
        // The components R, G, B are already in the range [0, 255].
        val brightness = 0.299f * r + 0.587f * g + 0.114f * b

        // The sortedByDescending function will use this brightness value
        // to order the colors from highest brightness to lowest.
        return brightness
    }
}
