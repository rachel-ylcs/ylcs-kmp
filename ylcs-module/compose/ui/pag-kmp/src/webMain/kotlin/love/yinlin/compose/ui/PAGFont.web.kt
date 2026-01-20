@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.ui

import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ByteArrayCompatible
import org.khronos.webgl.Uint8Array
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js

@Suppress("unused")
private fun hackDomLoadFont(family: String, data: Uint8Array): Promise<JsAny?> = js("""
{
    const fontFace = new FontFace(family, data);
    document.fonts.add(fontFace);
    return fontFace.load();
}
""")

actual data class PAGFont(private val delegate: PlatformPAGFont) {
    actual constructor(fontFamily: String, fontStyle: String) : this(PlatformPAGFont.create(fontFamily, fontStyle))

    actual companion object {
        actual fun registerFont(path: String, ttcIndex: Int, font: PAGFont): PAGFont? = null

        @OptIn(CompatibleRachelApi::class)
        actual fun registerFont(bytes: ByteArray, ttcIndex: Int, font: PAGFont): PAGFont? {
            val data = ByteArrayCompatible(bytes).asUint8Array
            hackDomLoadFont(font.fontFamily, data)
            return null
        }

        actual fun unregisterFont(font: PAGFont) { }
    }

    actual val fontFamily: String by delegate::fontFamily
    actual val fontStyle: String by delegate::fontStyle
}