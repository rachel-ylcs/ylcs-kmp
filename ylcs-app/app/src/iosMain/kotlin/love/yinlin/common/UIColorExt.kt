package love.yinlin.common

import platform.UIKit.UIColor

fun UIColor.Companion.colorWithHex(color: UInt): UIColor {
    val red = ((color and 0x00FF0000U) shr 16).toFloat() / 255.0
    val green = ((color and 0x0000FF00U) shr 8).toFloat() / 255.0
    val blue = (color and 0x000000FFU).toFloat() / 255.0
    val alpha = ((color and 0xFF000000U) shr 24).toFloat() / 255.0
    return UIColor(red = red, green = green, blue = blue, alpha = alpha)
}
