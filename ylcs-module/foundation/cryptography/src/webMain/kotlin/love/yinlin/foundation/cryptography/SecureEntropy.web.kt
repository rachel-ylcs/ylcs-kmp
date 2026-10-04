package love.yinlin.foundation.cryptography

import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray

@OptIn(ExperimentalWasmJsInterop::class)
private fun webCryptoRandomValues(size: Int): Int8Array = js("globalThis.crypto.getRandomValues(new Int8Array(size))")

actual object SecureEntropy {
    actual fun bytes(size: Int): ByteArray {
        return webCryptoRandomValues(size).toByteArray()
    }
}