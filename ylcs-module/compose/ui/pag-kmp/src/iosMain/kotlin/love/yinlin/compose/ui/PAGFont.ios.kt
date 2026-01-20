@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

actual class PAGFont(private val delegate: PlatformPAGFont) {
    actual constructor(fontFamily: String, fontStyle: String) : this(makePlatformPAGFont(fontFamily, fontStyle))

    actual companion object {
        actual fun registerFont(path: String, ttcIndex: Int, font: PAGFont): PAGFont? =
            PlatformPAGFont.RegisterFont(path, font.fontFamily, font.fontStyle)?.let(::PAGFont)
        actual fun registerFont(bytes: ByteArray, ttcIndex: Int, font: PAGFont): PAGFont? = null
        actual fun unregisterFont(font: PAGFont) { PlatformPAGFont.UnregisterFont(font.delegate) }
    }

    actual val fontFamily: String get() = delegate.fontFamily!!
    actual val fontStyle: String get() = delegate.fontStyle!!
}