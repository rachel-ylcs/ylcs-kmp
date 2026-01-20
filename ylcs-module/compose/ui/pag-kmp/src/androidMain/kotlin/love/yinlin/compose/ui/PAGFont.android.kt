package love.yinlin.compose.ui

actual data class PAGFont(private val delegate: PlatformPAGFont) {
    actual constructor(fontFamily: String, fontStyle: String) : this(PlatformPAGFont(fontFamily, fontStyle))

    actual companion object {
        actual fun registerFont(path: String, ttcIndex: Int, font: PAGFont): PAGFont? =
            PlatformPAGFont.RegisterFont(path, ttcIndex, font.fontFamily, font.fontStyle)?.let(::PAGFont)
        actual fun registerFont(bytes: ByteArray, ttcIndex: Int, font: PAGFont): PAGFont? = null
        actual fun unregisterFont(font: PAGFont) = PlatformPAGFont.UnregisterFont(font.delegate)
    }

    actual val fontFamily: String by delegate::fontFamily
    actual val fontStyle: String by delegate::fontStyle
}