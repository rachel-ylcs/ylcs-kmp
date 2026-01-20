package love.yinlin.compose.ui

actual data class PAGFont(private val delegate: PlatformPAGFont) {
    actual constructor(fontFamily: String, fontStyle: String) : this(PlatformPAGFont(fontFamily, fontStyle))

    actual companion object {
        actual fun registerFont(path: String, ttcIndex: Int, font: PAGFont): PAGFont? =
            PlatformPAGFont.registerFont(path, ttcIndex, font.delegate)?.let(::PAGFont)
        actual fun registerFont(bytes: ByteArray, ttcIndex: Int, font: PAGFont): PAGFont? =
            PlatformPAGFont.registerFont(bytes, ttcIndex, font.delegate)?.let(::PAGFont)
        actual fun unregisterFont(font: PAGFont) = PlatformPAGFont.unregisterFont(font.delegate)
    }

    actual val fontFamily: String by delegate::fontFamily
    actual val fontStyle: String by delegate::fontStyle
}