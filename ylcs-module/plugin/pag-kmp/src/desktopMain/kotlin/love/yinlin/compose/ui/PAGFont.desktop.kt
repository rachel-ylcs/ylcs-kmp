package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual class PAGFont(private val delegate: PlatformPAGFont) {
    actual constructor(fontFamily: String, fontStyle: String) : this(PlatformPAGFont(fontFamily, fontStyle))

    actual companion object {
        actual fun registerFont(path: String, ttcIndex: Int, font: PAGFont): PAGFont? =
            PlatformPAGFont.registerFont(path, ttcIndex, font.delegate)?.let(::PAGFont)
        actual fun registerFont(bytes: ByteArray, ttcIndex: Int, font: PAGFont): PAGFont? =
            PlatformPAGFont.registerFont(bytes, ttcIndex, font.delegate)?.let(::PAGFont)
        actual fun unregisterFont(font: PAGFont) = PlatformPAGFont.unregisterFont(font.delegate)
    }

    actual val fontFamily: String get() = delegate.fontFamily
    actual val fontStyle: String get() = delegate.fontStyle
}